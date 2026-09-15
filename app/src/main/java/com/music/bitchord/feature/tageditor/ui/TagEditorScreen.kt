package com.music.bitchord.feature.tageditor.ui

import android.app.SearchManager
import android.content.Intent
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FabPosition
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.music.bitchord.data.model.Song
import com.music.bitchord.feature.tageditor.ui.components.TagEditorArtworkSection
import com.music.bitchord.feature.tageditor.ui.components.TagEditorFormFields
import com.music.bitchord.feature.tageditor.ui.components.TagEditorGenreDialog
import com.music.bitchord.feature.tageditor.ui.components.TagEditorImageOptionsDialog
import com.music.bitchord.feature.tageditor.ui.components.TagEditorSaveFab
import com.music.bitchord.feature.tageditor.ui.components.TagEditorTopBar
import com.music.bitchord.feature.tageditor.ui.viewmodel.TagEditorViewModel

@Composable
fun TagEditorScreen(
    song: Song,
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: TagEditorViewModel = viewModel(),
    onTagsSaved: (() -> Unit)? = null,
) {
    val context = LocalContext.current
    val tags by viewModel.tags.collectAsStateWithLifecycle()
    val isLoading by viewModel.isLoading.collectAsStateWithLifecycle()
    val isSaving by viewModel.isSaving.collectAsStateWithLifecycle()

    var showImageOptions by remember { mutableStateOf(false) }
    var showGenreDialog by remember { mutableStateOf(false) }

    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia(),
    ) { uri ->
        if (uri != null) {
            viewModel.updateArtworkFromUri(uri)
        }
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartIntentSenderForResult(),
    ) { result ->
        if (result.resultCode == android.app.Activity.RESULT_OK) {
            viewModel.save(song)
        } else {
            Toast.makeText(context, "Permission denied to edit audio file", Toast.LENGTH_SHORT).show()
        }
    }

    fun handleSave() {
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.R) {
            val uriStr = song.localUri ?: song.videoId
            val uri = if (uriStr.startsWith("content://")) android.net.Uri.parse(uriStr) else null
            if (uri != null) {
                val hasPerm = context.checkUriPermission(
                    uri,
                    android.os.Process.myPid(),
                    android.os.Process.myUid(),
                    Intent.FLAG_GRANT_WRITE_URI_PERMISSION,
                ) == android.content.pm.PackageManager.PERMISSION_GRANTED
                if (!hasPerm) {
                    val pendingIntent = runCatching {
                        android.provider.MediaStore.createWriteRequest(context.contentResolver, listOf(uri))
                    }.getOrNull()
                    if (pendingIntent != null) {
                        permissionLauncher.launch(
                            androidx.activity.result.IntentSenderRequest.Builder(pendingIntent.intentSender).build()
                        )
                        return
                    }
                }
            }
        }
        viewModel.save(song)
    }

    LaunchedEffect(song) {
        viewModel.loadTags(song)
    }

    LaunchedEffect(Unit) {
        viewModel.saveEvent.collect { success ->
            if (success) {
                Toast.makeText(context, "Changes saved successfully", Toast.LENGTH_SHORT).show()
                onTagsSaved?.invoke()
                onNavigateBack()
            } else {
                Toast.makeText(context, "Failed to save some changes", Toast.LENGTH_SHORT).show()
            }
        }
    }

    fun openWebSearch(query: String) {
        runCatching {
            val intent = Intent(Intent.ACTION_WEB_SEARCH).apply {
                putExtra(SearchManager.QUERY, query)
            }
            context.startActivity(intent)
        }
    }

    Scaffold(
        topBar = {
            TagEditorTopBar(
                onBackClick = onNavigateBack,
                onSearchOnlineImage = {
                    openWebSearch("${tags.artist} ${tags.album.ifEmpty { tags.title }} album cover")
                },
                onResetTags = { viewModel.resetTags() },
            )
        },
        floatingActionButton = {
            TagEditorSaveFab(
                isSaving = isSaving,
                onSaveClick = { handleSave() },
            )
        },
        floatingActionButtonPosition = FabPosition.End,
        containerColor = MaterialTheme.colorScheme.background,
        modifier = modifier.fillMaxSize(),
    ) { paddingValues ->
        if (isLoading) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues),
                contentAlignment = Alignment.Center,
            ) {
                CircularProgressIndicator()
            }
        } else {
            LazyColumn(
                state = rememberLazyListState(),
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .padding(horizontal = 20.dp),
            ) {
                item {
                    TagEditorArtworkSection(
                        artworkBitmap = tags.artworkBitmap,
                        thumbnailUrl = song.thumbnailUrl,
                        onClick = { showImageOptions = true },
                    )
                }

                item {
                    TagEditorFormFields(
                        tags = tags,
                        onTagsChange = { viewModel.updateTags(it) },
                        onOpenGenrePicker = { showGenreDialog = true },
                    )
                }

                item {
                    Spacer(Modifier.height(88.dp))
                }
            }
        }
    }

    if (showImageOptions) {
        TagEditorImageOptionsDialog(
            onDismissRequest = { showImageOptions = false },
            onPickFromStorage = {
                photoPickerLauncher.launch(
                    PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly),
                )
            },
            onSearchOnline = {
                openWebSearch("${tags.artist} ${tags.album.ifEmpty { tags.title }} album cover")
            },
            onRestoreDefault = { viewModel.restoreArtwork() },
            onRemoveCover = { viewModel.deleteArtwork() },
        )
    }

    if (showGenreDialog) {
        TagEditorGenreDialog(
            onDismissRequest = { showGenreDialog = false },
            onGenreSelected = { selected ->
                val current = tags.genre.trim()
                val updated = if (current.isEmpty()) selected else "$current; $selected"
                viewModel.updateTags(tags.copy(genre = updated))
            },
        )
    }
}
