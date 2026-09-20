package com.music.bitchord

import com.music.bitchord.data.stats.Backup
import com.music.bitchord.feature.localmusic.domain.model.LocalPlaylist
import com.music.bitchord.feature.localsongactions.domain.model.LocalPlayStats
import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class BackupTest {

    private val json = Json {
        ignoreUnknownKeys = true
        encodeDefaults = true
        prettyPrint = true
    }

    @Test
    fun backupFile_v2_serializesAndDeserializesCorrectly() {
        val original = Backup.BackupFile(
            app = "musicbeat",
            version = 2,
            versionName = "2.0.0",
            exportedAt = "2026-09-20T12:00:00Z",
            settings = mapOf("dark_theme" to Backup.PrefValue("bool", "true")),
            playlists = listOf(
                LocalPlaylist(
                    id = "pl-1",
                    name = "My Rock Favorites",
                    createdAt = 1000L,
                    songIds = listOf("song-1", "song-2"),
                ),
            ),
            favorites = setOf("song-1", "song-3"),
            playStats = mapOf(
                "song-1" to LocalPlayStats(playedCount = 5, skippedCount = 1, lastPlayedTimestamp = 2000L),
            ),
        )

        val encoded = json.encodeToString(Backup.BackupFile.serializer(), original)
        val decoded = json.decodeFromString(Backup.BackupFile.serializer(), encoded)

        assertEquals("musicbeat", decoded.app)
        assertEquals(2, decoded.version)
        assertEquals("2.0.0", decoded.versionName)
        assertEquals(setOf("song-1", "song-3"), decoded.favorites)
        assertEquals(1, decoded.playStats.size)
        assertEquals(5, decoded.playStats["song-1"]?.playedCount)
        assertEquals(1, decoded.playStats["song-1"]?.skippedCount)
        assertEquals(2000L, decoded.playStats["song-1"]?.lastPlayedTimestamp)
        assertEquals(1, decoded.playlists.size)
        assertEquals("My Rock Favorites", decoded.playlists[0].name)
    }

    @Test
    fun backupFile_v1BackwardCompatibility_decodesGracefully() {
        // v1 payload has no 'favorites' or 'playStats' fields
        val v1Json = """
            {
                "app": "musicbeat",
                "version": 1,
                "versionName": "1.0.0",
                "exportedAt": "2026-01-01T00:00:00Z",
                "settings": {},
                "playlists": [
                    {
                        "id": "pl-old",
                        "name": "Old Playlist",
                        "createdAt": 500,
                        "songIds": ["track-1"]
                    }
                ],
                "listening": []
            }
        """.trimIndent()

        val decoded = json.decodeFromString(Backup.BackupFile.serializer(), v1Json)

        assertEquals("musicbeat", decoded.app)
        assertEquals(1, decoded.version)
        assertEquals("1.0.0", decoded.versionName)
        assertEquals(1, decoded.playlists.size)
        assertEquals("Old Playlist", decoded.playlists[0].name)
        assertTrue(decoded.favorites.isEmpty())
        assertTrue(decoded.playStats.isEmpty())
    }
}
