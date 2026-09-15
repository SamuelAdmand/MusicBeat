package com.music.bitchord.feature.localsongactions.ui

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import com.music.bitchord.data.model.Song

/**
 * Utility functions for local song actions like sharing.
 */
object LocalSongActionsHelper {

    fun shareSong(context: Context, song: Song) {
        try {
            val uri = song.localUri?.let { Uri.parse(it) }
            val intent = Intent(Intent.ACTION_SEND).apply {
                type = "audio/*"
                if (uri != null) {
                    putExtra(Intent.EXTRA_STREAM, uri)
                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                }
                putExtra(Intent.EXTRA_TEXT, "${song.title} - ${song.artist}")
            }
            val chooser = Intent.createChooser(intent, "Share")
            chooser.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            context.startActivity(chooser)
        } catch (_: Throwable) {
            Toast.makeText(context, "Could not share file", Toast.LENGTH_SHORT).show()
        }
    }
}
