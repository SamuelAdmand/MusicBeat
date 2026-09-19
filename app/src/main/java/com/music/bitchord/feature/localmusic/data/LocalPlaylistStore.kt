package com.music.bitchord.feature.localmusic.data

import android.content.Context
import com.music.bitchord.feature.localmusic.domain.model.LocalPlaylist
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.json.Json
import java.io.File
import java.util.UUID

/**
 * Manages persistence and state for user-created offline local music playlists.
 */
object LocalPlaylistStore {

    private const val FILE_NAME = "local_playlists.json"
    private val json = Json { ignoreUnknownKeys = true; prettyPrint = true }
    private val serializer = ListSerializer(LocalPlaylist.serializer())
    private val scope = CoroutineScope(Dispatchers.IO)

    private lateinit var storageFile: File
    private val _playlists = MutableStateFlow<List<LocalPlaylist>>(emptyList())
    val playlists: StateFlow<List<LocalPlaylist>> = _playlists.asStateFlow()

    fun init(context: Context) {
        storageFile = File(context.filesDir, FILE_NAME)
        load()
    }

    private fun load() {
        if (!storageFile.exists()) {
            _playlists.value = emptyList()
            return
        }
        _playlists.value = runCatching {
            val content = storageFile.readText()
            json.decodeFromString(serializer, content)
        }.getOrDefault(emptyList())
    }

    private fun persist() {
        val current = _playlists.value
        scope.launch {
            runCatching {
                val encoded = json.encodeToString(serializer, current)
                storageFile.writeText(encoded)
            }
        }
    }

    fun createPlaylist(name: String): LocalPlaylist {
        val trimmed = name.trim().ifBlank { "New Playlist" }
        val newPlaylist = LocalPlaylist(
            id = UUID.randomUUID().toString(),
            name = trimmed,
            createdAt = System.currentTimeMillis(),
            songIds = emptyList(),
        )
        _playlists.value = _playlists.value + newPlaylist
        persist()
        return newPlaylist
    }

    fun renamePlaylist(id: String, newName: String) {
        val trimmed = newName.trim()
        if (trimmed.isBlank()) return
        _playlists.value = _playlists.value.map { pl ->
            if (pl.id == id) pl.copy(name = trimmed) else pl
        }
        persist()
    }

    fun deletePlaylist(id: String) {
        _playlists.value = _playlists.value.filterNot { it.id == id }
        persist()
    }

    fun addSongToPlaylist(playlistId: String, songId: String, artworkUrl: String? = null) {
        _playlists.value = _playlists.value.map { pl ->
            if (pl.id == playlistId) {
                val updatedIds = if (songId in pl.songIds) pl.songIds else pl.songIds + songId
                val updatedCover = pl.coverUrl ?: artworkUrl
                pl.copy(songIds = updatedIds, coverUrl = updatedCover)
            } else {
                pl
            }
        }
        persist()
    }

    fun removeSongFromPlaylist(playlistId: String, songId: String) {
        _playlists.value = _playlists.value.map { pl ->
            if (pl.id == playlistId) {
                pl.copy(songIds = pl.songIds - songId)
            } else {
                pl
            }
        }
        persist()
    }

    fun getPlaylist(id: String): LocalPlaylist? {
        return _playlists.value.firstOrNull { it.id == id }
    }

    fun exportPlaylists(): List<LocalPlaylist> = _playlists.value

    fun importPlaylists(incoming: List<LocalPlaylist>) {
        _playlists.value = incoming
        persist()
    }
}
