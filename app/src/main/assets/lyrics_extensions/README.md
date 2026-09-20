# MusicBeat Lyrics Extensions

Official extension repository for **MusicBeat** lyrics providers, modeled after the SpotiFLAC Mobile modular extension system.

This repository allows lyrics fetching and scraping logic to be updated and distributed dynamically without requiring an update to the core Android application.

---

## Repository Structure

```
MusicBeat-Lyrics-Extensions/
├── registry.json              # Main registry of available extensions & versions
├── README.md
└── extensions/
    ├── betterlyrics/          # BetterLyrics (Apple Music TTML)
    │   ├── manifest.json
    │   └── index.js
    ├── lrclib/                # LRCLIB (Line-synced LRC)
    │   ├── manifest.json
    │   └── index.js
    ├── genius/                # Genius (Plain text web scraper)
    │   ├── manifest.json
    │   └── index.js
    ├── lyricsplus/            # LyricsPlus (YouLy+ multi-mirror)
    │   ├── manifest.json
    │   └── index.js
    ├── paxsenix/              # PaxSenix (Apple/Spotify proxy)
    │   ├── manifest.json
    │   └── index.js
    ├── kugou/                 # KuGou (Chinese & International LRC)
    │   ├── manifest.json
    │   └── index.js
    ├── musixmatch/            # Musixmatch desktop API
    │   ├── manifest.json
    │   └── index.js
    ├── simp-music/            # SimpMusic (VideoId-matched)
    │   ├── manifest.json
    │   └── index.js
    └── megalobiz/             # Megalobiz community LRC
        ├── manifest.json
        └── index.js
```

---

## How Extensions Work

Each extension is a lightweight JavaScript module executed inside MusicBeat's sandboxed QuickJS runtime.

### `manifest.json` Specification

```json
{
  "id": "lrclib",
  "name": "LRCLIB",
  "version": "1.0.0",
  "description": "Free, open community lyrics database with line-synced LRC.",
  "author": "MusicBeat Community",
  "wordSynced": false,
  "defaultPriority": 4
}
```

### `index.js` Specification

Extensions must export a `getLyrics(track)` function:

```javascript
module.exports = {
  /**
   * Fetches lyrics for the given track.
   * @param {Object} track
   * @param {string} track.title Track title
   * @param {string} track.artist Track artist
   * @param {number} track.durationMs Track duration in milliseconds
   * @param {string} [track.album] Album name (optional)
   * @param {string} [track.videoId] YouTube video ID (optional)
   * @returns {Promise<string|null>} Returns raw LRC, TTML, or text string; or null if not found.
   */
  getLyrics: async function(track) {
    // Standard web-compatible fetch() is available in the sandbox:
    const url = "https://lrclib.net/api/get?track_name=" + encodeURIComponent(track.title) +
                "&artist_name=" + encodeURIComponent(track.artist) +
                "&duration=" + Math.floor(track.durationMs / 1000);
    const res = await fetch(url, {
      headers: { "User-Agent": "MusicBeat (https://github.com/bitchord)" }
    });
    if (!res.ok) return null;
    const data = res.json();
    return data.syncedLyrics || data.plainLyrics || null;
  }
};
```

MusicBeat automatically detects whether the returned string is TTML, Enhanced LRC, Karaoke LRC, standard LRC, or plain text!
