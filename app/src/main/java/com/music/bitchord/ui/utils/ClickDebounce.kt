package com.music.bitchord.ui.utils

import android.os.SystemClock
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role

/**
 * Thread-safe global click debouncer.
 *
 * Prevents double-dispatch, rapid multi-touch collisions, and accidental simultaneous
 * gestures (e.g. tapping a track and another screen element at the same time) from
 * executing conflicting heavy playback or navigation tasks.
 */
object ClickDebounce {
    const val DEFAULT_COOLDOWN_MS = 350L

    @Volatile
    private var lastClickTime = 0L

    /**
     * Returns `true` if at least [cooldownMs] have passed since the last accepted click.
     * Otherwise returns `false` and suppresses the click.
     */
    fun canClick(cooldownMs: Long = DEFAULT_COOLDOWN_MS): Boolean {
        val now = SystemClock.uptimeMillis()
        synchronized(this) {
            if (now - lastClickTime < cooldownMs) {
                return false
            }
            lastClickTime = now
            return true
        }
    }
}

/**
 * Returns a wrapped lambda that only executes [onClick] if the global click cooldown
 * has elapsed.
 */
@Composable
fun rememberDebouncedClick(
    cooldownMs: Long = ClickDebounce.DEFAULT_COOLDOWN_MS,
    onClick: () -> Unit,
): () -> Unit {
    return remember(onClick, cooldownMs) {
        {
            if (ClickDebounce.canClick(cooldownMs)) {
                onClick()
            }
        }
    }
}

/**
 * A debounced alternative to [Modifier.clickable] that protects against multi-touch
 * spam and rapid taps.
 */
fun Modifier.debouncedClickable(
    cooldownMs: Long = ClickDebounce.DEFAULT_COOLDOWN_MS,
    enabled: Boolean = true,
    onClickLabel: String? = null,
    role: Role? = null,
    onClick: () -> Unit,
): Modifier = this.clickable(
    enabled = enabled,
    onClickLabel = onClickLabel,
    role = role,
) {
    if (ClickDebounce.canClick(cooldownMs)) {
        onClick()
    }
}

/**
 * A debounced alternative to [Modifier.combinedClickable] that protects against
 * simultaneous multi-touch and rapid clicks.
 */
@OptIn(ExperimentalFoundationApi::class)
fun Modifier.debouncedCombinedClickable(
    cooldownMs: Long = ClickDebounce.DEFAULT_COOLDOWN_MS,
    enabled: Boolean = true,
    onClickLabel: String? = null,
    role: Role? = null,
    onLongClickLabel: String? = null,
    onLongClick: (() -> Unit)? = null,
    onDoubleClick: (() -> Unit)? = null,
    onClick: () -> Unit,
): Modifier = this.combinedClickable(
    enabled = enabled,
    onClickLabel = onClickLabel,
    role = role,
    onLongClickLabel = onLongClickLabel,
    onLongClick = onLongClick,
    onDoubleClick = onDoubleClick,
    onClick = {
        if (ClickDebounce.canClick(cooldownMs)) {
            onClick()
        }
    },
)
