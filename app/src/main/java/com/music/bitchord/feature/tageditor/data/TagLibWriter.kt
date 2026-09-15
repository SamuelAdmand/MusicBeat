package com.music.bitchord.feature.tageditor.data

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.media.MediaScannerConnection
import android.net.Uri
import android.os.ParcelFileDescriptor
import android.provider.MediaStore
import android.util.Log
import com.kyant.taglib.Picture
import com.kyant.taglib.TagLib
import com.music.bitchord.data.model.Song
import com.music.bitchord.feature.tageditor.domain.model.SongTagData
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream
import java.io.File
import kotlin.coroutines.resume
import kotlinx.coroutines.suspendCancellableCoroutine

object TagLibWriter {

    private const val TAG = "TagLibWriter"

    suspend fun readTags(context: Context, song: Song): SongTagData = withContext(Dispatchers.IO) {
        val uri = getUriForSong(song)
        if (uri == null) {
            return@withContext fallbackSongTagData(song)
        }

        try {
            val pfd = openFileDescriptor(context, uri, "r")
            if (pfd == null) {
                return@withContext fallbackSongTagData(song)
            }

            pfd.use { descriptor ->
                val metadata = TagLib.getMetadata(descriptor.dup().detachFd(), readPictures = true)
                val properties = metadata?.propertyMap.orEmpty()
                val pictures = metadata?.pictures.orEmpty()

                var coverBitmap: Bitmap? = null
                val frontCover = pictures.firstOrNull { it.pictureType == "Front Cover" } ?: pictures.firstOrNull()
                if (frontCover != null && frontCover.data.isNotEmpty()) {
                    coverBitmap = BitmapFactory.decodeByteArray(frontCover.data, 0, frontCover.data.size)
                }

                fun first(key: String): String =
                    properties[key]?.firstOrNull()?.trim() ?: properties[key.uppercase()]?.firstOrNull()?.trim() ?: ""

                SongTagData(
                    title = first("TITLE").ifEmpty { song.title },
                    album = first("ALBUM").ifEmpty { song.albumName ?: "" },
                    artist = first("ARTIST").ifEmpty { song.artist },
                    albumArtist = first("ALBUMARTIST"),
                    composer = first("COMPOSER"),
                    conductor = first("PRODUCER").ifEmpty { first("CONDUCTOR") },
                    publisher = first("COPYRIGHT").ifEmpty { first("ORGANIZATION") },
                    genre = first("GENRE"),
                    year = first("DATE").ifEmpty { first("YEAR") },
                    trackNumber = first("TRACKNUMBER"),
                    trackTotal = first("TRACKTOTAL"),
                    discNumber = first("DISCNUMBER"),
                    discTotal = first("DISCTOTAL"),
                    lyrics = first("LYRICS").ifEmpty { first("UNSYNCEDLYRICS") },
                    lyricist = first("LYRICIST"),
                    arranger = first("ARRANGER"),
                    comment = first("COMMENT"),
                    artworkBitmap = coverBitmap,
                )
            }
        } catch (t: Throwable) {
            Log.e(TAG, "Failed reading tags via TagLib for ${song.videoId}", t)
            fallbackSongTagData(song)
        }
    }

    suspend fun writeTags(context: Context, song: Song, data: SongTagData): Boolean = withContext(Dispatchers.IO) {
        val uri = getUriForSong(song) ?: return@withContext false

        try {
            val pfd = openFileDescriptor(context, uri, "rw") ?: return@withContext false

            pfd.use { descriptor ->
                val fd = descriptor.dup().detachFd()
                val currentProps = TagLib.getMetadata(fd, false)?.propertyMap ?: hashMapOf()

                val newMap = hashMapOf<String, Array<String>>()
                newMap.putAll(currentProps)

                fun put(key: String, value: String) {
                    val trimmed = value.trim()
                    if (trimmed.isEmpty()) {
                        newMap.remove(key)
                    } else {
                        newMap[key] = arrayOf(trimmed)
                    }
                }

                put("TITLE", data.title)
                put("ALBUM", data.album)
                put("ARTIST", data.artist)
                put("ALBUMARTIST", data.albumArtist)
                put("COMPOSER", data.composer)
                put("PRODUCER", data.conductor)
                put("COPYRIGHT", data.publisher)
                put("GENRE", data.genre)
                put("DATE", data.year)
                put("TRACKNUMBER", data.trackNumber)
                put("TRACKTOTAL", data.trackTotal)
                put("DISCNUMBER", data.discNumber)
                put("DISCTOTAL", data.discTotal)
                put("LYRICS", data.lyrics)
                put("LYRICIST", data.lyricist)
                put("ARRANGER", data.arranger)
                put("COMMENT", data.comment)

                val cleanedMap = newMap
                    .filterKeys { !it.contains(Regex("(?i)REPLAYGAIN_(TRACK|ALBUM)_[A-Z0-7_]+")) }
                    .filterValues { it.isNotEmpty() }
                    .mapValuesTo(hashMapOf()) { it.value }

                val propsOk = TagLib.savePropertyMap(fd, cleanedMap)

                if (data.artworkChanged) {
                    if (data.artworkDeleted || data.artworkBitmap == null) {
                        TagLib.savePictures(fd, arrayOf())
                    } else {
                        val stream = ByteArrayOutputStream()
                        data.artworkBitmap.compress(Bitmap.CompressFormat.JPEG, 95, stream)
                        val bytes = stream.toByteArray()
                        val pic = Picture(
                            data = bytes,
                            description = "Front Cover",
                            pictureType = "Front Cover",
                            mimeType = "image/jpeg",
                        )
                        TagLib.savePictures(fd, arrayOf(pic))
                    }
                }

                val filePath = resolveFilePath(context, uri) ?: song.localPath
                if (!filePath.isNullOrBlank()) {
                    scanFile(context, filePath)
                }

                propsOk
            }
        } catch (t: Throwable) {
            Log.e(TAG, "Failed writing tags via TagLib for ${song.videoId}", t)
            false
        }
    }

    private suspend fun scanFile(context: Context, path: String): String? =
        suspendCancellableCoroutine { cont ->
            MediaScannerConnection.scanFile(context, arrayOf(path), null) { _, scannedUri ->
                if (cont.isActive) cont.resume(scannedUri?.toString())
            }
        }

    private fun getUriForSong(song: Song): Uri? {
        val raw = song.localUri ?: song.videoId
        return when {
            raw.startsWith("content://") || raw.startsWith("file://") -> Uri.parse(raw)
            !song.localPath.isNullOrBlank() -> Uri.fromFile(File(song.localPath))
            else -> null
        }
    }

    private fun openFileDescriptor(context: Context, uri: Uri, mode: String): ParcelFileDescriptor? = runCatching {
        if (uri.scheme == "file") {
            val file = File(uri.path ?: return null)
            val pfdMode = if (mode.contains("w")) {
                ParcelFileDescriptor.MODE_READ_WRITE
            } else {
                ParcelFileDescriptor.MODE_READ_ONLY
            }
            ParcelFileDescriptor.open(file, pfdMode)
        } else {
            context.contentResolver.openFileDescriptor(uri, mode)
        }
    }.getOrNull()

    private fun resolveFilePath(context: Context, uri: Uri): String? = runCatching {
        if (uri.scheme == "file") return uri.path
        if (uri.scheme != "content") return null
        val proj = arrayOf(MediaStore.Audio.Media.DATA)
        context.contentResolver.query(uri, proj, null, null, null)?.use { cursor ->
            if (cursor.moveToFirst()) {
                val idx = cursor.getColumnIndex(MediaStore.Audio.Media.DATA)
                if (idx != -1) cursor.getString(idx) else null
            } else null
        }
    }.getOrNull()

    private fun fallbackSongTagData(song: Song): SongTagData =
        SongTagData(
            title = song.title,
            artist = song.artist,
            album = song.albumName ?: "",
        )
}
