# Gallery App

A modern, privacy-focused Android Gallery app built with Jetpack Compose and Material 3.

## Features

- **Privacy First** — Uses Android Photo Picker (no broad storage permissions)
- **URI Persistence** — Selected images remain accessible after restart
- **Adaptive Grid** — Responsive thumbnail grid like a modern gallery app
- **Image Viewer** — Full-screen with pinch-to-zoom, double-tap zoom, pan, swipe navigation
- **Sorting** — By Date Added, Date Taken, or Filename
- **Search** — Search images by filename
- **Pull to Refresh** — Validates and cleans up revoked URI permissions
- **Image Info** — Filename, resolution, date taken, file size
- **Share & Remove** — Share images or remove from gallery (never deletes original files)
- **Light & Dark Mode** — Full Material 3 theming with dynamic color on Android 12+

## Architecture

- **MVVM** with ViewModel + StateFlow
- **Hilt** for Dependency Injection
- **Room** for storing URI references
- **Repository Pattern** for data access
- **Navigation Compose** for screen navigation
- **Kotlin Coroutines** for async operations

## Tech Stack

| Library | Purpose |
|---------|---------|
| Jetpack Compose | UI framework |
| Material 3 | Design system |
| Hilt | Dependency injection |
| Room | Local database |
| Coil | Image loading & caching |
| Navigation Compose | Screen navigation |
| Android Photo Picker | Privacy-safe image selection |

## Requirements

- Android Studio Hedgehog or newer
- Android SDK 35
- Kotlin 2.1.0
- Java 11+

## Build Instructions

### From Command Line

```bash
# Debug APK
./gradlew assembleDebug

# The APK will be at:
# app/build/outputs/apk/debug/app-debug.apk
```

### From Android Studio

1. Open this folder in Android Studio
2. Wait for Gradle sync to complete
3. Click **Build > Build Bundle(s) / APK(s) > Build APK(s)**
4. Find the APK in `app/build/outputs/apk/debug/`

## Installing the APK

### Via ADB
```bash
adb install app/build/outputs/apk/debug/app-debug.apk
```

### Direct Install
Transfer the APK to your device and open it. You may need to allow "Install from Unknown Sources" in Settings > Security.

## Package Name

`com.gallery.app`

## Minimum SDK

Android 8.0 (API 26)

## Target SDK

Android 15 (API 35)

## Privacy

- **No broad storage access** — Uses Android Photo Picker
- **Persistable URI permissions** — Only accesses images you explicitly select
- **No file copying** — Only URI references are stored
- **No network access** — Fully offline
- **Graceful permission revocation** — Automatically removes inaccessible images on refresh
