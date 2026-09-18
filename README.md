<div align="center">

<br/>
<br/>

<img src="Logo.png" alt="MusicBeat app icon" width="200" />

# MusicBeat

### Aesthetic YouTube Music & Local Music Player for Android

<br/>

[![Latest release](https://img.shields.io/github/v/release/SamuelAdmand/MusicBeat?style=for-the-badge&labelColor=0d1117)](https://github.com/SamuelAdmand/MusicBeat/releases)
[![Forked from kushagrasinghx/BitChord](https://img.shields.io/badge/Forked%20From-kushagrasinghx%2FBitChord-blue?style=for-the-badge&labelColor=0d1117)](https://github.com/kushagrasinghx/BitChord)
[![License](https://img.shields.io/github/license/SamuelAdmand/MusicBeat?style=for-the-badge&labelColor=0d1117)](LICENSE)

<br/>

[**Download**](#download) · [**OnePlus Audio Whitelist**](#oneplus-hardware-audio-enhancement) · [**Features**](#features) · [**Credits**](#credits-and-attribution)

</div>

---

> [!NOTE]
> **MusicBeat** is an enhanced, community-driven fork of [BitChord](https://github.com/kushagrasinghx/BitChord) by [Kushagrasinghx](https://github.com/kushagrasinghx). It provides a polished Material 3 experience with lossless audio, word-synced lyrics, and specialized variants tailored for hardware equalizers (Dolby Atmos / Dirac) on OnePlus and OPPO devices.

---

<div align="center">

<img src="Banner.png" alt="MusicBeat banner" width="100%" />

<h1><a id="features"></a>Features</h1>

<table>
  <tr>
    <td width="50%" valign="top">

#### 🎧 Playback & Audio
- **Search, browse, and stream** the entire YouTube Music catalog.
- **Hi-Res lossless audio** — stream FLAC/ALAC from configured module sources with YouTube Music fallback.
- **Hardware audio enhancement support** — native Dolby Atmos & Dirac integration on OnePlus/OPPO devices.
- **Gapless playback with adjustable crossfade** (0–12s).
- **Automix [Beta]** — transition between tracks with on-device tempo matching.
- **Local music library** integration with fast tagging and cover management.
- **Fast Scroller with Alphabet Indicator** — Material Design 2 teardrop bubble perfectly synchronized with viewport items.
- **Offline downloads** — save tracks with embedded metadata.

    </td>
    <td width="50%" valign="top">

#### 🎨 Design & Experience
- **Animated album canvas** — fluid motion artwork on the now-playing screen.
- **Word-synced lyrics** — syllable-level highlighting from multiple scrapers & embedded tags.
- **Dynamic artwork-driven theming** — extracted Material 3 palette.
- **Frosted-glass UI** — translucent frosted bars powered by Haze.

#### 🌐 Connectivity & Controls
- **Discord Rich Presence** — live track, artist, album art, and progress.
- **Scrobbling** to Last.fm and ListenBrainz.
- **System equalizer integration**.
- **Stats for nerds** — codec, bit depth, sample rate, and real-time audio pipeline metrics.

    </td>
  </tr>
</table>

</div>

---

## ⚡ OnePlus Hardware Audio Enhancement

OxygenOS and ColorOS (OnePlus, OPPO, and Realme devices) enforce a strict system-level whitelist for their built-in audio enhancement DSP engines (**Dolby Atmos**, **Dirac Audio**, and **OReality**). Unlisted third-party players cannot access these system equalizers.

To bypass this restriction without root, **MusicBeat** provides dedicated releases packaged under whitelisted IDs:

| Edition | Application ID (`package`) | Supported Devices & Benefits |
| :--- | :--- | :--- |
| **Standard Edition** | `com.samuel.musicbeat` | Recommended for all general Android devices (Google Pixel, Samsung Galaxy, Xiaomi, Motorola, etc.). |
| **OnePlus (QQ Music)** | `com.tencent.qqmusic` | **OnePlus, OPPO, Realme** devices — unlocks native hardware **Dolby Atmos / Dirac** sound enhancement profiles. |
| **OnePlus (KuGou)** | `com.kugou.android` | Alternative whitelisted package for OnePlus/OPPO devices. |

---

## 📥 <a id="download"></a>Download

Get the latest signed APKs directly from the [**Releases**](https://github.com/SamuelAdmand/MusicBeat/releases) page.

| Architecture | Recommendation |
| :--- | :--- |
| **`arm64-v8a`** | **Recommended (99% of modern Android devices)**. Smaller download size and optimized 64-bit native binaries. |
| **`universal`** | Compatible with all devices and architectures. |
| **`armeabi-v7a`** | Older 32-bit devices. |
| **`x86_64`** | Emulators and Intel/AMD-based Android tablets or ChromeOS. |

---

## 🛠️ Building from Source

### Prerequisites
- JDK 17 (Eclipse Temurin recommended)
- Android SDK Platform 34+ and CMake 3.22.1

### Build Commands

```bash
# Clone the repository
git clone https://github.com/SamuelAdmand/MusicBeat.git
cd MusicBeat

# Build Standard Edition Release
./gradlew assembleStandardRelease

# Build OnePlus Audio Whitelist Editions
./gradlew assembleQqmusicRelease
./gradlew assembleKugouRelease

# Build Debug APK for quick local testing
./gradlew assembleDevDebug
```

---

## 🤝 <a id="credits-and-attribution"></a>Credits and Attribution

MusicBeat is built upon the exceptional work of the open-source community:

- **[BitChord](https://github.com/kushagrasinghx/BitChord)** — Created by **[Kushagra Singh](https://github.com/kushagrasinghx)**. All core music streaming, decoding, and client architecture originated in BitChord. Full credit and gratitude go to Kushagra and the BitChord contributors.
- **[Booming Music](https://github.com/mardous/BoomingMusic)** — Reference for Material Design 2 fast scroller and teardrop indicator design.
- **[NewPipeExtractor](https://github.com/TeamNewPipe/NewPipeExtractor)** — YouTube streaming stream resolution and metadata parsing.
- **[TagLib](https://github.com/kyant0/taglib)** — High-performance native audio tag parsing.

---

<div align="center">

Made with ❤️ for music lovers everywhere.

</div>
