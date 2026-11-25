# 🌍 Orbit Live

**Every live TV channel and radio station, in one app.**

Orbit Live is a native Android app (Kotlin, Jetpack Compose, Media3/ExoPlayer) that brings free, publicly available TV channels and radio stations from around the world into a single, modern interface.

---

## 📺 Live TV

- Browse thousands of live channels sourced from the [iptv-org](https://iptv-org.github.io/) database (the same data that powers TV Garden).
- **Filter by country**, with flag chips sorted by number of available channels.
- **Filter by genre**: news, sports, movies, music, kids, documentary, entertainment, and more.
- **Instant search** across all channel names.
- Channel logos are cached on-device for fast, smooth scrolling.
- Smart source ordering favors ~720p streams for smooth playback on mobile networks.

## 📻 Radio

- Tens of thousands of stations from the community-run [radio-browser.info](https://www.radio-browser.info/) directory.
- Filter by **country** or **genre** (pop, rock, news, talk, jazz, classical, dance, and more).
- Search stations by name.
- Animated equalizer visual in the player.
- Shows codec and bitrate for each station.

## 🎬 Player

| Feature | Description |
|---|---|
| **Zap mode** | Swipe up or down to change channels like a TV remote. |
| **Auto-failover** | If a stream fails, the next available source for that channel loads automatically. |
| **Manual source switch** | Tap *Next source* to try an alternate stream yourself. |
| **Picture-in-Picture** | Keep watching in a floating window while using other apps. Also triggers automatically when you leave the app during TV playback. |
| **Fullscreen rotation** | One-tap switch between portrait and landscape. |
| **Auto-hiding controls** | Immersive viewing with tap-to-reveal controls. |
| **Sleep timer** | Stop playback after 15, 30, 60, or 90 minutes. |
| **Background audio** | Playback continues with the screen off, with notification and lock-screen controls. |

## ✨ Discovery

- **Surprise me**: jumps to a random channel or station and queues more random picks, so you can keep zapping.
- **Auto-surf**: automatically hops to a new channel every 25 seconds, perfect for exploring the world hands-free.
- **Continue watching/listening**: a quick-access row of your recently played channels.

## ❤️ Library

- **Favorites**: tap the heart on any channel or station to pin it.
- **Recents**: your last 20 played items are remembered.
- Everything is stored locally on your device. No account or sign-in needed.

## 🎨 Design

- Dark, glass-style interface with violet, cyan, and pink gradients.
- Edge-to-edge layout with animated bottom navigation.
- Pulsing **LIVE** badges and a persistent mini-player.
- Responsive grid that adapts to phone and tablet screens.

## ⚡ Performance and Scalability

- **No backend to overload.** Catalogue data is served from static CDNs and cached on-device for 6 hours.
- **Offline fallback.** If the network is down, the app uses the last cached catalogue.
- **Radio load balancing.** Requests rotate across multiple community mirrors.
- **Lean buffering.** Fast channel start with low memory use.
- **Lazy rendering.** Large lists load only what's on screen.
- **Disk and memory image caching** for logos.

## 🔌 Streaming Support

- HLS (`.m3u8`) and DASH (`.mpd`) streams
- Progressive audio (MP3, AAC) for radio
- Automatic handling of streams that require custom Referer or User-Agent headers
- Cross-protocol redirects (HTTP and HTTPS)

---

## 🛠 Build the APK on GitHub

1. Create a new GitHub repository and upload the project contents (keep the `.github` folder at the repo root).
2. Open the **Actions** tab. The *Build APK* workflow runs on push, or click **Run workflow**.
3. Download the **OrbitLive-APKs** artifact and install `app-release.apk` or `app-debug.apk`.

## ⚠️ Notes

- "Free to air" here means free, publicly available internet streams. Satellite and antenna broadcasts require dedicated hardware.
- Some streams may be offline or geo-restricted, as is common with public IPTV lists.
- The release APK is signed with the debug key for easy installation and is not intended for Play Store upload.

## 📚 Data Sources

- TV: [iptv-org API](https://iptv-org.github.io/)
- Radio: [radio-browser.info](https://www.radio-browser.info/)
