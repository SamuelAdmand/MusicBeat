package com.music.bitchord.feature.localmusic.data

import android.content.Context
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.serialization.builtins.SetSerializer
import kotlinx.serialization.builtins.serializer
import kotlinx.serialization.json.Json
import java.io.File

/**
 * Manages persistence and state for user-favorited local songs.
 */
object LocalFavoritesStore {

    private const val FILE_NAME = "local_favorites.json"
    private val json = Json { ignoreUnknownKeys = true; prettyPrint = true }
    private val serializer = SetSerializer(String.serializer())
    private val scope = CoroutineScope(Dispatchers.IO)

    private lateinit var storageFile: File
    private val _favoriteIds = MutableStateFlow<Set<String>>(emptySet())
    val favoriteIds: StateFlow<Set<String>> = _favoriteIds.asStateFlow()

    fun init(context: Context) {
        storageFile = File(context.filesDir, FILE_NAME)
        load()
    }

    private fun load() {
        if (!storageFile.exists()) {
            _favoriteIds.value = emptySet()
            return
        }
        _favoriteIds.value = runCatching {
            val content = storageFile.readText()
            json.decodeFromString(serializer, content)
        }.getOrDefault(emptySet())
    }

    private fun persist() {
        val current = _favoriteIds.value
        scope.launch {
            runCatching {
                val encoded = json.encodeToString(serializer, current)
                storageFile.writeText(encoded)
            }
        }
    }

    fun isFavorite(songId: String): Boolean {
        return songId in _favoriteIds.value
    }

    fun toggleFavorite(songId: String): Boolean {
        val current = _favoriteIds.value
        val isFav = songId in current
        _favoriteIds.value = if (isFav) current - songId else current + songId
        persist()
        return !isFav
    }

    fun addFavorite(songId: String) {
        if (songId !in _favoriteIds.value) {
            _favoriteIds.value = _favoriteIds.value + songId
            persist()
        }
    }

    fun removeFavorite(songId: String) {
        if (songId in _favoriteIds.value) {
            _favoriteIds.value = _favoriteIds.value - songId
            persist()
        }
    }
}
