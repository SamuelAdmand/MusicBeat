package com.music.bitchord.ui.screens.equalizer

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.RestartAlt
import androidx.compose.material.icons.rounded.Tune
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.SheetState
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.music.bitchord.R
import com.music.bitchord.data.settings.AppSettings
import com.music.bitchord.data.settings.EqualizerSettings
import com.music.bitchord.ui.screens.equalizer.components.AudioEnhancements
import com.music.bitchord.ui.screens.equalizer.components.EqualizerBandSliders
import com.music.bitchord.ui.screens.equalizer.components.EqualizerPresetChips

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EqualizerSheet(
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
    sheetState: SheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
) {
    val enabled by EqualizerSettings.enabled.collectAsStateWithLifecycle()
    val presetId by EqualizerSettings.presetId.collectAsStateWithLifecycle()
    val bandLevels by EqualizerSettings.bandLevels.collectAsStateWithLifecycle()
    val bassBoostStrength by EqualizerSettings.bassBoostStrength.collectAsStateWithLifecycle()
    val virtualizerStrength by EqualizerSettings.virtualizerStrength.collectAsStateWithLifecycle()
    val sessionId by AppSettings.audioSessionId.collectAsStateWithLifecycle()

    val scrollState = rememberScrollState()

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
        containerColor = MaterialTheme.colorScheme.surface,
        modifier = modifier,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(scrollState)
                .navigationBarsPadding(),
        ) {
            // Header
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Tune,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(24.dp),
                    )
                    Text(
                        text = stringResource(R.string.equalizer),
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    IconButton(
                        onClick = { EqualizerSettings.reset() },
                        enabled = enabled,
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.RestartAlt,
                            contentDescription = "Reset Equalizer",
                            tint = if (enabled) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.outlineVariant,
                        )
                    }

                    Switch(
                        checked = enabled,
                        onCheckedChange = { EqualizerSettings.setEnabled(it) },
                        colors = SwitchDefaults.colors(
                            checkedTrackColor = MaterialTheme.colorScheme.primary,
                            checkedBorderColor = MaterialTheme.colorScheme.primary,
                        ),
                    )
                }
            }

            // Presets
            EqualizerPresetChips(
                selectedPresetId = presetId,
                enabled = enabled,
                onSelectPreset = { EqualizerSettings.selectPreset(it) },
            )

            // Frequency band sliders
            EqualizerBandSliders(
                bandLevels = bandLevels,
                enabled = enabled,
                onBandLevelChange = { band, level ->
                    EqualizerSettings.setBandLevel(band, level)
                },
            )

            // Bass Boost, Virtualizer & System Equalizer action
            AudioEnhancements(
                bassBoostStrength = bassBoostStrength,
                virtualizerStrength = virtualizerStrength,
                enabled = enabled,
                audioSessionId = sessionId,
                onBassBoostChange = { EqualizerSettings.setBassBoostStrength(it) },
                onVirtualizerChange = { EqualizerSettings.setVirtualizerStrength(it) },
            )

            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}
