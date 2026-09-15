package com.music.bitchord.feature.lyricseditor.ui

import android.app.Activity
import android.app.SearchManager
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Process
import android.provider.MediaStore
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.IntentSenderRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.music.bitchord.data.model.Song
import com.music.bitchord.feature.lyricseditor.domain.model.LyricsEditorSource
import com.music.bitchord.feature.lyricseditor.ui.components.LyricsDownloadDialog
import com.music.bitchord.feature.lyricseditor.ui.components.LyricsEditorBottomBar
import com.music.bitchord.feature.lyricseditor.ui.components.LyricsEditorHeader
import com.music.bitchord.feature.lyricseditor.ui.components.LyricsEditorTopBar
import com.music.bitchord.feature.lyricseditor.ui.components.LyricsSelectorDialog
import com.music.bitchord.feature.lyricseditor.ui.components.LyricsSourcePillSelector
import com.music.bitchord.feature.lyricseditor.ui.viewmodel.LyricsEditorViewModel

@Composable
fun LyricsEditorScreen(
    song: Song,
    onBackClick: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: LyricsEditorViewModel = viewModel(),
    onLyricsSaved: (() -> Unit)? = null,
) {
    val context = LocalContext.current
    val clipboardManager = remember {
        context.getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager
    }

    val selectedSource by viewModel.selectedSource.collectAsState()
    val currentText by viewModel.currentText.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val isSaving by viewModel.isSaving.collectAsState()
    val candidateResult by viewModel.candidateResult.collectAsState()

    var showDownloadDialog by remember { mutableStateOf(false) }
    var textFieldValue by remember { mutableStateOf(TextFieldValue(currentText)) }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartIntentSenderForResult(),
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            viewModel.saveLyrics(song)
        } else {
            Toast.makeText(context, "Permission denied to edit audio file", Toast.LENGTH_SHORT).show()
        }
    }

    fun handleSave() {
        if (selectedSource == LyricsEditorSource.Embedded && Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            val uriStr = song.localUri ?: song.videoId
            val uri = if (uriStr.startsWith("content://")) Uri.parse(uriStr) else null
            if (uri != null) {
                val hasPerm = context.checkUriPermission(
                    uri,
                    Process.myPid(),
                    Process.myUid(),
                    Intent.FLAG_GRANT_WRITE_URI_PERMISSION,
                ) == PackageManager.PERMISSION_GRANTED
                if (!hasPerm) {
                    val pendingIntent = runCatching {
                        MediaStore.createWriteRequest(context.contentResolver, listOf(uri))
                    }.getOrNull()
                    if (pendingIntent != null) {
                        permissionLauncher.launch(
                            IntentSenderRequest.Builder(pendingIntent.intentSender).build()
                        )
                        return
                    }
                }
            }
        }
        viewModel.saveLyrics(song)
    }

    // Sync external currentText changes into TextFieldValue
    LaunchedEffect(currentText) {
        if (textFieldValue.text != currentText) {
            textFieldValue = TextFieldValue(
                text = currentText,
                selection = TextRange(currentText.length),
            )
        }
    }

    LaunchedEffect(song.videoId) {
        viewModel.loadSong(song)
    }

    LaunchedEffect(Unit) {
        viewModel.saveResult.collect { success ->
            val message = if (success) "Lyrics saved successfully" else "Failed to save lyrics"
            Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
            if (success) {
                onLyricsSaved?.invoke()
            }
        }
    }

    LaunchedEffect(Unit) {
        viewModel.toastMessage.collect { msg ->
            Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
        }
    }

    val isFileSource = selectedSource == LyricsEditorSource.File

    Scaffold(
        topBar = {
            LyricsEditorTopBar(
                onBackClick = onBackClick,
            )
        },
        bottomBar = {
            LyricsEditorBottomBar(
                enabled = !isLoading && !isSaving,
                onSearchClick = {
                    val query = "${song.title} ${song.artist} lyrics".trim()
                    runCatching {
                        val intent = Intent(Intent.ACTION_WEB_SEARCH).apply {
                            putExtra(SearchManager.QUERY, query)
                        }
                        context.startActivity(intent)
                    }.onFailure {
                        val fallback = Intent(
                            Intent.ACTION_VIEW,
                            Uri.parse("https://www.google.com/search?q=${Uri.encode(query)}")
                        )
                        context.startActivity(fallback)
                    }
                },
                onDownloadClick = {
                    showDownloadDialog = true
                },
                onSaveClick = {
                    handleSave()
                },
                onPasteClick = {
                    val clip = clipboardManager?.primaryClip
                    if (clip != null && clip.itemCount > 0) {
                        val pasteText = clip.getItemAt(0).text?.toString().orEmpty()
                        if (pasteText.isNotBlank()) {
                            textFieldValue = TextFieldValue(pasteText, selection = TextRange(pasteText.length))
                            viewModel.updateText(pasteText)
                            Toast.makeText(context, "Pasted from clipboard", Toast.LENGTH_SHORT).show()
                        }
                    }
                },
                onSelectAllClick = {
                    textFieldValue = textFieldValue.copy(
                        selection = TextRange(0, textFieldValue.text.length)
                    )
                },
                onUndoChangesClick = {
                    viewModel.undoChanges()
                    Toast.makeText(context, "Changes undone", Toast.LENGTH_SHORT).show()
                },
            )
        },
        modifier = modifier.imePadding(),
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(horizontal = 16.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            LyricsEditorHeader(
                song = song,
                isLoading = isLoading,
            )

            LyricsSourcePillSelector(
                selectedSource = selectedSource,
                onSourceSelected = { source ->
                    viewModel.selectSource(source)
                },
                enabled = !isLoading && !isSaving,
            )

            AnimatedVisibility(visible = isFileSource) {
                Text(
                    text = "Companion LRC file will be saved alongside the audio file",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth(),
                )
            }

            OutlinedTextField(
                value = textFieldValue,
                onValueChange = { newValue ->
                    textFieldValue = newValue
                    viewModel.updateText(newValue.text)
                },
                placeholder = {
                    Text("Write lyrics here...")
                },
                shape = RoundedCornerShape(16.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = MaterialTheme.colorScheme.primary,
                    unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant,
                ),
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
            )
        }
    }

    if (showDownloadDialog) {
        LyricsDownloadDialog(
            initialTitle = song.title,
            initialArtist = song.artist,
            onDismissRequest = { showDownloadDialog = false },
            onDownloadClick = { title, artist ->
                showDownloadDialog = false
                viewModel.downloadLyrics(title, artist)
            },
        )
    }

    if (candidateResult != null) {
        LyricsSelectorDialog(
            onDismissRequest = { viewModel.dismissCandidateDialog() },
            onModeSelected = { mode ->
                viewModel.applyCandidateSelection(mode)
            },
        )
    }
}
