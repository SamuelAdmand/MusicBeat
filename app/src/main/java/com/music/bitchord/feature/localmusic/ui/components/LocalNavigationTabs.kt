package com.music.bitchord.feature.localmusic.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.QueueMusic
import androidx.compose.material.icons.rounded.Album
import androidx.compose.material.icons.rounded.Folder
import androidx.compose.material.icons.rounded.MusicNote
import androidx.compose.material.icons.rounded.Person
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ScrollableTabRow
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRowDefaults
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.music.bitchord.feature.localmusic.domain.model.LocalMusicTab

/**
 * Tab bar for navigating between local music collections:
 * Tracks, Albums, Artists, Folders, and Playlists.
 */
@Composable
fun LocalNavigationTabs(
    selectedTab: LocalMusicTab,
    onTabSelected: (LocalMusicTab) -> Unit,
    modifier: Modifier = Modifier,
) {
    val tabs = remember {
        listOf(
            Triple(LocalMusicTab.TRACKS, "Tracks", Icons.Rounded.MusicNote),
            Triple(LocalMusicTab.ALBUMS, "Albums", Icons.Rounded.Album),
            Triple(LocalMusicTab.ARTISTS, "Artists", Icons.Rounded.Person),
            Triple(LocalMusicTab.FOLDERS, "Folders", Icons.Rounded.Folder),
            Triple(LocalMusicTab.PLAYLISTS, "Playlists", Icons.AutoMirrored.Rounded.QueueMusic),
        )
    }

    ScrollableTabRow(
        selectedTabIndex = selectedTab.index,
        modifier = modifier.fillMaxWidth(),
        containerColor = Color.Transparent,
        contentColor = MaterialTheme.colorScheme.primary,
        edgePadding = 16.dp,
        divider = {},
        indicator = { tabPositions ->
            if (selectedTab.index < tabPositions.size) {
                TabRowDefaults.SecondaryIndicator(
                    modifier = Modifier.tabIndicatorOffset(tabPositions[selectedTab.index]),
                    color = MaterialTheme.colorScheme.primary,
                    height = 3.dp,
                )
            }
        },
    ) {
        tabs.forEach { (tab, title, icon) ->
            val isSelected = selectedTab == tab
            val textColor by animateColorAsState(
                targetValue = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                label = "tab_text_color",
            )
            val iconColor by animateColorAsState(
                targetValue = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                label = "tab_icon_color",
            )

            Tab(
                selected = isSelected,
                onClick = { onTabSelected(tab) },
                modifier = Modifier
                    .clip(RoundedCornerShape(12.dp))
                    .padding(horizontal = 4.dp, vertical = 8.dp),
                interactionSource = remember { MutableInteractionSource() },
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = title,
                        tint = iconColor,
                        modifier = Modifier.padding(end = 6.dp),
                    )
                    Text(
                        text = title,
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                        color = textColor,
                        fontSize = 14.sp,
                    )
                }
            }
        }
    }
}
