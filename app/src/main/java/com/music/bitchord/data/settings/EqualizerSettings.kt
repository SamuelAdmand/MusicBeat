package com.music.bitchord.data.settings

import android.content.Context
import android.content.SharedPreferences
import com.music.bitchord.playback.equalizer.EqualizerPreset
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.serialization.Serializable

object EqualizerSettings {
    private const val PREF_FILE = "bitchord_equalizer_settings"
    private const val KEY_ENABLED = "eq_enabled"
    private const val KEY_PRESET_ID = "eq_preset_id"
    private const val KEY_BASS_BOOST = "eq_bass_boost"
    private const val KEY_VIRTUALIZER = "eq_virtualizer"
    private const val KEY_BAND_PREFIX = "eq_band_"

    private lateinit var prefs: SharedPreferences

    private val _enabled = MutableStateFlow(false)
    val enabled = _enabled.asStateFlow()

    private val _presetId = MutableStateFlow(0) // Default Flat (id = 0)
    val presetId = _presetId.asStateFlow()

    private val _bandLevels = MutableStateFlow<Map<Int, Short>>(emptyMap())
    val bandLevels = _bandLevels.asStateFlow()

    private val _bassBoostStrength = MutableStateFlow(0)
    val bassBoostStrength = _bassBoostStrength.asStateFlow()

    private val _virtualizerStrength = MutableStateFlow(0)
    val virtualizerStrength = _virtualizerStrength.asStateFlow()

    fun init(context: Context) {
        prefs = context.getSharedPreferences(PREF_FILE, Context.MODE_PRIVATE)
        readAll()
    }

    private fun readAll() {
        if (!this::prefs.isInitialized) return
        _enabled.value = prefs.getBoolean(KEY_ENABLED, false)
        val pId = prefs.getInt(KEY_PRESET_ID, 0)
        _presetId.value = pId
        _bassBoostStrength.value = prefs.getInt(KEY_BASS_BOOST, 0)
        _virtualizerStrength.value = prefs.getInt(KEY_VIRTUALIZER, 0)

        val levels = mutableMapOf<Int, Short>()
        for (i in 0 until 5) {
            val defaultLevel = EqualizerPreset.getPresetById(pId)?.bandLevels?.getOrNull(i) ?: 0
            val saved = prefs.getInt("$KEY_BAND_PREFIX$i", defaultLevel.toInt()).toShort()
            levels[i] = saved
        }
        _bandLevels.value = levels
    }

    fun setEnabled(value: Boolean) {
        _enabled.value = value
        if (this::prefs.isInitialized) {
            prefs.edit().putBoolean(KEY_ENABLED, value).apply()
        }
    }

    fun selectPreset(id: Int) {
        _presetId.value = id
        val preset = EqualizerPreset.getPresetById(id)
        if (preset != null) {
            val newLevels = mutableMapOf<Int, Short>()
            preset.bandLevels.forEachIndexed { index, level ->
                newLevels[index] = level
            }
            _bandLevels.value = newLevels
            if (this::prefs.isInitialized) {
                val editor = prefs.edit().putInt(KEY_PRESET_ID, id)
                newLevels.forEach { (band, level) ->
                    editor.putInt("$KEY_BAND_PREFIX$band", level.toInt())
                }
                editor.apply()
            }
        } else {
            if (this::prefs.isInitialized) {
                prefs.edit().putInt(KEY_PRESET_ID, id).apply()
            }
        }
    }

    fun setBandLevel(band: Int, level: Short) {
        val current = _bandLevels.value.toMutableMap()
        current[band] = level
        _bandLevels.value = current
        _presetId.value = EqualizerPreset.CUSTOM_PRESET_ID

        if (this::prefs.isInitialized) {
            prefs.edit()
                .putInt(KEY_PRESET_ID, EqualizerPreset.CUSTOM_PRESET_ID)
                .putInt("$KEY_BAND_PREFIX$band", level.toInt())
                .apply()
        }
    }

    fun setBassBoostStrength(strength: Int) {
        val clamped = strength.coerceIn(0, 1000)
        _bassBoostStrength.value = clamped
        if (this::prefs.isInitialized) {
            prefs.edit().putInt(KEY_BASS_BOOST, clamped).apply()
        }
    }

    fun setVirtualizerStrength(strength: Int) {
        val clamped = strength.coerceIn(0, 1000)
        _virtualizerStrength.value = clamped
        if (this::prefs.isInitialized) {
            prefs.edit().putInt(KEY_VIRTUALIZER, clamped).apply()
        }
    }

    fun reset() {
        selectPreset(0) // Flat
        setBassBoostStrength(0)
        setVirtualizerStrength(0)
    }

    fun exportBackup(): EqualizerBackup = EqualizerBackup(
        enabled = _enabled.value,
        presetId = _presetId.value,
        bandLevels = _bandLevels.value,
        bassBoostStrength = _bassBoostStrength.value,
        virtualizerStrength = _virtualizerStrength.value,
    )

    fun importBackup(backup: EqualizerBackup?) {
        if (backup == null) return
        setEnabled(backup.enabled)
        _presetId.value = backup.presetId
        setBassBoostStrength(backup.bassBoostStrength)
        setVirtualizerStrength(backup.virtualizerStrength)

        if (backup.bandLevels.isNotEmpty()) {
            _bandLevels.value = backup.bandLevels
            if (this::prefs.isInitialized) {
                val editor = prefs.edit().putInt(KEY_PRESET_ID, backup.presetId)
                backup.bandLevels.forEach { (band, level) ->
                    editor.putInt("$KEY_BAND_PREFIX$band", level.toInt())
                }
                editor.apply()
            }
        } else {
            selectPreset(backup.presetId)
        }
    }
}

@Serializable
data class EqualizerBackup(
    val enabled: Boolean = false,
    val presetId: Int = 0,
    val bandLevels: Map<Int, Short> = emptyMap(),
    val bassBoostStrength: Int = 0,
    val virtualizerStrength: Int = 0,
)
