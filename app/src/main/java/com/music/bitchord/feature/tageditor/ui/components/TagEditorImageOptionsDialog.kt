package com.music.bitchord.feature.tageditor.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.Image
import androidx.compose.material.icons.rounded.Restore
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp

@Composable
fun TagEditorImageOptionsDialog(
    onDismissRequest: () -> Unit,
    onPickFromStorage: () -> Unit,
    onSearchOnline: () -> Unit,
    onRestoreDefault: () -> Unit,
    onRemoveCover: () -> Unit,
    modifier: Modifier = Modifier,
) {
    AlertDialog(
        onDismissRequest = onDismissRequest,
        shape = RoundedCornerShape(24.dp),
        title = {
            Text(
                text = "Update cover art",
                style = MaterialTheme.typography.titleLarge,
            )
        },
        text = {
            Column(modifier = Modifier.fillMaxWidth()) {
                ImageOptionRow(
                    icon = Icons.Rounded.Image,
                    text = "Pick from local storage",
                    onClick = {
                        onPickFromStorage()
                        onDismissRequest()
                    },
                )
                ImageOptionRow(
                    icon = Icons.Rounded.Search,
                    text = "Search online image",
                    onClick = {
                        onSearchOnline()
                        onDismissRequest()
                    },
                )
                ImageOptionRow(
                    icon = Icons.Rounded.Restore,
                    text = "Restore default",
                    onClick = {
                        onRestoreDefault()
                        onDismissRequest()
                    },
                )
                ImageOptionRow(
                    icon = Icons.Rounded.Delete,
                    text = "Remove cover",
                    isDestructive = true,
                    onClick = {
                        onRemoveCover()
                        onDismissRequest()
                    },
                )
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

@Composable
private fun ImageOptionRow(
    icon: ImageVector,
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    isDestructive: Boolean = false,
) {
    val color = if (isDestructive) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurface
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 14.dp),
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = color,
            modifier = Modifier.size(22.dp),
        )
        Spacer(Modifier.width(16.dp))
        Text(
            text = text,
            style = MaterialTheme.typography.bodyLarge,
            color = color,
        )
    }
}
