package com.music.bitchord.feature.lyricseditor.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.unit.dp
import com.music.bitchord.data.lyrics.LyricsSource

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun LyricsDownloadDialog(
    initialTitle: String,
    initialArtist: String,
    initialAlbum: String? = null,
    onDismissRequest: () -> Unit,
    onAutoDownloadClick: (title: String, artist: String, album: String?) -> Unit,
    onSearchAllClick: (title: String, artist: String, album: String?, providers: Set<LyricsSource>) -> Unit,
    modifier: Modifier = Modifier,
) {
    var title by remember { mutableStateOf(initialTitle) }
    var artist by remember { mutableStateOf(initialArtist) }
    var album by remember { mutableStateOf(initialAlbum.orEmpty()) }

    val availableProviders = remember {
        listOf(
            LyricsSource.LRCLIB,
            LyricsSource.BETTER_LYRICS,
            LyricsSource.KUGOU,
            LyricsSource.MUSIXMATCH,
            LyricsSource.GENIUS,
        )
    }

    val selectedProviders = remember {
        mutableStateListOf<LyricsSource>().apply {
            addAll(availableProviders)
        }
    }

    val canSearch = title.isNotBlank() || artist.isNotBlank()

    AlertDialog(
        onDismissRequest = onDismissRequest,
        shape = RoundedCornerShape(24.dp),
        title = {
            Text(
                text = "Download lyrics",
                style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold),
            )
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text("Title") },
                    placeholder = { Text("Song title") },
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth(),
                )

                OutlinedTextField(
                    value = artist,
                    onValueChange = { artist = it },
                    label = { Text("Artist") },
                    placeholder = { Text("Artist name") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Words),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth(),
                )

                OutlinedTextField(
                    value = album,
                    onValueChange = { album = it },
                    label = { Text("Album (Optional)") },
                    placeholder = { Text("Album name") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Words),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth(),
                )

                Text(
                    text = "Lyrics providers:",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 4.dp),
                )

                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    availableProviders.forEach { provider ->
                        val isChecked = provider in selectedProviders
                        FilterChip(
                            selected = isChecked,
                            onClick = {
                                if (isChecked) {
                                    if (selectedProviders.size > 1) {
                                        selectedProviders.remove(provider)
                                    }
                                } else {
                                    selectedProviders.add(provider)
                                }
                            },
                            label = { Text(provider.label) },
                            shape = RoundedCornerShape(8.dp),
                        )
                    }
                }
            }
        },
        confirmButton = {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(
                    onClick = {
                        onAutoDownloadClick(
                            title.trim(),
                            artist.trim(),
                            album.trim().ifEmpty { null },
                        )
                    },
                    enabled = canSearch,
                ) {
                    Text("Auto")
                }

                Button(
                    onClick = {
                        onSearchAllClick(
                            title.trim(),
                            artist.trim(),
                            album.trim().ifEmpty { null },
                            selectedProviders.toSet(),
                        )
                    },
                    enabled = canSearch && selectedProviders.isNotEmpty(),
                ) {
                    Text("Search Results")
                }
            }
        },
        dismissButton = {
            TextButton(onClick = onDismissRequest) {
                Text("Cancel")
            }
        },
        modifier = modifier,
    )
}
