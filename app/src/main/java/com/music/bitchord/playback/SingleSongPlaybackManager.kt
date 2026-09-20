package com.music.bitchord.playback

import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Manages the "Play Single Song & Close Player" playback mode.
 *
 * When enabled, the player will only play the currently selected song.
 * Once the song finishes playing, playback automatically pauses, stays
 * on the completed song, and dispatches an event to close/collapse
 * the full-screen Now Playing screen.
 */
object SingleSongPlaybackManager {

    private val _enabled = MutableStateFlow(false)
    val enabled: StateFlow<Boolean> = _enabled.asStateFlow()

    private val _closePlayerEvent = MutableSharedFlow<Unit>(extraBufferCapacity = 1)
    val closePlayerEvent: SharedFlow<Unit> = _closePlayerEvent.asSharedFlow()

    fun toggle() {
        _enabled.value = !_enabled.value
    }

    fun setEnabled(value: Boolean) {
        _enabled.value = value
    }

    fun disable() {
        _enabled.value = false
    }

    /**
     * Triggered when the song finishes. Notifies UI to close the player
     * and automatically disarms the single-song mode.
     */
    fun triggerClosePlayer() {
        _closePlayerEvent.tryEmit(Unit)
        _enabled.value = false
    }
}
