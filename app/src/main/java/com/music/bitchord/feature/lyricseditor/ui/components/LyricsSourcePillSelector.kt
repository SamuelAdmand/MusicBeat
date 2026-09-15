package com.music.bitchord.feature.lyricseditor.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.InsertDriveFile
import androidx.compose.material.icons.rounded.AudioFile
import androidx.compose.material.icons.rounded.Download
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.music.bitchord.feature.lyricseditor.domain.model.LyricsEditorSource

@Composable
fun LyricsSourcePillSelector(
    selectedSource: LyricsEditorSource,
    onSourceSelected: (LyricsEditorSource) -> Unit,
    enabled: Boolean = true,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(24.dp)),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        LyricsEditorSource.entries.forEach { source ->
            val isSelected = source == selectedSource
            val targetContainerColor = if (isSelected) {
                MaterialTheme.colorScheme.secondaryContainer
            } else {
                MaterialTheme.colorScheme.surfaceContainerLow
            }
            val targetContentColor = if (isSelected) {
                MaterialTheme.colorScheme.onSecondaryContainer
            } else {
                MaterialTheme.colorScheme.onSurfaceVariant
            }
            val containerColor by animateColorAsState(
                targetValue = targetContainerColor,
                animationSpec = tween(200),
                label = "pill_container_color",
            )
            val contentColor by animateColorAsState(
                targetValue = targetContentColor,
                animationSpec = tween(200),
                label = "pill_content_color",
            )

            val icon: ImageVector = when (source) {
                LyricsEditorSource.Embedded -> Icons.Rounded.AudioFile
                LyricsEditorSource.Downloaded -> Icons.Rounded.Download
                LyricsEditorSource.File -> Icons.AutoMirrored.Rounded.InsertDriveFile
            }

            val buttonWeight = if (isSelected) 1.35f else 1f

            Surface(
                modifier = Modifier
                    .weight(buttonWeight)
                    .height(40.dp)
                    .clip(RoundedCornerShape(20.dp))
                    .clickable(enabled = enabled) {
                        onSourceSelected(source)
                    },
                shape = RoundedCornerShape(20.dp),
                color = containerColor,
                border = if (!isSelected) {
                    BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                } else null,
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 4.dp),
                    horizontalArrangement = Arrangement.spacedBy(4.dp, Alignment.CenterHorizontally),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    if (isSelected) {
                        Icon(
                            imageVector = icon,
                            contentDescription = null,
                            tint = contentColor,
                            modifier = Modifier.size(16.dp),
                        )
                    }
                    Text(
                        text = source.displayName,
                        style = MaterialTheme.typography.labelMedium.copy(
                            fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Medium,
                        ),
                        color = contentColor,
                        maxLines = 1,
                    )
                }
            }
        }
    }
}
