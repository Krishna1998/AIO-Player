<div align="center">

<img src="app/src/main/res/mipmap-xxxhdpi/ic_launcher_round.png" width="110" alt="Horizon Player Logo" />

# Horizon Player

**A private, offline-first Android video & Hi-Res music player powered by `mpv`, `FFmpeg`, and `libplacebo`, crafted with Material 3 Expressive Liquid Glass.**

[![Release](https://img.shields.io/github/v/release/Krishna1998/Horizon-Player?style=for-the-badge&color=a855f7)](https://github.com/Krishna1998/Horizon-Player/releases/latest)
[![Downloads](https://img.shields.io/github/downloads/Krishna1998/Horizon-Player/total?style=for-the-badge&color=ec4899)](https://github.com/Krishna1998/Horizon-Player/releases)
[![Stars](https://img.shields.io/github/stars/Krishna1998/Horizon-Player?style=for-the-badge&color=eab308)](https://github.com/Krishna1998/Horizon-Player/stargazers)
[![Android](https://img.shields.io/badge/Android-8.0%2B-3ddc84?style=for-the-badge&logo=android&logoColor=white)](https://github.com/Krishna1998/Horizon-Player/releases/latest)
[![Engine](https://img.shields.io/badge/Engine-mpv%20%2B%20FFmpeg-6366f1?style=for-the-badge)](https://mpv.io/)
[![Website](https://img.shields.io/badge/Website-Live-38bdf8?style=for-the-badge)](https://krishna1998.github.io/Horizon-Player/)
[![License](https://img.shields.io/badge/License-AGPL--3.0-blue?style=for-the-badge)](LICENSE)

</div>

---

## 📸 Screenshots

| Home & Continue Watching | Music & Audio Pulsing | Audio Player |
| :---: | :---: | :---: |
| <img src="docs/screenshots/home_hub.jpg" width="240" alt="Home Hub" /> | <img src="docs/screenshots/music_rail.jpg" width="240" alt="Music Rail" /> | <img src="docs/screenshots/audio_player.jpg" width="240" alt="Audio Player" /> |

| Synced Lyrics | Movie Details & Episodes | Preferences |
| :---: | :---: | :---: |
| <img src="docs/screenshots/audio_lyrics.jpg" width="240" alt="Synced Lyrics" /> | <img src="docs/screenshots/movie_details.jpg" width="240" alt="Movie Details" /> | <img src="docs/screenshots/preferences.jpg" width="240" alt="Preferences" /> |

---

## ✨ Features

### 🎬 Pro Video Playback
- **mpv 0.42 + FFmpeg 9.1 + libplacebo:** Hardware-accelerated (`HW+ Direct`) playback with 10-bit HDR tone-mapping.
- **Smart Movies & Series Hub:** Automatic poster artwork, watch progress bars, merged series episodes, and direct resume.
- **SVG Format Badges:** Instant codec, resolution, bit-depth, audio channel, and subtitle badges (`MP4`, `H.265`, `AV1`, `10-BIT`, `5.1`, `SRT`).
- **Portrait & Landscape PiP:** Seamless Picture-in-Picture matching the video's orientation.

### 🎵 Hi-Res Music & Synced Lyrics
- **Lossless Audio:** Native support for `FLAC`, `ALAC`, `WAV`, `OPUS`, `AAC`, `AC3`, and `DTS` with SVG quality badges.
- **Audio Pulsing & Transport:** Expressive waveform seekbar, A-B loop, 10-band equalizer, sleep timer, and speed control.
- **Synced Lyrics:** Real-time embedded and online lyrics with fullscreen mode.

### 🔮 Liquid Glass & Privacy
- **Material 3 Expressive:** Frosted Liquid Glass optics, OLED dark theme, custom wallpapers, and configurable player layouts.
- **Biometric Secure Vault:** Lock private media folders behind fingerprint/biometric authentication.
- **100% Offline & Ad-Free:** Zero ads, zero tracking, zero telemetry.

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

## 🙏 Credits

- **MPVEX**
- **MPVRX**
- **REX PLAYER**

---

<div align="center">

Licensed under **AGPL-3.0**

</div>
