package com.music.bitchord.playback.equalizer

import android.content.Context
import android.content.Intent
import android.media.audiofx.AudioEffect
import android.media.audiofx.BassBoost
import android.media.audiofx.Equalizer
import android.media.audiofx.Virtualizer
import android.util.Log

/**
 * Manages platform audio effects (Equalizer, BassBoost, Virtualizer) attached
 * to ExoPlayer's active audio session, and broadcasts session lifecycle intents
 * so system-level or third-party audio panels can also detect playback.
 */
class AudioEffectManager(private val context: Context) {
    companion object {
        private const val TAG = "AudioEffectManager"
    }

    private var currentSessionId: Int = 0
    private var equalizer: Equalizer? = null
    private var bassBoost: BassBoost? = null
    private var virtualizer: Virtualizer? = null

    // Cached hardware limits (or safe defaults if effect cannot be initialized)
    var bandLevelRange: Pair<Short, Short> = Pair((-1500).toShort(), 1500.toShort())
        private set
    var numberOfBands: Short = 5
        private set
    private val centerFreqs = mutableMapOf<Int, Int>()

    fun setAudioSessionId(
        sessionId: Int,
        enabled: Boolean,
        bandLevels: Map<Int, Short>,
        bassBoostStrength: Int,
        virtualizerStrength: Int,
    ) {
        if (sessionId <= 0 || sessionId == currentSessionId) return

        // Release existing effects and close old session broadcast
        releaseEffects()
        if (currentSessionId > 0) {
            broadcastSessionAction(AudioEffect.ACTION_CLOSE_AUDIO_EFFECT_CONTROL_SESSION, currentSessionId)
        }

        currentSessionId = sessionId
        broadcastSessionAction(AudioEffect.ACTION_OPEN_AUDIO_EFFECT_CONTROL_SESSION, sessionId)

        initEffects(sessionId)
        applyAll(enabled, bandLevels, bassBoostStrength, virtualizerStrength)
    }

    private fun initEffects(sessionId: Int) {
        runCatching {
            equalizer = Equalizer(0, sessionId).apply {
                val numBands = numberOfBands
                this@AudioEffectManager.numberOfBands = numBands
                val range = bandLevelRange
                if (range.size >= 2) {
                    this@AudioEffectManager.bandLevelRange = Pair(range[0], range[1])
                }
                for (i in 0 until numBands) {
                    this@AudioEffectManager.centerFreqs[i.toInt()] = getCenterFreq(i.toShort())
                }
            }
        }.onFailure { e ->
            Log.w(TAG, "Failed to initialize Equalizer: ${e.message}")
            equalizer = null
        }

        runCatching {
            bassBoost = BassBoost(0, sessionId)
        }.onFailure { e ->
            Log.w(TAG, "Failed to initialize BassBoost: ${e.message}")
            bassBoost = null
        }

        runCatching {
            virtualizer = Virtualizer(0, sessionId)
        }.onFailure { e ->
            Log.w(TAG, "Failed to initialize Virtualizer: ${e.message}")
            virtualizer = null
        }
    }

    fun applyAll(
        enabled: Boolean,
        bandLevels: Map<Int, Short>,
        bassBoostStrength: Int,
        virtualizerStrength: Int,
    ) {
        applyEnabled(enabled)
        if (enabled) {
            bandLevels.forEach { (band, level) ->
                applyBandLevel(band, level)
            }
            applyBassBoost(bassBoostStrength)
            applyVirtualizer(virtualizerStrength)
        }
    }

    fun applyEnabled(enabled: Boolean) {
        runCatching { equalizer?.enabled = enabled }
        runCatching { bassBoost?.enabled = enabled }
        runCatching { virtualizer?.enabled = enabled }
    }

    fun applyBandLevel(band: Int, level: Short) {
        runCatching {
            val clamped = level.coerceIn(bandLevelRange.first, bandLevelRange.second)
            equalizer?.setBandLevel(band.toShort(), clamped)
        }
    }

    fun applyBassBoost(strength: Int) {
        runCatching {
            bassBoost?.let {
                if (it.strengthSupported) {
                    it.setStrength(strength.coerceIn(0, 1000).toShort())
                }
            }
        }
    }

    fun applyVirtualizer(strength: Int) {
        runCatching {
            virtualizer?.let {
                if (it.strengthSupported) {
                    it.setStrength(strength.coerceIn(0, 1000).toShort())
                }
            }
        }
    }

    fun getCenterFreq(band: Int): Int {
        return centerFreqs[band] ?: when (band) {
            0 -> 60_000
            1 -> 230_000
            2 -> 910_000
            3 -> 3_600_000
            4 -> 14_000_000
            else -> 1_000_000
        }
    }

    private fun broadcastSessionAction(action: String, sessionId: Int) {
        runCatching {
            val intent = Intent(action).apply {
                putExtra(AudioEffect.EXTRA_AUDIO_SESSION, sessionId)
                putExtra(AudioEffect.EXTRA_PACKAGE_NAME, context.packageName)
                putExtra(AudioEffect.EXTRA_CONTENT_TYPE, AudioEffect.CONTENT_TYPE_MUSIC)
            }
            context.sendBroadcast(intent)
        }
    }

    private fun releaseEffects() {
        runCatching {
            equalizer?.enabled = false
            equalizer?.release()
        }
        equalizer = null

        runCatching {
            bassBoost?.enabled = false
            bassBoost?.release()
        }
        bassBoost = null

        runCatching {
            virtualizer?.enabled = false
            virtualizer?.release()
        }
        virtualizer = null
    }

    fun release() {
        releaseEffects()
        if (currentSessionId > 0) {
            broadcastSessionAction(AudioEffect.ACTION_CLOSE_AUDIO_EFFECT_CONTROL_SESSION, currentSessionId)
            currentSessionId = 0
        }
    }
}
