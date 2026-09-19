package com.music.bitchord.ui.screens.equalizer.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import java.util.Locale

@Composable
fun EqualizerBandSliders(
    bandLevels: Map<Int, Short>,
    enabled: Boolean,
    onBandLevelChange: (band: Int, level: Short) -> Unit,
    modifier: Modifier = Modifier,
    centerFrequencies: List<Int> = listOf(60_000, 230_000, 910_000, 3_600_000, 14_000_000),
    minLevel: Short = -1500,
    maxLevel: Short = 1500,
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp)
            .background(
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f),
                shape = RoundedCornerShape(16.dp),
            )
            .padding(16.dp)
            .alpha(if (enabled) 1f else 0.4f),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        centerFrequencies.forEachIndexed { band, freqMilliHz ->
            val freqText = formatFrequency(freqMilliHz)
            val currentLevel = bandLevels[band] ?: 0
            val dB = currentLevel.toFloat() / 100f
            val dBText = if (dB > 0) {
                String.format(Locale.US, "+%.1f dB", dB)
            } else {
                String.format(Locale.US, "%.1f dB", dB)
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = freqText,
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.width(64.dp),
                    color = MaterialTheme.colorScheme.onSurface,
                )

                Slider(
                    value = currentLevel.toFloat(),
                    onValueChange = { newValue ->
                        if (enabled) {
                            // Snap to 0 dB if within ±30 mB
                            val snapped = if (kotlin.math.abs(newValue) < 30f) 0f else newValue
                            onBandLevelChange(band, snapped.toInt().toShort())
                        }
                    },
                    valueRange = minLevel.toFloat()..maxLevel.toFloat(),
                    enabled = enabled,
                    modifier = Modifier.weight(1f),
                    colors = SliderDefaults.colors(
                        thumbColor = MaterialTheme.colorScheme.primary,
                        activeTrackColor = MaterialTheme.colorScheme.primary,
                        inactiveTrackColor = MaterialTheme.colorScheme.outlineVariant,
                    ),
                )

                Text(
                    text = dBText,
                    style = MaterialTheme.typography.labelSmall,
                    textAlign = TextAlign.End,
                    modifier = Modifier.width(58.dp),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

private fun formatFrequency(milliHz: Int): String {
    val hz = milliHz / 1000
    return if (hz >= 1000) {
        val kHz = hz / 1000f
        if (kHz % 1f == 0f) "${kHz.toInt()} kHz" else String.format(Locale.US, "%.1f kHz", kHz)
    } else {
        "$hz Hz"
    }
}
