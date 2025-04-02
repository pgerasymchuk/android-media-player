# 🎵 Android Media Player

A Kotlin media player for Android that keeps your own playlists of local audio/video files and online streams, plays them with **Media3 ExoPlayer** and keeps playing in the background through a **MediaSessionService**, with controls in the notification and on the lock screen. A separate screen plays internet radio, including **HLS** streams.

🎓 Built for the Android Development course at Igor Sikorsky Kyiv Polytechnic Institute (3rd year, spring 2025).

## ✨ Features

- **Playlists stored in Room**: create, rename and delete playlists; tap a playlist name to expand or collapse its tracks (the state is saved too).
- **Two kinds of sources**: pick audio or video files with the system document picker (the app keeps a persistable read grant, so files still open after a restart) or paste any media URL.
- **Playback of the whole list**: pressing *Play* on a track queues the entire playlist and starts from that track; *Shuffle* reorders the playlist and starts it from the top.
- **Video and audio in one view**: a Media3 `PlayerView` with the standard controls sits on top of the main screen.
- **Internet radio**: enter a stream URL (HLS `.m3u8`, MP3/AAC streams and anything else ExoPlayer can open) and play it; playback errors are shown as a toast.
- **Background playback**: the player lives in a service, so music continues when the app is closed and can be controlled from the media notification, lock screen or headset. Audio focus is handled and playback pauses when headphones are unplugged.

## 🧩 How it works

```
 MainActivity ──┐                      ┌────────────────────────────┐
                ├── MediaController ──▶│ MediaPlaybackService       │
 RadioActivity ─┘   (SessionToken)     │  MediaSession + ExoPlayer  │──▶ notification / lock screen
        │                              └────────────────────────────┘
        ▼
  PlaylistAdapter ─▶ MediaAdapter      AppDatabase (Room)
                                        ├── playlists
                                        └── media_files (FK → playlists, ON DELETE CASCADE)
```

**Playback.** `MediaPlaybackService` extends `MediaSessionService` and owns the only `ExoPlayer` in the app (with audio attributes for music, audio focus and "becoming noisy" handling). Both activities connect to it in `onStart` with `MediaController.Builder(...).buildAsync()`, attach the controller to their `PlayerView` and release it in `onStop`. Items sent through a controller can arrive without their local configuration, so every item also carries its URI in `RequestMetadata`, and the session callback `onAddMediaItems` restores it before the items reach the player. Tracks also get a title in `MediaMetadata`, which is what the notification shows.

**Data.** Room keeps two tables:

| Table | Columns |
|-------|---------|
| `playlists` | `id`, `name`, `is_visible` |
| `media_files` | `id`, `playlist_id` (indexed FK), `name`, `path`, `is_url` |

`PlaylistDao` exposes suspend queries for loading, inserting, renaming and deleting; removing a playlist cascades to its tracks. Small mapper functions (`Mappers.kt`) convert entities to the UI models `Playlist` / `MediaFile` and back, and derive a readable name from a URL (last path segment without query or fragment).

**UI.** The main screen is a `RecyclerView` of playlists, each with a nested `RecyclerView` of tracks. Click handlers resolve the item through `bindingAdapterPosition`, so they stay correct after items are added or removed. The edit dialog (`dialog_edit_playlist.xml`) renames a playlist and adds URLs or files to it. Database work runs in `lifecycleScope` coroutines.

## 🧪 Tests

JVM unit tests in `app/src/test` cover the entity/model mapping and URL name parsing:

```bash
./gradlew testDebugUnitTest
```

## 🖥️ Build and run

Requirements: Android Studio (or JDK 17+ with the Android SDK, platform 35), a device or emulator with Android 7.0 (API 24) or newer.

```bash
git clone https://github.com/pgerasymchuk/android-media-player.git
cd android-media-player
./gradlew installDebug        # build and install on a connected device
```

To try the radio screen, any public HLS test stream works, for example an `.m3u8` URL.

## 🗂 Project structure

```
app/src/main/java/com/pgerasymchuk/mediaplayer/
├── activity/      MainActivity (playlists + player), RadioActivity (streams)
├── adapter/       PlaylistAdapter and nested MediaAdapter
├── data/          Room database, DAO, entities, UI models, mappers
└── service/       MediaPlaybackService (MediaSessionService + ExoPlayer)
app/src/main/res/  layouts for screens, list items and the edit dialog
app/src/test/      unit tests
```

## 🛠️ Tech stack

- Kotlin, Android SDK 35 (min SDK 24), View Binding
- AndroidX Media3 1.5: ExoPlayer, HLS, MediaSession / MediaSessionService, PlayerView
- Room 2.6 (kapt) with Kotlin coroutines
- RecyclerView, ConstraintLayout, AppCompat dialogs
- Storage Access Framework for picking local media
- Gradle Kotlin DSL with a version catalog, JUnit 4
