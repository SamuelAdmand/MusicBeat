package com.music.bitchord.feature.tageditor.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp

val STANDARD_GENRES = listOf(
    "Acoustic", "Alternative", "Ambient", "Anime", "Blues", "Classical", "Classic Rock",
    "Country", "Dance", "Disco", "Electronic", "Folk", "Funk", "Gospel", "Grunge",
    "Hard Rock", "Hip-Hop", "House", "Indie", "Instrumental", "J-Pop", "Jazz",
    "K-Pop", "Latin", "Lo-Fi", "Metal", "Musical", "New Age", "Oldies", "Opera",
    "Pop", "Punk", "R&B", "Rap", "Reggae", "Rock", "Soul", "Soundtrack", "Techno",
    "Trance", "World",
)

@Composable
fun TagEditorGenreDialog(
    onDismissRequest: () -> Unit,
    onGenreSelected: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    var search by remember { mutableStateOf("") }
    val filtered = remember(search) {
        if (search.isBlank()) STANDARD_GENRES
        else STANDARD_GENRES.filter { it.contains(search, ignoreCase = true) }
    }

    AlertDialog(
        onDismissRequest = onDismissRequest,
        shape = RoundedCornerShape(24.dp),
        title = {
            Text(
                text = "Select genre",
                style = MaterialTheme.typography.titleLarge,
            )
        },
        text = {
            Column(modifier = Modifier.fillMaxWidth()) {
                OutlinedTextField(
                    value = search,
                    onValueChange = { search = it },
                    placeholder = { Text("Search genre...") },
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth(),
                )

                Spacer(Modifier.height(12.dp))

                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 280.dp),
                ) {
                    items(filtered, key = { it }) { genre ->
                        Text(
                            text = genre,
                            style = MaterialTheme.typography.bodyLarge,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(8.dp))
                                .clickable {
                                    onGenreSelected(genre)
                                    onDismissRequest()
                                }
                                .padding(horizontal = 12.dp, vertical = 12.dp),
                        )
                    }
                }
            }
        },
        confirmButton = {},
        dismissButton = {
            TextButton(onClick = onDismissRequest) {
                Text("Cancel")
            }
        },
        modifier = modifier,
    )
}
