package com.music.bitchord.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.Sort
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.music.bitchord.R
import com.music.bitchord.data.settings.LibraryViewType
import com.music.bitchord.feature.artistimage.model.ArtistSort
import com.music.bitchord.ui.haptics.Haptic
import com.music.bitchord.ui.haptics.rememberHaptics
import com.music.bitchord.ui.icons.BitChordIcons

/**
 * View type toggle and sort selector controls specifically designed for the Artists library tab.
 * Provides options: Most Songs, A to Z, and Z to A.
 */
@Composable
fun ArtistViewAndSortControls(
    sortOrder: ArtistSort,
    onSortOrderChange: (ArtistSort) -> Unit,
    viewType: LibraryViewType,
    onViewTypeToggle: () -> Unit,
    modifier: Modifier = Modifier,
    showViewToggle: Boolean = true,
    showSort: Boolean = true,
) {
    var sortMenuOpen by remember { mutableStateOf(false) }
    val haptics = rememberHaptics()

    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        if (showViewToggle) {
            Box(
                modifier = Modifier
                    .size(32.dp)
                    .clip(CircleShape)
                    .clickable {
                        haptics.play(Haptic.Select)
                        onViewTypeToggle()
                    },
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = if (viewType == LibraryViewType.GRID) BitChordIcons.ListView else BitChordIcons.GridView,
                    contentDescription = stringResource(
                        if (viewType == LibraryViewType.GRID) R.string.switch_to_list_view else R.string.switch_to_grid_view,
                    ),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(18.dp),
                )
            }
        }

        if (showSort) {
            Box {
                Box(
                    modifier = Modifier
                        .size(32.dp)
                        .clip(CircleShape)
                        .clickable { sortMenuOpen = true },
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        Icons.AutoMirrored.Rounded.Sort,
                        contentDescription = stringResource(R.string.sort_music),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(18.dp),
                    )
                }

                DropdownMenu(
                    expanded = sortMenuOpen,
                    onDismissRequest = { sortMenuOpen = false },
                ) {
                    ArtistSort.entries.forEach { option ->
                        DropdownMenuItem(
                            text = { Text(option.label()) },
                            trailingIcon = if (option == sortOrder) {
                                {
                                    Icon(
                                        Icons.Rounded.Check,
                                        contentDescription = stringResource(R.string.selected),
                                        modifier = Modifier.size(18.dp),
                                    )
                                }
                            } else {
                                null
                            },
                            onClick = {
                                sortMenuOpen = false
                                if (option != sortOrder) {
                                    haptics.play(Haptic.Select)
                                    onSortOrderChange(option)
                                }
                            },
                        )
                    }
                }
            }
        }
    }
}
