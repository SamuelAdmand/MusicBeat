package com.music.bitchord.playback.equalizer

/**
 * Standard equalizer presets with 5-band gain offsets in millibels (100 mB = 1 dB).
 *
 * The standard center frequencies used across most Android devices for a 5-band EQ
 * are roughly: 60 Hz, 230 Hz, 910 Hz, 3.6 kHz, and 14 kHz.
 */
data class EqualizerPreset(
    val id: Int,
    val name: String,
    val bandLevels: List<Short>, // in millibels
) {
    companion object {
        val PRESETS = listOf(
            EqualizerPreset(
                id = 0,
                name = "Flat",
                bandLevels = listOf(0, 0, 0, 0, 0),
            ),
            EqualizerPreset(
                id = 1,
                name = "Bass Boost",
                bandLevels = listOf(600, 400, 100, 0, 0),
            ),
            EqualizerPreset(
                id = 2,
                name = "Rock",
                bandLevels = listOf(450, 250, -100, 250, 400),
            ),
            EqualizerPreset(
                id = 3,
                name = "Pop",
                bandLevels = listOf(-150, 150, 400, 200, -100),
            ),
            EqualizerPreset(
                id = 4,
                name = "Jazz",
                bandLevels = listOf(350, 200, -150, 200, 350),
            ),
            EqualizerPreset(
                id = 5,
                name = "Classical",
                bandLevels = listOf(400, 250, -150, 250, 350),
            ),
            EqualizerPreset(
                id = 6,
                name = "Electronic",
                bandLevels = listOf(500, 350, 0, 250, 450),
            ),
            EqualizerPreset(
                id = 7,
                name = "Hip Hop",
                bandLevels = listOf(500, 300, 0, 150, 300),
            ),
            EqualizerPreset(
                id = 8,
                name = "Vocal Boost",
                bandLevels = listOf(-200, 100, 450, 300, 100),
            ),
        )

        const val CUSTOM_PRESET_ID = -1

        fun getPresetById(id: Int): EqualizerPreset? = PRESETS.firstOrNull { it.id == id }
    }
}
