package com.music.bitchord.feature.lyricseditor.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.music.bitchord.data.lyrics.LyricsSource
import com.music.bitchord.data.model.Song
import com.music.bitchord.data.model.durationMillis
import com.music.bitchord.data.settings.AppSettings
import com.music.bitchord.feature.lyricseditor.data.LocalLyricsManager
import com.music.bitchord.feature.lyricseditor.domain.model.LyricsEditorSource
import com.music.bitchord.feature.lyricseditor.domain.model.LyricsSearchResultItem
import com.music.bitchord.feature.lyricseditor.ui.components.LyricsSelectionMode
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class LyricsEditorViewModel(app: Application) : AndroidViewModel(app) {

    private val _selectedSource = MutableStateFlow(LyricsEditorSource.Embedded)
    val selectedSource: StateFlow<LyricsEditorSource> = _selectedSource.asStateFlow()

    private val _currentText = MutableStateFlow("")
    val currentText: StateFlow<String> = _currentText.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _isSaving = MutableStateFlow(false)
    val isSaving: StateFlow<Boolean> = _isSaving.asStateFlow()

    private val _candidateResult = MutableStateFlow<LocalLyricsManager.DownloadedLyricsResult?>(null)
    val candidateResult: StateFlow<LocalLyricsManager.DownloadedLyricsResult?> = _candidateResult.asStateFlow()

    private val _searchResults = MutableStateFlow<List<LyricsSearchResultItem>>(emptyList())
    val searchResults: StateFlow<List<LyricsSearchResultItem>> = _searchResults.asStateFlow()

    private val _isSearchingResults = MutableStateFlow(false)
    val isSearchingResults: StateFlow<Boolean> = _isSearchingResults.asStateFlow()

    private val _saveResult = MutableSharedFlow<Boolean>()
    val saveResult: SharedFlow<Boolean> = _saveResult.asSharedFlow()

    private val _toastMessage = MutableSharedFlow<String>()
    val toastMessage: SharedFlow<String> = _toastMessage.asSharedFlow()

    private val originalLyricsMap = mutableMapOf<LyricsEditorSource, String>()
    private val editedLyricsMap = mutableMapOf<LyricsEditorSource, String>()
    private var activeSong: Song? = null

    fun loadSong(song: Song) {
        activeSong = song
        viewModelScope.launch {
            _isLoading.value = true
            originalLyricsMap.clear()
            editedLyricsMap.clear()

            for (source in LyricsEditorSource.entries) {
                val text = LocalLyricsManager.readLyrics(getApplication(), song, source)
                originalLyricsMap[source] = text
                editedLyricsMap[source] = text
            }

            _currentText.value = editedLyricsMap[_selectedSource.value].orEmpty()
            _isLoading.value = false
        }
    }

    fun selectSource(source: LyricsEditorSource) {
        // Save currently active text into map before switching
        editedLyricsMap[_selectedSource.value] = _currentText.value
        _selectedSource.value = source
        _currentText.value = editedLyricsMap[source].orEmpty()
    }

    fun updateText(text: String) {
        _currentText.value = text
        editedLyricsMap[_selectedSource.value] = text
    }

    fun undoChanges() {
        val original = originalLyricsMap[_selectedSource.value].orEmpty()
        _currentText.value = original
        editedLyricsMap[_selectedSource.value] = original
    }

    fun autoDownload(
        title: String,
        artist: String,
        album: String? = null,
        providers: Set<LyricsSource>? = null,
    ) {
        viewModelScope.launch {
            _isLoading.value = true
            val sources = providers ?: AppSettings.lyricsSources.value
            val durationMs = activeSong?.durationMillis() ?: 0L
            val result = LocalLyricsManager.autoDownload(
                title = title,
                artist = artist,
                album = album,
                durationMs = durationMs,
                sources = sources,
            )
            _isLoading.value = false

            if (result.hasBoth) {
                _candidateResult.value = result
            } else if (!result.synced.isNullOrBlank()) {
                updateText(result.synced)
                _toastMessage.emit("Synced lyrics loaded")
            } else if (!result.plain.isNullOrBlank()) {
                updateText(result.plain)
                _toastMessage.emit("Plain lyrics loaded")
            } else {
                _toastMessage.emit("No lyrics found online")
            }
        }
    }

    fun searchAllProviders(
        title: String,
        artist: String,
        album: String? = null,
        providers: Set<LyricsSource>,
    ) {
        viewModelScope.launch {
            _isSearchingResults.value = true
            val durationMs = activeSong?.durationMillis() ?: 0L
            val items = LocalLyricsManager.searchAllProviders(
                title = title,
                artist = artist,
                album = album,
                durationMs = durationMs,
                providers = providers,
            )
            _searchResults.value = items
            _isSearchingResults.value = false
        }
    }

    fun clearSearchResults() {
        _searchResults.value = emptyList()
        _isSearchingResults.value = false
    }

    fun applySearchResult(item: LyricsSearchResultItem) {
        if (item.hasSynced && item.hasPlain) {
            _candidateResult.value = LocalLyricsManager.DownloadedLyricsResult(
                plain = item.plainLyrics,
                synced = item.syncedLyrics,
            )
        } else if (item.hasSynced) {
            updateText(item.syncedLyrics!!)
            viewModelScope.launch {
                _toastMessage.emit("Loaded synced lyrics from ${item.provider}")
            }
        } else if (item.hasPlain) {
            updateText(item.plainLyrics!!)
            viewModelScope.launch {
                _toastMessage.emit("Loaded plain lyrics from ${item.provider}")
            }
        }
        clearSearchResults()
    }

    fun downloadLyrics(title: String, artist: String) {
        autoDownload(title, artist)
    }

    fun applyCandidateSelection(mode: LyricsSelectionMode) {
        val candidate = _candidateResult.value ?: return
        val text = when (mode) {
            LyricsSelectionMode.Synced -> candidate.synced ?: candidate.plain
            LyricsSelectionMode.Plain -> candidate.plain ?: candidate.synced
        }
        if (!text.isNullOrBlank()) {
            updateText(text)
        }
        _candidateResult.value = null
    }

    fun dismissCandidateDialog() {
        _candidateResult.value = null
    }

    fun saveLyrics(song: Song) {
        viewModelScope.launch {
            _isSaving.value = true
            editedLyricsMap[_selectedSource.value] = _currentText.value
            val currentSource = _selectedSource.value
            val lyricsToSave = _currentText.value

            val success = LocalLyricsManager.saveLyrics(
                context = getApplication(),
                song = song,
                source = currentSource,
                lyricsText = lyricsToSave,
            )

            if (success) {
                originalLyricsMap[currentSource] = lyricsToSave
            }

            _isSaving.value = false
            _saveResult.emit(success)
        }
    }
}
