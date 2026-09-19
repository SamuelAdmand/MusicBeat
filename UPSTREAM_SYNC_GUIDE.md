# Upstream Synchronization & Maintenance Guide

This document defines the maintenance strategy for **MusicBeat** to keep critical shared infrastructure up to date with the official upstream repository ([kushagrasinghx/BitChord](https://github.com/kushagrasinghx/BitChord.git)) without breaking custom local/offline features.

---

## Architecture Overview

```
MusicBeat (Local Media Player)
├── 🟢 Critical Shared Components (Sync from Upstream)
│   ├── Lyrics Engine (PaxSenix, Genius, LRCLIB, KuGou, TTML, new providers)
│   └── Playback Engine (ExoPlayer/Media3 lifecycle, AudioTrack sinks, MediaRouter fixes)
│
├── 🔴 Protected Local Features (NEVER Overwrite)
│   ├── Local Music Library & Drill-Down Screens
│   ├── Alphabet Fast Scroller (MD2 teardrop indicator)
│   ├── TagLib Audio Tag Editor (ID3v2, FLAC, Vorbis, MP4)
│   ├── Lyrics Editor (LRC editing & timestamp synchronization)
│   ├── Embedded Lyrics (Tag reading & automatic background embedding)
│   ├── Double-Tap Seek (5s forward/backward with arc ripple)
│   └── Rebranding & OnePlus Audio Whitelist
│
└── ⚪ Excluded Upstream Features (Not applicable)
    ├── YouTube Music / InnerTube audio streaming & fallback ladders
    ├── JioSaavn & third-party addon streaming catalogs
    └── Online account scrobbling & playlist synchronization
```

---

## Critical Components That Need Periodic Updates

### 1. Online Lyrics Providers (`app/src/main/java/com/music/bitchord/data/lyrics/`)
- **PaxSenix (`PaxSenix.kt`)**: The third-party proxy for Musixmatch/Spotify lyrics periodically changes or fails. Upstream updates the endpoints and adds generic LRCGet fallbacks.
- **Genius (`Genius.kt`)**: Genius scrapers break when HTML structure or URL formatting changes. Upstream updates title cleaning and search query normalization.
- **New Providers (`Megalobiz.kt`, `YouTubeLyrics.kt`, `ProviderLyrics.kt`)**: Upstream introduces new lyric APIs and scrapers that improve match rates for local songs.
- **Apple TTML Parser (`TtmlLyrics.kt`)**: Upstream improves line and syllable timing parser accuracy.

### 2. Audio Playback Engine (`app/src/main/java/com/music/bitchord/playback/`)
- **ExoPlayer / Media3 Compatibility**: Upstream fixes OS-level bugs (e.g. `MediaRouter2` crashing on Android 9/10, AudioTrack underruns, or Bluetooth routing issues).
- **Queue Shuffle Performance (`QueueShuffle.kt`)**: Upstream optimizes shuffling logic for large song lists.
- **Equalizer & DSP (`equalizer/`)**: Audio processor improvements.

---

## Step-by-Step Maintenance Workflow (Every Few Months)

Whenever you want to check for and apply upstream improvements, follow these steps:

### 1. Fetch Upstream Commits
```pwsh
git fetch upstream
```

### 2. Inspect Commits in Critical Areas
Filter commits to only the lyrics and playback directories:
```pwsh
git log --oneline main..upstream/main -- `
  app/src/main/java/com/music/bitchord/data/lyrics/ `
  app/src/main/java/com/music/bitchord/playback/
```

### 3. Check Commit Details
Review the diff of any commit that mentions lyric providers or playback fixes:
```pwsh
git show <commit_hash>
```

### 4. Port the Improvements
- **For new standalone files** (e.g. a new lyrics provider):
  ```pwsh
  git checkout upstream/main -- app/src/main/java/com/music/bitchord/data/lyrics/<NewProvider>.kt
  ```
- **For bug fixes in existing files**:
  Port the specific bug fix lines into the file, ensuring local features (e.g. `EmbeddedLyrics`, double-tap seek) remain untouched.

### 5. Verify Build
```pwsh
.\gradlew compileStandardDebugKotlin
```

### 6. Commit
```pwsh
git commit -m "sync(lyrics): update <provider/engine> from upstream (<commit_hash>)"
```

---

## Antigravity Skill Integration

An Antigravity skill has been installed at `.agents/skills/upstream-sync/SKILL.md`.

You can ask Antigravity at any time:
> *"Run the upstream sync skill and check if there are any new lyric or playback fixes upstream."*

Antigravity will automatically inspect the upstream commits, filter out online streaming code, present the relevant fixes for your review, and safely apply them while keeping all your custom features intact.
