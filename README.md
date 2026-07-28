# Secure Android Gallery Application

A modern, highly-secure, and privacy-focused Android Gallery application. Built from the ground up with a **security-first** mindset to demonstrate robust Android AppSec principles, minimal permission surfaces, and defense-in-depth architecture.

## 🛡️ Security & Privacy Architecture

As an offensive security practitioner, I understand that the most secure permission is the one you never request. This application is architected around the principle of **Least Privilege**:

- **Zero-Trust Storage Model** — Completely eliminates the need for dangerous `READ_EXTERNAL_STORAGE` or `MANAGE_EXTERNAL_STORAGE` permissions. It leverages the Android Photo Picker to ensure the app only has access to explicitly user-selected media.
- **Strict Data Scoping (URI Persistence)** — Instead of duplicating sensitive files into the app's sandboxed storage (which creates redundant attack surfaces), it persists temporary URI permissions using `ContentResolver`.
- **Anti-Exfiltration Design** — Operates entirely offline. The app intentionally lacks the `INTERNET` permission, guaranteeing zero network-based data exfiltration of sensitive user media.
- **Self-Healing State** — Implements a robust validation mechanism (Pull to Refresh) that automatically detects and purges revoked or stale URI permissions, ensuring the app state strictly mirrors current OS-level access grants.

## 🎯 Threat Model & Mitigations

| Threat | Mitigation Strategy |
|--------|---------------------|
| **Data Exfiltration** | No `INTERNET` permission requested. Fully air-gapped architecture. |
| **Broad Storage Abuse** | Scoped access via Photo Picker. No broad storage permissions requested. |
| **Local SQL Injection** | Utilizing Room ORM to ensure all database queries are tightly parameterized. |
| **Unauthorized File Deletion** | The app operates on URI references and never deletes original user files from the OS. |

## 🏗️ Technical Stack & Secure Coding

- **Architecture:** MVVM with ViewModel + StateFlow for strict, immutable state management.
- **Dependency Injection:** Hilt (Dagger) to enforce modularity and prevent unauthorized object instantiation or dependency spoofing.
- **UI Framework:** Jetpack Compose (Material 3), reducing the attack surface of classic Android XML vulnerabilities (e.g., specific intent-redirection or tapjacking flaws related to legacy views).
- **Data Persistence:** Room for secure, local database management of URI references.

## 📱 Core Features

While security is the priority, the user experience is uncompromised:
- **Adaptive Grid** — Responsive thumbnail grid mimicking modern native gallery apps.
- **Advanced Image Viewer** — Full-screen with pinch-to-zoom, pan, and swipe navigation.
- **Smart Sorting & Search** — Fast local search and sorting by Date or Filename.
- **Granular Control** — Share images securely or remove them from the app's scope (without affecting the underlying OS files).
- **Material 3 Design** — Full Light & Dark mode support with dynamic colors on Android 12+.

## 🚀 Build & Deployment Instructions

### Prerequisites
- Android Studio Hedgehog or newer
- Android SDK 35
- Kotlin 2.1.0 / Java 11+

### Compilation (Command Line)
```bash
# Build a debug APK for static/dynamic analysis
./gradlew assembleDebug

# Output location: app/build/outputs/apk/debug/app-debug.apk
```

### Deployment (Via ADB)
```bash
# Push and install the application to a connected test device/emulator
adb install app/build/outputs/apk/debug/app-debug.apk
```

## 📊 Technical Details

- **Package Name:** `com.gallery.app`
- **Minimum SDK:** Android 8.0 (API 26)
- **Target SDK:** Android 15 (API 35)
