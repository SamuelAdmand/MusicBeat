package com.music.bitchord.feature.lyricseditor.data

import android.content.Context
import android.net.Uri
import android.os.ParcelFileDescriptor
import android.provider.MediaStore
import android.util.Log
import com.kyant.taglib.TagLib
import com.music.bitchord.data.Http
import com.music.bitchord.data.lyrics.EmbeddedLyrics
import com.music.bitchord.data.lyrics.LrcLib
import com.music.bitchord.data.lyrics.toLrc
import com.music.bitchord.data.model.Song
import com.music.bitchord.feature.lyricseditor.domain.model.LyricsEditorSource
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonPrimitive
import okhttp3.HttpUrl.Companion.toHttpUrl
import okhttp3.Request
import java.io.File

object LocalLyricsManager {

    private const val TAG = "LocalLyricsManager"
    private val downloadedCache = mutableMapOf<String, String>()

    data class DownloadedLyricsResult(
        val plain: String? = null,
        val synced: String? = null,
    ) {
        val hasBoth: Boolean get() = !plain.isNullOrBlank() && !synced.isNullOrBlank()
        val best: String? get() = synced?.ifBlank { null } ?: plain?.ifBlank { null }
    }

    suspend fun readLyrics(
        context: Context,
        song: Song,
        source: LyricsEditorSource,
    ): String = withContext(Dispatchers.IO) {
        when (source) {
            LyricsEditorSource.Embedded -> readEmbeddedLyrics(context, song)
            LyricsEditorSource.File -> readFileLyrics(context, song)
            LyricsEditorSource.Downloaded -> readDownloadedLyrics(context, song)
        }
    }

    suspend fun saveLyrics(
        context: Context,
        song: Song,
        source: LyricsEditorSource,
        lyricsText: String,
    ): Boolean = withContext(Dispatchers.IO) {
        when (source) {
            LyricsEditorSource.Embedded -> saveEmbeddedLyrics(context, song, lyricsText)
            LyricsEditorSource.File -> saveFileLyrics(context, song, lyricsText)
            LyricsEditorSource.Downloaded -> saveDownloadedLyrics(song, lyricsText)
        }
    }

    suspend fun downloadFromLrcLib(
        title: String,
        artist: String,
        durationMs: Long = 0L,
    ): DownloadedLyricsResult = withContext(Dispatchers.IO) {
        val directResult = runCatching {
            val url = "https://lrclib.net/api/search".toHttpUrl().newBuilder()
                .addQueryParameter("track_name", title)
                .addQueryParameter("artist_name", artist)
                .build()
            val request = Request.Builder()
                .url(url)
                .header("User-Agent", "BitChord (https://github.com/bitchord)")
                .build()
            Http.client.newCall(request).execute().use { response ->
                if (response.isSuccessful) {
                    val body = response.body?.string()
                    if (!body.isNullOrBlank()) {
                        val hits = Json { ignoreUnknownKeys = true }.parseToJsonElement(body) as? JsonArray
                        val firstHit = hits?.firstOrNull() as? JsonObject
                        if (firstHit != null) {
                            val synced = firstHit["syncedLyrics"]?.jsonPrimitive?.contentOrNull?.takeIf { it.isNotBlank() }
                            val plain = firstHit["plainLyrics"]?.jsonPrimitive?.contentOrNull?.takeIf { it.isNotBlank() }
                            if (!synced.isNullOrBlank() || !plain.isNullOrBlank()) {
                                DownloadedLyricsResult(plain = plain, synced = synced)
                            } else null
                        } else null
                    } else null
                } else null
            }
        }.getOrNull()

        if (directResult != null) {
            return@withContext directResult
        }

        val lines = runCatching {
            LrcLib.lyrics(title = title, artist = artist, durationMs = durationMs)
        }.getOrNull()

        val synced = lines?.takeIf { it.isNotEmpty() }?.toLrc()
        val plain = lines?.map { it.text }?.filter { it.isNotBlank() }?.joinToString("\n")?.takeIf { it.isNotBlank() }

        DownloadedLyricsResult(plain = plain, synced = synced)
    }

    private fun readEmbeddedLyrics(context: Context, song: Song): String {
        val uri = getUriForSong(song) ?: return ""
        return runCatching {
            val pfd = openFileDescriptor(context, uri, "r") ?: return ""
            pfd.use { desc ->
                val metadata = TagLib.getMetadata(desc.dup().detachFd(), false)
                val props = metadata?.propertyMap.orEmpty()
                props["LYRICS"]?.firstOrNull() ?: props["UNSYNCEDLYRICS"]?.firstOrNull() ?: ""
            }
        }.getOrDefault("")
    }

    private fun saveEmbeddedLyrics(context: Context, song: Song, lyricsText: String): Boolean {
        val uri = getUriForSong(song) ?: return false
        return runCatching {
            val pfd = openFileDescriptor(context, uri, "rw") ?: return false
            pfd.use { desc ->
                val fd = desc.dup().detachFd()
                val current = TagLib.getMetadata(fd, false)?.propertyMap ?: hashMapOf()
                val map = hashMapOf<String, Array<String>>()
                map.putAll(current)
                if (lyricsText.isBlank()) {
                    map.remove("LYRICS")
                    map.remove("UNSYNCEDLYRICS")
                } else {
                    map["LYRICS"] = arrayOf(lyricsText.trim())
                }
                TagLib.savePropertyMap(fd, map)
            }
        }.getOrDefault(false)
    }

    private fun readFileLyrics(context: Context, song: Song): String {
        val filePath = resolveFilePath(context, song) ?: return ""
        val audioFile = File(filePath)
        val lrcFile = File(audioFile.parentFile ?: return "", "${audioFile.nameWithoutExtension}.lrc")
        return if (lrcFile.exists() && lrcFile.isFile) {
            runCatching { lrcFile.readText() }.getOrDefault("")
        } else ""
    }

    private fun saveFileLyrics(context: Context, song: Song, lyricsText: String): Boolean {
        val filePath = resolveFilePath(context, song) ?: return false
        val audioFile = File(filePath)
        val lrcFile = File(audioFile.parentFile ?: return false, "${audioFile.nameWithoutExtension}.lrc")
        return runCatching {
            if (lyricsText.isBlank()) {
                if (lrcFile.exists()) lrcFile.delete() else true
            } else {
                lrcFile.writeText(lyricsText.trim())
                true
            }
        }.getOrDefault(false)
    }

    private fun readDownloadedLyrics(context: Context, song: Song): String {
        return downloadedCache[song.videoId] ?: run {
            val sidecar = EmbeddedLyrics.sidecar(song.localPath)
            sidecar.orEmpty()
        }
    }

    private fun saveDownloadedLyrics(song: Song, lyricsText: String): Boolean {
        downloadedCache[song.videoId] = lyricsText
        return true
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

    private fun resolveFilePath(context: Context, song: Song): String? {
        if (!song.localPath.isNullOrBlank() && File(song.localPath).exists()) {
            return song.localPath
        }
        val uri = getUriForSong(song) ?: return null
        if (uri.scheme == "file") return uri.path
        if (uri.scheme != "content") return null
        return runCatching {
            val proj = arrayOf(MediaStore.Audio.Media.DATA)
            context.contentResolver.query(uri, proj, null, null, null)?.use { cursor ->
                if (cursor.moveToFirst()) {
                    val idx = cursor.getColumnIndex(MediaStore.Audio.Media.DATA)
                    if (idx != -1) cursor.getString(idx) else null
                } else null
            }
        }.getOrNull()
    }
}
