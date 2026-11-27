<div align="center">

<img src="app/src/main/assets/branding/aura_aperture_icon.svg" width="128" height="128" alt="Horizon Player" /><br>

# ✦ HORIZON PLAYER ✦

### `NEXT-GEN CINEMA • BIT-PERFECT HI-RES AUDIO • LIQUID GLASS`

[![Release](https://img.shields.io/github/v/release/Krishna1998/Horizon-Player?style=for-the-badge&color=a855f7&labelColor=0b0914)](https://github.com/Krishna1998/Horizon-Player/releases/latest)
[![Downloads](https://img.shields.io/github/downloads/Krishna1998/Horizon-Player/total?style=for-the-badge&color=ec4899&labelColor=0b0914)](https://github.com/Krishna1998/Horizon-Player/releases)
[![Stars](https://img.shields.io/github/stars/Krishna1998/Horizon-Player?style=for-the-badge&color=eab308&labelColor=0b0914)](https://github.com/Krishna1998/Horizon-Player/stargazers)
[![Android](https://img.shields.io/badge/Android-8.0%2B-3ddc84?style=for-the-badge&logo=android&logoColor=white&labelColor=0b0914)](https://github.com/Krishna1998/Horizon-Player/releases/latest)
[![Engine](https://img.shields.io/badge/Engine-mpv%20%2B%20Oboe%20C%2B%2B-6366f1?style=for-the-badge&labelColor=0b0914)](https://mpv.io/)
[![License](https://img.shields.io/badge/License-AGPL--3.0-8b5cf6?style=for-the-badge&labelColor=0b0914)](LICENSE)

[Download APK](https://github.com/Krishna1998/Horizon-Player/releases/latest) • [Changelog](CHANGELOG.md)

</div>

---

## ⚡ Core Architecture

| Layer | Technology | Capability |
| :--- | :--- | :--- |
| **Video Engine** | `mpv` + `FFmpeg` | `HW+ Direct` decoding, 10-bit HDR, Anime4K shaders, portrait & landscape PiP |
| **Renderer** | `libplacebo` | High-precision tone-mapping, custom GLSL shaders & reference color accuracy |
| **Bit-Perfect Audio** | `Oboe C++` (Exclusive MMAP) | Bypasses Android AudioFlinger resampling for raw USB DAC lossless output |
| **Audiophile DSP** | `15-Band Parametric EQ` | Native C++ PEQ + searchable **AutoEQ** headphone & IEM catalog |
| **Interface** | `Material 3 Expressive` | Frosted **Liquid Glass**, OLED dark theme, dynamic ambient glow, 3-column tablet UI |
| **Privacy & Vault** | `Biometric Vault` | Hardware-backed Secure Folder lock, 100% offline, zero ads & zero telemetry |

---

## 📸 Visual Showcase

### 📱 Phone & Portrait Experience

| Home & Continue Watching | Music & Audio Pulsing | Hi-Res Audio Player |
| :---: | :---: | :---: |
| <img src="docs/screenshots/home_hub.jpg" width="240" alt="Home Hub" /> | <img src="docs/screenshots/music_rail.jpg" width="240" alt="Music Rail" /> | <img src="docs/screenshots/audio_player.jpg" width="240" alt="Audio Player" /> |

| Real-Time Synced Lyrics | Movie Details & Episodes | Expressive Preferences |
| :---: | :---: | :---: |
| <img src="docs/screenshots/audio_lyrics.jpg" width="240" alt="Synced Lyrics" /> | <img src="docs/screenshots/movie_details.jpg" width="240" alt="Movie Details" /> | <img src="docs/screenshots/preferences.jpg" width="240" alt="Preferences" /> |

---

### 🖥️ Tablet & Landscape Studio

<div align="center">
  <em>3-Column Audio Studio • Movies Hub • Video Folders • Music Library • Liquid Glass Appearance • Player & About</em>
</div>

<br>

| Home Hub | Movies & Series | Video Folders |
| :---: | :---: | :---: |
| <img src="docs/screenshots/1.jpg" width="100%" alt="Tablet Home Hub" /> | <img src="docs/screenshots/2.jpg" width="100%" alt="Tablet Movies" /> | <img src="docs/screenshots/3.jpg" width="100%" alt="Tablet Folders" /> |

| Music Library | Appearance & Glass | 3-Column Audio Player |
| :---: | :---: | :---: |
| <img src="docs/screenshots/4.jpg" width="100%" alt="Tablet Music" /> | <img src="docs/screenshots/5.jpg" width="100%" alt="Tablet Appearance" /> | <img src="docs/screenshots/6.jpg" width="100%" alt="Tablet Audio Player" /> |

| Movie Details | Pro Video Player | About & Engine |
| :---: | :---: | :---: |
| <img src="docs/screenshots/7.jpg" width="100%" alt="Tablet Movie Details" /> | <img src="docs/screenshots/8.jpg" width="100%" alt="Tablet Video Player" /> | <img src="docs/screenshots/9.jpg" width="100%" alt="Tablet About" /> |

---

## ✨ Feature Matrix

- **🎧 Bit-Perfect USB DAC & Signal Path:**
  - Exclusive AAudio/MMAP native C++ output (`horizonplayer_audio`) with automatic USB DAC detection.
  - Real-time **Signal Path** & hardware route inspection inside Audio Information and codec badges.
- **🎛️ 15-Band Parametric EQ & AutoEQ:**
  - Native C++ 15-band PEQ with target curve presets and online searchable headphone/IEM catalog.
- **🎵 Hi-Res Music & Synced Lyrics:**
  - 3-column tablet landscape player, waveform seekbar, Audio Pulsing spotlight, A-B loop, sleep timer, and embedded/online synced lyrics with translation.
- **🎬 Pro Video & Movies Hub:**
  - Automatic poster artwork, merged series episodes, watched checkmarks, and crisp SVG format badges (`MP4`, `H.265`, `AV1`, `10-BIT`, `5.1`, `SRT`).
- **🌐 Network & Media Servers:**
  - Stream directly from **Jellyfin**, **Audiobookshelf**, **Navidrome**, SMB/FTP/WebDAV, and custom URLs.
- **🔮 Liquid Glass & Customization:**
  - Customizable blur, wallpapers, OLED dark theme, squircle iconography, and gesture controls.
- **🛡️ 100% Private:**
  - Biometric Secure Folder, no ads, no tracking, no analytics.

---

## 🛠️ Build from Source

```bash
# Debug APK
./gradlew assembleStandardDebug

# Release APK
./gradlew assembleStandardRelease
```

---

## 📄 License

Licensed under **AGPL-3.0**. See [LICENSE](LICENSE) for details.
