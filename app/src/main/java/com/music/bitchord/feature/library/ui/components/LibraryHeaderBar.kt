package com.music.bitchord.feature.library.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.music.bitchord.R
import com.music.bitchord.data.settings.LibraryViewType
import com.music.bitchord.ui.haptics.Haptic
import com.music.bitchord.ui.haptics.rememberHaptics
import com.music.bitchord.ui.icons.BitChordIcons

enum class LibraryFilter {
    ALL,
    FAVORITES,
    PLAYLISTS,
}

/**
 * Control bar for Library screen offering category filter chips and view controls.
 */
@Composable
fun LibraryHeaderBar(
    selectedFilter: LibraryFilter,
    onFilterSelect: (LibraryFilter) -> Unit,
    viewType: LibraryViewType,
    onToggleViewType: () -> Unit,
    onCreatePlaylist: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val haptics = rememberHaptics()

    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 6.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Row(
            modifier = Modifier
                .weight(1f)
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            FilterChip(
                selected = selectedFilter == LibraryFilter.ALL,
                onClick = {
                    haptics.play(Haptic.Select)
                    onFilterSelect(LibraryFilter.ALL)
                },
                label = { Text("All") },
                shape = RoundedCornerShape(12.dp),
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                    selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer,
                ),
            )

            FilterChip(
                selected = selectedFilter == LibraryFilter.FAVORITES,
                onClick = {
                    haptics.play(Haptic.Select)
                    onFilterSelect(LibraryFilter.FAVORITES)
                },
                label = { Text(stringResource(R.string.favorites)) },
                shape = RoundedCornerShape(12.dp),
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                    selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer,
                ),
            )

            FilterChip(
                selected = selectedFilter == LibraryFilter.PLAYLISTS,
                onClick = {
                    haptics.play(Haptic.Select)
                    onFilterSelect(LibraryFilter.PLAYLISTS)
                },
                label = { Text(stringResource(R.string.playlists)) },
                shape = RoundedCornerShape(12.dp),
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                    selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer,
                ),
            )
        }

        Spacer(modifier = Modifier.width(8.dp))

        Row(
            horizontalArrangement = Arrangement.spacedBy(4.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(CircleShape)
                    .clickable {
                        haptics.play(Haptic.Select)
                        onToggleViewType()
                    },
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = if (viewType == LibraryViewType.GRID) BitChordIcons.ListView else BitChordIcons.GridView,
                    contentDescription = stringResource(
                        if (viewType == LibraryViewType.GRID) R.string.switch_to_list_view else R.string.switch_to_grid_view,
                    ),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(20.dp),
                )
            }

            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(CircleShape)
                    .clickable {
                        haptics.play(Haptic.Tap)
                        onCreatePlaylist()
                    },
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = Icons.Rounded.Add,
                    contentDescription = stringResource(R.string.new_playlist),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(22.dp),
                )
            }
        }
    }
}
