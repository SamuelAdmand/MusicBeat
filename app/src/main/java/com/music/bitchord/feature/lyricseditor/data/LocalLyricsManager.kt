package com.music.bitchord.feature.lyricseditor.data

import android.content.Context
import android.net.Uri
import android.os.ParcelFileDescriptor
import android.provider.MediaStore
import android.util.Log
import com.kyant.taglib.TagLib
import com.music.bitchord.data.Http
import com.music.bitchord.data.lyrics.EmbeddedLyrics
import com.music.bitchord.data.lyrics.LyricsRepository
import com.music.bitchord.data.lyrics.LyricsSource
import com.music.bitchord.data.lyrics.cleanArtistForSearch
import com.music.bitchord.data.lyrics.toLrc
import com.music.bitchord.data.model.Song
import com.music.bitchord.data.settings.AppSettings
import com.music.bitchord.feature.lyricseditor.domain.model.LyricsEditorSource
import com.music.bitchord.feature.lyricseditor.domain.model.LyricsSearchResultItem
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
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.serialization.json.doubleOrNull

object LocalLyricsManager {

    private const val TAG = "LocalLyricsManager"
    private val json = Json { ignoreUnknownKeys = true }
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

    /**
     * Automatically writes online lyrics into a local music file's metadata tags without user prompts.
     * If embedded tag saving fails, safely falls back to a companion .lrc file beside the track.
     */
    suspend fun autoEmbedLyrics(
        context: Context,
        song: Song,
        lyricsText: String,
    ): Boolean = withContext(Dispatchers.IO) {
        if (lyricsText.isBlank()) return@withContext false
        val embeddedOk = saveEmbeddedLyrics(context, song, lyricsText)
        if (embeddedOk) {
            return@withContext true
        }
        saveFileLyrics(context, song, lyricsText)
    }

    suspend fun autoDownload(
        title: String,
        artist: String,
        album: String? = null,
        durationMs: Long = 0L,
        sources: Set<LyricsSource> = AppSettings.lyricsSources.value,
        order: List<LyricsSource> = AppSettings.lyricsSourceOrder.value,
        prioritizeSyllableSync: Boolean = AppSettings.prioritizeSyllableSync.value,
    ): DownloadedLyricsResult = withContext(Dispatchers.IO) {
        val repoResult = runCatching {
            LyricsRepository.lyrics(
                videoId = "",
                title = title,
                artist = artist,
                durationMs = durationMs,
                album = album,
                sources = sources,
                order = order,
                prioritizeSyllableSync = prioritizeSyllableSync,
            )
        }.getOrNull()

        if (repoResult != null && repoResult.lines.isNotEmpty()) {
            val lines = repoResult.lines
            val synced = lines.toLrc().takeIf { it.isNotBlank() }
            val plain = lines.map { it.text }.filter { it.isNotBlank() }.joinToString("\n").takeIf { it.isNotBlank() }
            if (!synced.isNullOrBlank() || !plain.isNullOrBlank()) {
                return@withContext DownloadedLyricsResult(plain = plain, synced = synced)
            }
        }

        downloadFromLrcLib(title = title, artist = artist, album = album, durationMs = durationMs)
    }

    suspend fun downloadFromLrcLib(
        title: String,
        artist: String,
        album: String? = null,
        durationMs: Long = 0L,
    ): DownloadedLyricsResult = withContext(Dispatchers.IO) {
        val cleanArtist = cleanArtistForSearch(artist)
        val normalizedArtist = if (artist.contains(";")) artist.replace(";", ",").trim() else artist.trim()
        val directResult = queryLrcLibDirect(title, normalizedArtist, album)
            ?: if (cleanArtist.isNotBlank() && !cleanArtist.equals(normalizedArtist, ignoreCase = true)) {
                queryLrcLibDirect(title, cleanArtist, album)
            } else null

        if (directResult != null) {
            return@withContext directResult
        }

        val lines = runCatching {
            LyricsRepository.lyrics(
                videoId = "",
                title = title,
                artist = normalizedArtist,
                durationMs = durationMs,
                album = album,
            )?.lines ?: if (cleanArtist.isNotBlank() && !cleanArtist.equals(normalizedArtist, ignoreCase = true)) {
                LyricsRepository.lyrics(
                    videoId = "",
                    title = title,
                    artist = cleanArtist,
                    durationMs = durationMs,
                    album = album,
                )?.lines
            } else null
        }.getOrNull()

        val synced = lines?.takeIf { it.isNotEmpty() }?.toLrc()
        val plain = lines?.map { it.text }?.filter { it.isNotBlank() }?.joinToString("\n")?.takeIf { it.isNotBlank() }

        DownloadedLyricsResult(plain = plain, synced = synced)
    }

    private fun queryLrcLibDirect(
        title: String,
        artist: String,
        album: String?,
    ): DownloadedLyricsResult? = runCatching {
        val builder = "https://lrclib.net/api/search".toHttpUrl().newBuilder()
            .addQueryParameter("track_name", title)
            .addQueryParameter("artist_name", artist)
        if (!album.isNullOrBlank()) {
            builder.addQueryParameter("album_name", album)
        }
        val url = builder.build()
        val request = Request.Builder()
            .url(url)
            .header("User-Agent", "BitChord (https://github.com/bitchord)")
            .build()
        Http.client.newCall(request).execute().use { response ->
            if (response.isSuccessful) {
                val body = response.body?.string()
                if (!body.isNullOrBlank()) {
                    val hits = json.parseToJsonElement(body) as? JsonArray
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

    suspend fun searchAllProviders(
        title: String,
        artist: String,
        album: String? = null,
        durationMs: Long = 0L,
        providers: Set<LyricsSource> = AppSettings.lyricsSources.value,
    ): List<LyricsSearchResultItem> = withContext(Dispatchers.IO) {
        val results = mutableListOf<LyricsSearchResultItem>()
        val primaryArtist = cleanArtistForSearch(artist)
        val normalizedArtist = if (artist.contains(";")) artist.replace(";", ",").trim() else artist.trim()

        coroutineScope {
            val jobs = mutableListOf<Deferred<List<LyricsSearchResultItem>>>()

            for (source in providers) {
                if (source.id == "lrclib") {
                    jobs += async {
                        searchLrcLib(title, artist, album)
                    }
                } else {
                    jobs += async {
                        // Try searchLyrics first (multi-result) for extensions that support it
                        val ext = com.music.bitchord.feature.lyrics.manager.LyricsExtensionManager.getExtension(source.id)
                        if (ext != null && ext.scriptFile.exists()) {
                            val searchJson = runCatching {
                                com.music.bitchord.feature.lyrics.engine.LyricsExtensionExecutor.searchLyrics(
                                    extensionId = ext.id,
                                    scriptFile = ext.scriptFile,
                                    title = title,
                                    artist = primaryArtist.ifBlank { normalizedArtist },
                                    album = album,
                                )
                            }.getOrNull()

                            val parsed = if (!searchJson.isNullOrBlank()) {
                                parseExtensionSearchResults(searchJson, source.label)
                            } else emptyList()

                            if (parsed.isNotEmpty()) {
                                parsed
                            } else {
                                // Fallback: use getLyrics for a single result
                                fetchSingleResult(source, title, artist, primaryArtist, normalizedArtist, durationMs, album)
                            }
                        } else {
                            fetchSingleResult(source, title, artist, primaryArtist, normalizedArtist, durationMs, album)
                        }
                    }
                }
            }

            jobs.forEach { job ->
                val items = runCatching { job.await() }.getOrDefault(emptyList())
                results.addAll(items)
            }
        }

        results
    }

    /**
     * Fallback path: calls [LyricsRepository.fetch] (getLyrics) and wraps
     * the single result into a one-element list for the search results dialog.
     */
    private suspend fun fetchSingleResult(
        source: LyricsSource,
        title: String,
        artist: String,
        primaryArtist: String,
        normalizedArtist: String,
        durationMs: Long,
        album: String?,
    ): List<LyricsSearchResultItem> {
        val lines = runCatching {
            LyricsRepository.fetch(
                source = source,
                videoId = "",
                title = title,
                artist = primaryArtist.ifBlank { normalizedArtist },
                durationMs = durationMs,
                album = album,
            )
        }.getOrNull()
        return if (!lines.isNullOrEmpty()) {
            listOf(
                LyricsSearchResultItem(
                    id = "${source.id}_${System.currentTimeMillis()}",
                    title = title,
                    artist = artist,
                    album = album,
                    durationSeconds = (durationMs / 1000).toInt(),
                    provider = source.label,
                    syncedLyrics = lines.toLrc(),
                    plainLyrics = lines.joinToString("\n") { it.text },
                )
            )
        } else emptyList()
    }

    /**
     * Parses the JSON array returned by [LyricsExtensionExecutor.searchLyrics].
     *
     * Each element is expected to have:
     * `{ id, title, artist, album?, durationSeconds?, provider?,
     *    syncedLyrics?, plainLyrics? }`
     */
    private fun parseExtensionSearchResults(
        jsonString: String,
        fallbackProvider: String,
    ): List<LyricsSearchResultItem> = runCatching {
        val array = json.parseToJsonElement(jsonString) as? JsonArray ?: return emptyList()
        array.mapNotNull { element ->
            val obj = element as? JsonObject ?: return@mapNotNull null
            val id = obj["id"]?.jsonPrimitive?.contentOrNull ?: return@mapNotNull null
            val synced = obj["syncedLyrics"]?.jsonPrimitive?.contentOrNull?.takeIf { it.isNotBlank() }
            val plain = obj["plainLyrics"]?.jsonPrimitive?.contentOrNull?.takeIf { it.isNotBlank() }
            if (synced == null && plain == null) return@mapNotNull null
            if (synced?.contains("Wob gopini", ignoreCase = true) == true || plain?.contains("Wob gopini", ignoreCase = true) == true) {
                return@mapNotNull null
            }

            LyricsSearchResultItem(
                id = id,
                title = obj["title"]?.jsonPrimitive?.contentOrNull ?: "",
                artist = obj["artist"]?.jsonPrimitive?.contentOrNull ?: "",
                album = obj["album"]?.jsonPrimitive?.contentOrNull,
                durationSeconds = obj["durationSeconds"]?.jsonPrimitive?.contentOrNull?.toIntOrNull() ?: 0,
                provider = obj["provider"]?.jsonPrimitive?.contentOrNull ?: fallbackProvider,
                syncedLyrics = synced,
                plainLyrics = plain,
            )
        }
    }.getOrDefault(emptyList())

    private fun searchLrcLib(title: String, artist: String, album: String?): List<LyricsSearchResultItem> {
        val primaryArtist = cleanArtistForSearch(artist)
        val normalizedArtist = if (artist.contains(";")) artist.replace(";", ",").trim() else artist.trim()
        val direct = executeLrcLibSearch(title, normalizedArtist, album)
        if (direct.isNotEmpty()) return direct

        if (primaryArtist.isNotBlank() && !primaryArtist.equals(normalizedArtist, ignoreCase = true)) {
            return executeLrcLibSearch(title, primaryArtist, album)
        }
        return emptyList()
    }

    private fun executeLrcLibSearch(title: String, artist: String, album: String?): List<LyricsSearchResultItem> {
        return runCatching {
            val builder = "https://lrclib.net/api/search".toHttpUrl().newBuilder()
            if (title.isNotBlank()) builder.addQueryParameter("track_name", title.trim())
            if (artist.isNotBlank()) builder.addQueryParameter("artist_name", artist.trim())
            if (!album.isNullOrBlank()) builder.addQueryParameter("album_name", album.trim())
            val url = builder.build()

            val request = Request.Builder()
                .url(url)
                .header("User-Agent", "BitChord (https://github.com/bitchord)")
                .build()

            Http.client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) return emptyList()
                val body = response.body?.string() ?: return emptyList()
                val hits = json.parseToJsonElement(body) as? JsonArray ?: return emptyList()

                hits.mapNotNull { element ->
                    val obj = element as? JsonObject ?: return@mapNotNull null
                    val id = obj["id"]?.jsonPrimitive?.contentOrNull ?: return@mapNotNull null
                    val track = obj["trackName"]?.jsonPrimitive?.contentOrNull ?: title
                    val art = obj["artistName"]?.jsonPrimitive?.contentOrNull ?: artist
                    val alb = obj["albumName"]?.jsonPrimitive?.contentOrNull
                    val duration = obj["duration"]?.jsonPrimitive?.doubleOrNull?.toInt() ?: 0
                    val synced = obj["syncedLyrics"]?.jsonPrimitive?.contentOrNull?.takeIf { it.isNotBlank() }
                    val plain = obj["plainLyrics"]?.jsonPrimitive?.contentOrNull?.takeIf { it.isNotBlank() }

                    if (synced == null && plain == null) return@mapNotNull null

                    LyricsSearchResultItem(
                        id = "lrclib_$id",
                        title = track,
                        artist = art,
                        album = alb,
                        durationSeconds = duration,
                        provider = "LRCLIB",
                        syncedLyrics = synced,
                        plainLyrics = plain,
                    )
                }
            }
        }.getOrDefault(emptyList())
    }

    private fun readEmbeddedLyrics(context: Context, song: Song): String {
        return runCatching {
            val pfd = openFileDescriptor(context, song, "r") ?: return ""
            pfd.use { desc ->
                val metadata = TagLib.getMetadata(desc.dup().detachFd(), false)
                val props = metadata?.propertyMap.orEmpty()
                props["LYRICS"]?.firstOrNull() ?: props["UNSYNCEDLYRICS"]?.firstOrNull() ?: ""
            }
        }.getOrDefault("")
    }

    private fun saveEmbeddedLyrics(context: Context, song: Song, lyricsText: String): Boolean {
        return runCatching {
            val pfd = openFileDescriptor(context, song, "rw") ?: return false
            pfd.use { desc ->
                val current = TagLib.getMetadata(desc.dup().detachFd(), false)?.propertyMap ?: hashMapOf()
                val map = hashMapOf<String, Array<String>>()
                map.putAll(current)
                if (lyricsText.isBlank()) {
                    map.remove("LYRICS")
                    map.remove("UNSYNCEDLYRICS")
                } else {
                    map["LYRICS"] = arrayOf(lyricsText.trim())
                }
                val cleanedMap = map
                    .filterKeys { !it.contains(Regex("(?i)REPLAYGAIN_(TRACK|ALBUM)_[A-Z0-7_]+")) }
                    .filterValues { it.isNotEmpty() }
                    .mapValuesTo(hashMapOf()) { it.value }

                val ok = TagLib.savePropertyMap(desc.dup().detachFd(), cleanedMap)
                if (ok) {
                    val path = resolveFilePath(context, song)
                    if (!path.isNullOrBlank()) {
                        android.media.MediaScannerConnection.scanFile(context, arrayOf(path), null, null)
                    }
                }
                ok
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

        val path = resolveFilePath(context, song)
        if (!path.isNullOrBlank()) {
            val file = File(path)
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
