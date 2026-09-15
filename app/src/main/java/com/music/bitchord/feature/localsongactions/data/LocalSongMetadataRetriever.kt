package com.music.bitchord.feature.localsongactions.data

import android.content.Context
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.os.Build
import com.music.bitchord.data.model.Song
import com.music.bitchord.feature.localsongactions.domain.model.LocalSongFullMetadata
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.util.Locale

/**
 * Extracts comprehensive metadata and audio properties from local audio files.
 */
object LocalSongMetadataRetriever {

    suspend fun retrieve(context: Context, song: Song): LocalSongFullMetadata = withContext(Dispatchers.IO) {
        val retriever = MediaMetadataRetriever()
        var hasDataSource = false

        try {
            if (!song.localUri.isNullOrBlank()) {
                retriever.setDataSource(context, Uri.parse(song.localUri))
                hasDataSource = true
            } else if (!song.localPath.isNullOrBlank() && File(song.localPath).exists()) {
                retriever.setDataSource(song.localPath)
                hasDataSource = true
            }
        } catch (_: Throwable) {
            hasDataSource = false
        }

        try {
            val rawTitle = if (hasDataSource) retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_TITLE) else null
            val rawArtist = if (hasDataSource) retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_ARTIST) else null
            val rawAlbum = if (hasDataSource) retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_ALBUM) else null
            val rawAlbumArtist = if (hasDataSource) retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_ALBUMARTIST) else null
            val rawComposer = if (hasDataSource) retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_COMPOSER) else null
            val rawGenre = if (hasDataSource) retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_GENRE) else null
            val rawYear = if (hasDataSource) {
                retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_YEAR)
                    ?: retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DATE)
            } else null

            val rawTrack = if (hasDataSource) retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_CD_TRACK_NUMBER) else null
            val totalTracks = if (hasDataSource) retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_NUM_TRACKS) else null
            val rawDisc = if (hasDataSource) retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DISC_NUMBER) else null
            val rawAuthor = if (hasDataSource) retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_AUTHOR) else null
            val rawWriter = if (hasDataSource) retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_WRITER) else null

            val rawBitrate = if (hasDataSource) retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_BITRATE) else null
            val rawDuration = if (hasDataSource) retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION) else null
            val rawMimeType = if (hasDataSource) retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_MIMETYPE) else null

            val rawSampleRate = if (hasDataSource && Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_SAMPLERATE)
            } else null

            // File properties
            val file = song.localPath?.let { File(it) }?.takeIf { it.exists() }
            val fileSizeBytes = file?.length() ?: runCatching {
                song.localUri?.let { uriStr ->
                    context.contentResolver.openFileDescriptor(Uri.parse(uriStr), "r")?.use { it.statSize }
                }
            }.getOrNull() ?: 0L

            val fileSizeFormatted = formatFileSize(fileSizeBytes)
            val pathFormatted = file?.absolutePath ?: song.localPath ?: song.localUri

            val extension = pathFormatted?.substringAfterLast('.', "")?.uppercase(Locale.ROOT)
            val format = if (!extension.isNullOrBlank()) extension else rawMimeType?.substringAfter('/')?.uppercase(Locale.ROOT)

            val isLossless = format in setOf("FLAC", "WAV", "ALAC", "APE")

            val bitrateFormatted = rawBitrate?.toIntOrNull()?.let { bps ->
                val kbps = bps / 1000
                if (kbps > 0) "$kbps kbps" else null
            }

            val sampleRateFormatted = rawSampleRate?.toIntOrNull()?.let { hz ->
                if (hz >= 1000) "%.1f kHz".format(Locale.US, hz / 1000.0) else "$hz Hz"
            }

            val durationMs = rawDuration?.toLongOrNull()
            val durationFormatted = durationMs?.let { formatDuration(it) } ?: song.durationText

            LocalSongFullMetadata(
                title = rawTitle?.takeUnless { it.isBlank() } ?: song.title,
                artist = rawArtist?.takeUnless { it.isBlank() } ?: song.artist,
                album = rawAlbum?.takeUnless { it.isBlank() } ?: song.albumName,
                albumArtist = rawAlbumArtist?.takeUnless { it.isBlank() },
                trackNumber = formatNumberAndTotal(rawTrack, totalTracks),
                discNumber = formatNumberAndTotal(rawDisc, null) ?: "01/01",
                publisher = rawAuthor?.takeUnless { it.isBlank() } ?: rawWriter?.takeUnless { it.isBlank() },
                composer = rawComposer?.takeUnless { it.isBlank() },
                genre = rawGenre?.takeUnless { it.isBlank() },
                year = rawYear?.takeUnless { it.isBlank() },
                durationText = durationFormatted,
                fileSize = fileSizeFormatted,
                filePath = pathFormatted,
                format = format,
                bitrate = bitrateFormatted,
                sampleRate = sampleRateFormatted,
                channels = "Stereo",
                isLossless = isLossless,
            )
        } finally {
            runCatching { retriever.release() }
        }
    }

    private fun formatNumberAndTotal(numberStr: String?, totalStr: String?): String? {
        if (numberStr.isNullOrBlank()) return null
        if (numberStr.contains('/')) {
            val parts = numberStr.split('/')
            val num = parts.getOrNull(0)?.trim()?.toIntOrNull()
            val total = parts.getOrNull(1)?.trim()?.toIntOrNull()
            return if (num != null && total != null) {
                "%02d/%02d".format(Locale.US, num, total)
            } else if (num != null) {
                "%02d".format(Locale.US, num)
            } else numberStr
        }

        val num = numberStr.trim().toIntOrNull() ?: return numberStr
        val total = totalStr?.trim()?.toIntOrNull()
        return if (total != null && total > 0) {
            "%02d/%02d".format(Locale.US, num, total)
        } else {
            "%02d".format(Locale.US, num)
        }
    }

    private fun formatFileSize(bytes: Long): String {
        if (bytes <= 0) return "0 B"
        val kb = bytes / 1024.0
        val mb = kb / 1024.0
        val gb = mb / 1024.0
        return when {
            gb >= 1.0 -> "%.2f GB".format(Locale.US, gb)
            mb >= 1.0 -> "%.1f MB".format(Locale.US, mb)
            kb >= 1.0 -> "%.1f KB".format(Locale.US, kb)
            else -> "$bytes B"
        }
    }

    private fun formatDuration(durationMs: Long): String {
        val totalSeconds = durationMs / 1000
        val minutes = totalSeconds / 60
        val seconds = totalSeconds % 60
        return "%d:%02d".format(Locale.US, minutes, seconds)
    }
}
