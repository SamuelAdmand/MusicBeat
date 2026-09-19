package com.music.bitchord.data.stats

import android.content.Context
import android.net.Uri
import com.music.bitchord.BuildConfig
import com.music.bitchord.data.settings.AppSettings
import com.music.bitchord.data.settings.EqualizerBackup
import com.music.bitchord.data.settings.EqualizerSettings
import com.music.bitchord.data.settings.SearchHistory
import com.music.bitchord.feature.localmusic.data.LocalPlaylistStore
import com.music.bitchord.feature.localmusic.domain.model.LocalPlaylist
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

/**
 * Manages backup and restore of offline player data for MusicBeat:
 * - Local user playlists ([LocalPlaylistStore])
 * - Equalizer & audio effect configurations ([EqualizerSettings])
 * - Offline preferences & blacklisted folders ([AppSettings])
 * - Local listening statistics & aggregates ([ListeningStats])
 * - Search history ([SearchHistory])
 */
object Backup {

    private const val APP_TAG = "musicbeat"
    private const val SCHEMA_VERSION = 1

    private val json = Json {
        ignoreUnknownKeys = true
        encodeDefaults = true
        prettyPrint = true
    }

    /** Suggested filename for export with today's date. */
    fun suggestedName(): String =
        "musicbeat-backup-${DateTimeFormatter.ofPattern("yyyy-MM-dd").format(
            Instant.now().atZone(ZoneId.systemDefault()),
        )}.json"

    /**
     * Writes all offline player data to [target] document picked by user.
     */
    suspend fun exportTo(context: Context, target: Uri): Result<ExportSummary> = withContext(Dispatchers.IO) {
        runCatching {
            val buckets = ListeningStats.exportAll()
            val playlists = LocalPlaylistStore.exportPlaylists()
            val equalizer = EqualizerSettings.exportBackup()
            val settingsMap = AppSettings.exportPrefs().mapNotNull { (key, value) ->
                PrefValue.of(value)?.let { key to it }
            }.toMap()

            val file = BackupFile(
                app = APP_TAG,
                version = SCHEMA_VERSION,
                versionName = BuildConfig.VERSION_NAME,
                exportedAt = Instant.now().toString(),
                settings = settingsMap,
                playlists = playlists,
                equalizer = equalizer,
                listening = buckets,
            )
            val text = json.encodeToString(BackupFile.serializer(), file)
            context.contentResolver.openOutputStream(target, "wt")
                ?.use { it.write(text.toByteArray()) }
                ?: error("Couldn't open that file for writing")

            ExportSummary(
                playlists = playlists.size,
                settings = settingsMap.size,
                months = buckets.size,
                hasEqualizer = equalizer.enabled,
            )
        }
    }

    /**
     * Reads [source] and restores offline player data (playlists, equalizer, settings, history).
     */
    suspend fun importFrom(context: Context, source: Uri): Result<Summary> = withContext(Dispatchers.IO) {
        runCatching {
            val text = context.contentResolver.openInputStream(source)
                ?.use { it.readBytes().decodeToString() }
                ?: error("Couldn't open that file")
            val file = runCatching { json.decodeFromString(BackupFile.serializer(), text) }
                .getOrElse { error("That doesn't look like a valid MusicBeat backup") }

            require(file.app == APP_TAG) { "That backup is from another app: ${file.app}" }
            require(file.version <= SCHEMA_VERSION) {
                "That backup was written by a newer version of MusicBeat"
            }

            LocalPlaylistStore.importPlaylists(file.playlists)
            EqualizerSettings.importBackup(file.equalizer)
            AppSettings.importPrefs(file.settings.mapValues { it.value.decoded() })
            ListeningStats.importAll(file.listening)
            SearchHistory.reload()

            Summary(
                playlists = file.playlists.size,
                settings = file.settings.size,
                months = file.listening.size,
                hasEqualizer = file.equalizer != null,
                from = file.versionName,
                at = file.exportedAt,
            )
        }
    }

    data class ExportSummary(
        val playlists: Int,
        val settings: Int,
        val months: Int,
        val hasEqualizer: Boolean,
    )

    data class Summary(
        val playlists: Int,
        val settings: Int,
        val months: Int,
        val hasEqualizer: Boolean,
        val from: String,
        val at: String,
    )

    @Serializable
    data class BackupFile(
        val app: String = APP_TAG,
        val version: Int = SCHEMA_VERSION,
        val versionName: String = "",
        val exportedAt: String = "",
        val settings: Map<String, PrefValue> = emptyMap(),
        val playlists: List<LocalPlaylist> = emptyList(),
        val equalizer: EqualizerBackup? = null,
        val listening: List<StoredBucket> = emptyList(),
    )

    @Serializable
    data class PrefValue(
        val type: String,
        val value: String? = null,
        val values: List<String> = emptyList(),
    ) {
        fun decoded(): Any? = when (type) {
            BOOLEAN -> value?.toBooleanStrictOrNull()
            INT -> value?.toIntOrNull()
            LONG -> value?.toLongOrNull()
            FLOAT -> value?.toFloatOrNull()
            STRING -> value
            STRING_SET -> values.toSet()
            else -> null
        }

        companion object {
            fun of(value: Any?): PrefValue? = when (value) {
                is Boolean -> PrefValue(BOOLEAN, value.toString())
                is Int -> PrefValue(INT, value.toString())
                is Long -> PrefValue(LONG, value.toString())
                is Float -> PrefValue(FLOAT, value.toString())
                is String -> PrefValue(STRING, value)
                is Set<*> -> PrefValue(STRING_SET, values = value.filterIsInstance<String>())
                else -> null
            }

            private const val BOOLEAN = "bool"
            private const val INT = "int"
            private const val LONG = "long"
            private const val FLOAT = "float"
            private const val STRING = "string"
            private const val STRING_SET = "stringSet"
        }
    }
}
