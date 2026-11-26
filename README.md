# Orbit Live

**Every live TV channel and radio station, in one app.**

Orbit Live is a native Android application built with Kotlin, Jetpack Compose, Material 3, and Media3/ExoPlayer. It brings free, publicly available live TV channels and radio stations from around the world into a dark, glass-styled interface.

## Features

- **Live TV (`iptv-org`):** Browse thousands of live TV channels filtered by country (flag chips sorted by channel count), genre, or instant search, with 6-hour HTTP caching and offline stale-cache fallback.
- **Radio (`radio-browser.info`):** Explore stations worldwide filtered by country, genre, or search query, with client-side mirror load balancing, codec/bitrate metadata, and an animated equalizer.
- **Player & Zap Mode:** Media3/ExoPlayer playback with vertical swipe-to-zap channel switching, automatic stream failover, manual *Next source* switching, Picture-in-Picture (including automatic PiP on leaving the app during TV playback), fullscreen orientation toggle, auto-hiding controls, and a sleep timer (15, 30, 60, or 90 minutes).
- **Background Playback:** `MediaSessionService` (`PlaybackService`) keeps radio and TV audio playing in the background with notification and lock-screen media controls.
- **Discovery & Library:** *Surprise me*, *Auto-surf* (25-second channel hopping), *Continue watching / listening*, on-device Favorites, and last-20 Recents persisted via `SharedPreferences`.

## Project Structure

- `app/src/main/java/com/orbit/live/Data.kt` — `Repo` (`iptv-org` + `radio-browser.info` API clients & cache), `Station`/`Country` models, `Prefs`.
- `app/src/main/java/com/orbit/live/AppVM.kt` — ViewModel managing catalogues, search/filter state, playback queue, failover, auto-surf, sleep timer, and library.
- `app/src/main/java/com/orbit/live/PlayerHolder.kt` & `PlaybackService.kt` — Shared ExoPlayer instance with custom per-host `Referer`/`User-Agent` header resolution and `MediaSessionService`.
- `app/src/main/java/com/orbit/live/Screens.kt` & `PlayerScreen.kt` — Jetpack Compose screens (`TvScreen`, `RadioScreen`, `LibraryScreen`, `MiniPlayer`, `BottomBar`, `PlayerScreen`).
- `app/src/main/java/com/orbit/live/MainActivity.kt`, `OrbitApp.kt`, `Theme.kt` — App initialization, Coil disk/memory image caching, edge-to-edge UI, notification permission, and PiP support.

## Data Sources

- TV: [iptv-org API](https://iptv-org.github.io/)
- Radio: [radio-browser.info](https://www.radio-browser.info/)
