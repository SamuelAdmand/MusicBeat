<div align="center">

<br/>

<img src="Logo.png" alt="MusicBeat app icon" width="160" />

# MusicBeat

### Aesthetic, High-Performance Local Music Player for Android
**Lossless Hi-Res Audio · Word-Synced Lyrics · Booming-Style Tag Editor · Native Hardware Equalizer Whitelisting**

<br/>

[![Latest release](https://img.shields.io/github/v/release/SamuelAdmand/MusicBeat?style=for-the-badge&color=6366f1&labelColor=0d1117)](https://github.com/SamuelAdmand/MusicBeat/releases)
[![Platform](https://img.shields.io/badge/Platform-Android%208.0%2B-3DDC84?style=for-the-badge&logo=android&logoColor=white&labelColor=0d1117)](https://developer.android.com)
[![Hardware Audio](https://img.shields.io/badge/Dolby%20Atmos-OnePlus%20%2F%20OPPO%20Whitelisted-FF0055?style=for-the-badge&labelColor=0d1117)](#-oneplus--oppo-hardware-audio-enhancement)
[![Forked from kushagrasinghx/BitChord](https://img.shields.io/badge/Forked%20From-kushagrasinghx%2FBitChord-007ACC?style=for-the-badge&logo=github&logoColor=white&labelColor=0d1117)](https://github.com/kushagrasinghx/BitChord)
[![License](https://img.shields.io/github/license/SamuelAdmand/MusicBeat?style=for-the-badge&color=22c55e&labelColor=0d1117)](LICENSE)

<br/>

[**📥 Download APK**](#-download) · [**⚡ OnePlus Audio Whitelist**](#-oneplus--oppo-hardware-audio-enhancement) · [**✨ Features**](#-features) · [**🛠️ Building from Source**](#️-building-from-source) · [**🤝 Credits**](#-credits-and-attribution)

</div>

---

> [!NOTE]
> **MusicBeat** is an enhanced, privacy-first local music player forked with gratitude from [BitChord](https://github.com/kushagrasinghx/BitChord) by [Kushagra Singh](https://github.com/kushagrasinghx). 
> 
> We removed remote cloud streaming bloat and transformed MusicBeat into a blazing-fast, pure **offline local music player** equipped with native C++ metadata editing, automated lyrics embedding, a Material Design 2 teardrop fast scroller, and specialized packages whitelisted for **Dolby Atmos / Dirac Audio** on OnePlus and OPPO devices.

---

<div align="center">

<img src="Banner.png" alt="MusicBeat banner" width="100%" />

</div>

## ✨ Features

<table>
  <tr>
    <td width="50%" valign="top">

### 🎧 Audiophile Audio Engine
- **Hi-Res Lossless Audio**: Native bit-perfect playback for **FLAC**, **ALAC**, **WAV**, **AAC**, **MP3**, **OPUS**, and **OGG Vorbis**.
- **Gapless Playback & Crossfade**: Smooth transitions with configurable crossfade duration (0 to 12 seconds).
- **Stats for Nerds Overlay**: Inspect real-time audio pipeline metrics:
  - Audio Codec & Container format
  - Sample Rate (up to 192 kHz)
  - Bit Depth (16-bit / 24-bit / 32-bit float)
  - Bitrate & Channel configuration
  - ExoPlayer internal buffer timeline

<br/>

### 🏷️ Booming-Style Tag & Metadata Editor
- **Native C++ TagLib Engine**: Direct, safe, and lightning-fast tag editing for local audio files.
- **Comprehensive Tag Fields**: Modify Title, Artist, Album, Album Artist, Track Number, Disc Number, Year, and Genre.
- **Android Scoped Storage Support**: Built-in permission flows handling MediaStore write requests and optional All Files Access (`MANAGE_EXTERNAL_STORAGE`) for seamless storage writes on Android 10, 11, 12, 13, and 14+.

    </td>
    <td width="50%" valign="top">

### 📜 Synchronized Lyrics Suite
- **Syllable-Level Word Synced Lyrics**: Real-time karaoke-style highlighted lyrics with fluid auto-scrolling.
- **Multi-Provider Lyrics Search**: Dedicated search dialog with Title, Artist, and Album filters to pick from multiple online sources.
- **Auto Background Lyrics Embedding**: Automatically downloads and permanently embeds synced lyrics directly into audio file tags (ID3v2 USLT / SYLT / Vorbis) on playback.

<br/>

### 🔤 Material Design 2 Teardrop Fast Scroller
- **Booming Music Curve Physics**: Pixel-perfect mathematical bezier curves faithfully recreated from Booming Music's `Md2PopupBackground`.
- **Anchored Teardrop Indicator**: Animated spring indicator anchored directly to the scrollbar thumb tip.
- **True Viewport Synchronization**: Alphabet indicator accurately tracks the current top visible song without offset lag.

<br/>

### 🎨 Modern Material 3 Aesthetics
- **Dynamic Theming**: Color palettes dynamically generated from album art.
- **Frosted Glass UI**: Translucent blurred app bars and bottom navigation powered by Haze.
- **Pure Offline Privacy**: Zero ads, zero background tracking, zero analytics, and zero accounts required.

    </td>
  </tr>
</table>

---

## ⚡ OnePlus / OPPO Hardware Audio Enhancement

### The Problem
OxygenOS and ColorOS (found on **OnePlus**, **OPPO**, and **Realme** devices) feature kernel and system-level audio DSPs—including **Dolby Atmos**, **Dirac Audio**, and **OReality Audio**. However, these systems enforce a strict hardcoded whitelist in system frameworks: third-party music players are blocked from using native Dolby Atmos profiles and equalizers.

### The MusicBeat Solution
MusicBeat offers specialized edition packages compiled with whitelisted application IDs. When installed on a OnePlus or OPPO device, the OS recognizes MusicBeat as a whitelisted player, immediately unlocking the system equalizer:

| Edition | Application ID (`package`) | Recommended Devices & Benefits |
| :--- | :--- | :--- |
| **Standard Edition** | `com.samuel.musicbeat` | **All general Android devices** (Google Pixel, Samsung Galaxy, Xiaomi, Motorola, Sony, etc.). |
| **OnePlus (QQ Music)** | `com.tencent.qqmusic` | **OnePlus, OPPO, Realme** devices — unlocks native hardware **Dolby Atmos / Dirac / OReality** equalizer presets and custom curves. |
| **OnePlus (KuGou)** | `com.kugou.android` | Alternative whitelisted package for OnePlus & OPPO devices. |

#### How to Enable Dolby Atmos on OnePlus/OPPO:
1. Download and install either the **OnePlus (QQ Music)** or **OnePlus (KuGou)** APK from [Releases](https://github.com/SamuelAdmand/MusicBeat/releases).
2. Start playing any song in MusicBeat.
3. Open your phone's **Settings** → **Sound & Vibration** → **Dolby Atmos** (or **Dirac Audio** / **OReality Audio**).
4. You will see that Dolby Atmos is fully unlocked and active! You can now switch between Music/Movie/Game presets and tune the multi-band graphic equalizer.

---

## 📥 Download

Pre-built signed APKs are available on the [**GitHub Releases Page**](https://github.com/SamuelAdmand/MusicBeat/releases).

### Which APK Should I Download?

| Variant | Recommended Architecture | Details |
| :--- | :--- | :--- |
| **`arm64-v8a`** | **Recommended (99% of modern phones)** | Optimized 64-bit native binaries for modern processors (Snapdragon, MediaTek, Tensor, Exynos). Smallest download size and best battery efficiency. |
| **`universal`** | Universal Compatibility | Bundles all native libraries into one APK. Choose this if you are unsure of your phone's architecture. |
| **`armeabi-v7a`** | Legacy 32-bit Devices | For older 32-bit Android phones or budget devices. |
| **`x86_64`** | Emulators & Intel/AMD | For Android emulators (Android Studio, BlueStacks, LDPlayer) or x86 tablets/ChromeOS. |

---

## 🗂️ Library & Organization

MusicBeat organizes your local music collection cleanly with instant navigation:

- **Songs**: Comprehensive track list with real-time alphabet fast scroller and instant search.
- **Albums**: Grid view with dynamic color extraction from album art.
- **Artists**: Unified artist discography drill-down with track grouping.
- **Folders**: Browse songs directly by directory hierarchy on internal storage or SD card.
- **Playlists**: Create, manage, and reorder custom playlists.
- **3-Dot Context Menu**: Quick access to **Track Details** (path, file size, bitrate, format), **Play Next**, **Add to Queue**, **Edit Tags**, **Edit Lyrics**, and **Delete**.

---

## 🛠️ Building from Source

### Prerequisites
- Android Studio Ladybug (2024.2.1) or newer
- JDK 17 (Eclipse Temurin recommended)
- Android SDK Platform 34+ / Build-Tools 34+
- CMake 3.22.1+ (for native C++ TagLib build)

### Build Commands

```bash
# Clone repository
git clone https://github.com/SamuelAdmand/MusicBeat.git
cd MusicBeat

# Build Standard Edition (Release)
./gradlew assembleStandardRelease

# Build OnePlus Whitelist Editions (Release)
./gradlew assembleQqmusicRelease
./gradlew assembleKugouRelease

# Build Debug APK for quick local testing
./gradlew assembleDevDebug
```

Compiled APKs will be located in `app/build/outputs/apk/<flavor>/release/`.

---

## 🤝 Credits and Attribution

MusicBeat is made possible thanks to these open-source projects:

- **[BitChord](https://github.com/kushagrasinghx/BitChord)** — Created by **[Kushagra Singh](https://github.com/kushagrasinghx)**. Core UI components, playback architecture, and foundation originated from BitChord. Sincere thanks and credit to Kushagra and all BitChord contributors.
- **[Booming Music](https://github.com/mardous/BoomingMusic)** — Reference for the Material Design 2 teardrop fast scroller and tag editor workflow.
- **[TagLib](https://github.com/kyant0/taglib)** — High-performance C++ audio metadata parser and writer.
- **[Haze](https://github.com/chrisbanes/haze)** — Frosted-glass backdrop blur effects for Jetpack Compose.
- **[ExoPlayer / Media3](https://github.com/androidx/media)** — AndroidX low-latency audio playback engine.

---

<div align="center">

**[MusicBeat](https://github.com/SamuelAdmand/MusicBeat)** · Made with ❤️ for audiophiles and local music lovers.

</div>
