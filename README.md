<div align="center">

<img src="app/src/main/assets/branding/videoplayer_icon.svg" width="120" height="120" alt="V&M Player" /><br>

# V&M Player

**Lightweight, high-performance video & music player for Android.**  
Powered by the raw playback power of **mpv** and a modern **Material 3 / Liquid Glass** interface.

[![Release](https://img.shields.io/badge/Release-v1.0.0-00F5FF?style=flat-square&logo=android&logoColor=black)](https://github.com/Krishna1998/Video-Music-Player/releases)
[![Android](https://img.shields.io/badge/Android-8.0%2B-3DDC84?style=flat-square&logo=android&logoColor=white)](https://developer.android.com)
[![Engine](https://img.shields.io/badge/Core-mpv%20%2B%20FFmpeg-9D4EDD?style=flat-square&logo=cplusplus&logoColor=white)](https://mpv.io)
[![UI](https://img.shields.io/badge/UI-Jetpack%20Compose-4285F4?style=flat-square&logo=jetpackcompose&logoColor=white)](https://developer.android.com/jetpack/compose)
[![Privacy](https://img.shields.io/badge/Telemetry-Zero-00E676?style=flat-square&logo=shield&logoColor=black)](https://github.com)
[![CI/CD Build](https://github.com/Krishna1998/AIO-Player/actions/workflows/build.yml/badge.svg)](https://github.com/Krishna1998/AIO-Player/actions/workflows/build.yml)

</div>

---

## ✨ Features

### 🎬 Video Playback
- **mpv Native Core**: Full hardware decoding (`mediacodec`) and wide codec support via FFmpeg.
- **Crystal-Clear Subtitles**: Full styling support for ASS, SSA, SRT, and PGS karaoke subtitles.
- **Intuitive Gestures**: Smooth vertical swipes for brightness/volume and horizontal swipes for precise seeking.
- **Picture-in-Picture & Background Play**: Keep watching or listening seamlessly while multitasking.
- **Customizable HUD**: Clean controls, dynamic ambient glow, and adjustable UI opacity.

### 🎵 Music & Audio
- **Lossless Audio**: Pristine playback for FLAC, ALAC, WAV, AAC, MP3, OPUS, and more.
- **Equalizer & Sound Tuning**: 10-band equalizer with Bass Boost, Treble, and spatial widening.
- **Live Wavy Seekbar & Visualizers**: Ultra-smooth animated waveform and One UI 9 wavy visuals.
- **Synchronized Lyrics**: Timed line-by-line and word-by-word lyrics from over 15 online providers.
- **Smart Sleep Timer**: Gentle audio fade-out and active-only countdown timer.

### 🔮 Fusion Hub & Design
- **Unified Media Browser**: Customizable rails for Videos, Music, and Recent items.
- **Reorderable Music Tabs**: Easily organize All Songs, Favorites, Playlists, Folders, Albums, and Artists.
- **Liquid Glass Aesthetics**: Elegant frosted glass effects, AMOLED pure black, and curated color palettes.
- **Custom Launcher Icons**: 10 hand-crafted app icon designs with Material You dynamic color support.
- **Jitter-Free Navigation**: Hardware-accelerated panel transitions for silky-smooth 120Hz displays.

### 🛡️ 100% Private & Lightweight
- **Zero Tracking**: No analytics, telemetry, or background monitoring.
- **Zero Google Play Services**: Works standalone and fully offline.
- **Small Footprint**: Fast launch speed, minimal battery consumption, and light memory usage.

---

## 🛠️ Building

### Requirements
- **JDK 17**
- **Android SDK** (API 26+)
- **CMake 3.22.1** & Android NDK 27+

### Build APK

```bash
# Debug build
./gradlew assembleStandardDebug

# Release build
./gradlew assembleStandardRelease
```

---

<div align="center">

Licensed under **AGPL-3.0**

</div>
