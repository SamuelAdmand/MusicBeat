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
        try {
            val pfd = openFileDescriptor(context, song, "r") ?: return@withContext fallbackSongTagData(song)

            pfd.use { descriptor ->
                val metadata = TagLib.getMetadata(descriptor.dup().detachFd(), readPictures = false)
                val propMap = metadata?.propertyMap.orEmpty()
                val audioProps = TagLib.getAudioProperties(descriptor.dup().detachFd())

                fun get(key: String): String = propMap[key]?.firstOrNull()?.trim() ?: propMap[key.uppercase()]?.firstOrNull()?.trim() ?: ""

                val coverPicture = TagLib.getFrontCover(descriptor.dup().detachFd())
                val coverBitmap = coverPicture?.data?.let { bytes ->
                    BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
                }

                SongTagData(
                    title = get("TITLE").ifEmpty { song.title },
                    album = get("ALBUM").ifEmpty { song.albumName ?: "" },
                    artist = get("ARTIST").ifEmpty { song.artist },
                    albumArtist = get("ALBUMARTIST"),
                    composer = get("COMPOSER"),
                    conductor = get("PRODUCER").ifEmpty { get("CONDUCTOR") },
                    publisher = get("COPYRIGHT").ifEmpty { get("LABEL") },
                    genre = get("GENRE"),
                    year = get("DATE").ifEmpty { get("YEAR") },
                    trackNumber = get("TRACKNUMBER"),
                    trackTotal = get("TRACKTOTAL"),
                    discNumber = get("DISCNUMBER"),
                    discTotal = get("DISCTOTAL"),
                    lyrics = get("LYRICS").ifEmpty { get("UNSYNCEDLYRICS") },
                    lyricist = get("LYRICIST"),
                    arranger = get("ARRANGER"),
                    comment = get("COMMENT"),
                    artworkBitmap = coverBitmap,
                )
            }
        } catch (t: Throwable) {
            Log.e(TAG, "Failed reading tags via TagLib for ${song.videoId}", t)
            fallbackSongTagData(song)
        }
    }

    suspend fun writeTags(context: Context, song: Song, data: SongTagData): Boolean = withContext(Dispatchers.IO) {
        try {
            val pfd = openFileDescriptor(context, song, "rw") ?: return@withContext false

            pfd.use { descriptor ->
                val currentProps = TagLib.getMetadata(descriptor.dup().detachFd(), false)?.propertyMap ?: hashMapOf()

                val newMap = hashMapOf<String, Array<String>>()
                newMap.putAll(currentProps)

                fun put(key: String, value: String) {
                    val trimmed = value.trim()
                    if (trimmed.isEmpty()) {
                        newMap.remove(key)
                        newMap.remove(key.uppercase())
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

                val propsOk = TagLib.savePropertyMap(descriptor.dup().detachFd(), cleanedMap)

                if (data.artworkChanged) {
                    if (data.artworkDeleted || data.artworkBitmap == null) {
                        TagLib.savePictures(descriptor.dup().detachFd(), arrayOf())
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
                        TagLib.savePictures(descriptor.dup().detachFd(), arrayOf(pic))
                    }
                }

                val uri = getUriForSong(song)
                val filePath = (if (uri != null) resolveFilePath(context, uri) else null) ?: song.localPath
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

    private fun openFileDescriptor(context: Context, song: Song, mode: String): ParcelFileDescriptor? {
        val uri = getUriForSong(song)
        if (uri != null) {
            val pfd = runCatching {
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
            if (pfd != null) return pfd
        }

        if (!song.localPath.isNullOrBlank()) {
            val file = File(song.localPath)
            if (file.exists()) {
                return runCatching {
                    val pfdMode = if (mode.contains("w")) {
                        ParcelFileDescriptor.MODE_READ_WRITE
                    } else {
                        ParcelFileDescriptor.MODE_READ_ONLY
                    }
                    ParcelFileDescriptor.open(file, pfdMode)
                }.getOrNull()
            }
        }
        return null
    }

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
