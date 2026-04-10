# CaptureMaster Pro

A professional screen recording app for Android with video editing capabilities, built entirely with Jetpack Compose and Material Design 3.

## Features

- **Screen Recording** - Capture your screen in SD 480p, 720p HD, 1080p Full HD, or 1440p QHD resolution at 24, 30, or 60 FPS
- **Internal Audio Capture** - Record game and app sounds directly
- **Microphone Recording** - Add voice commentary over your recordings
- **Video Editor** - Trim and export recordings with a built-in editor powered by ExoPlayer
- **Gallery** - Browse, sort, filter, favorite, share, and manage all your recordings
- **Show Touches** - Display touch indicators while recording for tutorials
- **Countdown Timer** - Configurable countdown before recording starts
- **Dark Mode** - Full dark/light/system theme support with Material You dynamic colors
- **Premium System** - Google Play Billing integration with yearly subscription and lifetime purchase options

## Requirements

- **Android Version**: Android 7.0 (API 24) or higher
- **Target SDK**: Android 14 (API 34)
- **Build Tools**: JDK 17
- **Kotlin**: 1.9.x with Compose Compiler 1.5.8

## Build Instructions

1. Clone the repository
2. Open the project in Android Studio (Hedgehog or newer recommended)
3. Sync Gradle dependencies
4. Build and run:
   ```bash
   ./gradlew assembleDebug
   ```
5. Install on a connected device:
   ```bash
   ./gradlew installDebug
   ```

## Project Structure

```
app/src/main/java/com/factory/capturemasterpro/
├── CaptureMasterApp.kt          # Application class (database, billing init)
├── MainActivity.kt              # Entry point with theme and navigation setup
├── billing/
│   ├── BillingManager.kt        # Google Play Billing implementation
│   └── PremiumManager.kt        # Premium status and feature flags
├── data/
│   ├── db/
│   │   ├── AppDatabase.kt       # Room database
│   │   ├── Recording.kt         # Recording entity
│   │   └── RecordingDao.kt      # Data access object
│   └── repository/
│       └── RecordingRepository.kt
├── service/
│   └── ScreenRecordService.kt   # Foreground service for screen capture
├── ui/
│   ├── navigation/
│   │   └── NavGraph.kt          # Navigation routes and bottom bar
│   ├── screens/
│   │   ├── HomeScreen.kt        # Main recording screen
│   │   ├── GalleryScreen.kt     # Video gallery with sorting/filtering
│   │   ├── EditorScreen.kt      # Video editor with trim controls
│   │   ├── SettingsScreen.kt    # App settings and preferences
│   │   └── PaywallScreen.kt     # Premium upgrade screen
│   └── theme/
│       ├── Color.kt             # Color palette
│       ├── Theme.kt             # Material 3 theme configuration
│       └── Type.kt              # Typography styles
└── viewmodel/
    ├── HomeViewModel.kt         # Recording state management
    ├── GalleryViewModel.kt      # Gallery data and sorting
    ├── EditorViewModel.kt       # Editor state and trim logic
    └── SettingsViewModel.kt     # Preferences via DataStore
```

## Tech Stack

- **UI**: Jetpack Compose with Material Design 3
- **Navigation**: Jetpack Navigation Compose
- **Database**: Room with KSP
- **Preferences**: DataStore
- **Media Playback**: Media3 / ExoPlayer
- **Image Loading**: Coil
- **Permissions**: Accompanist Permissions
- **Billing**: Google Play Billing Library
- **Async**: Kotlin Coroutines + Flow
