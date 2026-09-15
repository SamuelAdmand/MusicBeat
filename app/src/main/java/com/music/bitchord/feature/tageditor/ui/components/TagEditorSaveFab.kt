package com.music.bitchord.feature.tageditor.ui.components

import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Save
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
fun TagEditorSaveFab(
    isSaving: Boolean,
    onSaveClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    FloatingActionButton(
        onClick = { if (!isSaving) onSaveClick() },
        shape = RoundedCornerShape(18.dp),
        containerColor = MaterialTheme.colorScheme.primaryContainer,
        contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
        modifier = modifier,
    ) {
        if (isSaving) {
            CircularProgressIndicator(
                strokeWidth = 2.5.dp,
                modifier = Modifier.size(24.dp),
            )
        } else {
            Icon(
                imageVector = Icons.Rounded.Save,
                contentDescription = "Save tags",
                modifier = Modifier.size(24.dp),
            )
        }
    }
}
