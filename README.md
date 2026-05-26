# OpsecOpsec

**OpsecOpsec** is a zero-trace, air-gapped media vault designed for Red Teamers and offensive security operators to securely store sensitive engagement screenshots and media on physical devices without the risk of OS-level exposure or rogue app exfiltration.

## 🛡️ Operational Security (OPSEC) Architecture

During physical engagements, operators often need to capture and store sensitive data (e.g., physical access photos, hardware configuration screenshots). Storing these in a standard opsec exposes them to broad `READ_EXTERNAL_STORAGE` permissions that could be abused by malware, third-party apps, or telemetry services. 

OpsecOpsec mitigates this through **Strict OPSEC Principles**:

- **Zero-Trace OS Footprint** — By leveraging the Android Photo Picker and temporary `ContentResolver` URI permissions, the app creates a sandboxed view of selected media without leaving redundant file copies in standard accessible directories.
- **Air-Gapped by Design** — The application explicitly drops the `INTERNET` permission. It is mathematically impossible for the app to act as an exfiltration vector over the network, ensuring sensitive engagement data stays on the device.
- **Permissionless Operation** — Completely eliminates the need for dangerous `READ_EXTERNAL_STORAGE` or `MANAGE_EXTERNAL_STORAGE` permissions, shrinking the attack surface to zero for broad storage access.
- **Self-Destructing State** — Uses a "Pull to Refresh" validation mechanism that automatically detects and purges revoked or stale URI permissions, ensuring the app's access state strictly mirrors OS-level grants and leaves no dangling pointers to sensitive files.

## 🎯 Threat Model & Mitigations

| Threat | OPSEC Mitigation Strategy |
|--------|---------------------------|
| **Rogue App Exfiltration** | No `INTERNET` permission requested. Fully air-gapped. |
| **Broad Storage Scraping** | Scoped access only. No broad storage permissions requested. |
| **Local Device Exploitation (SQLi)** | Utilizes Room ORM to ensure all local state queries are tightly parameterized. |
| **Forensic Artifact Duplication** | Operates solely on URI references. Never duplicates files into its sandbox. |

## 🏗️ Technical Stack & Secure Coding

- **Architecture:** MVVM with ViewModel + StateFlow for strict, immutable state management.
- **Dependency Injection:** Hilt (Dagger) to enforce modularity and prevent unauthorized object instantiation or dependency spoofing.
- **UI Framework:** Jetpack Compose (Material 3), mitigating legacy Android XML vulnerabilities (e.g., intent-redirection or tapjacking via overlapping views).
- **Data Persistence:** Room for secure, local SQLite management of URI references.

## 📱 Core Features

Designed for operators who need speed and reliability in the field:
- **Adaptive Grid** — Responsive thumbnail grid mimicking native galleries for incognito usage.
- **Advanced Image Viewer** — Full-screen inspection with pinch-to-zoom and pan for analyzing physical layout photos.
- **Smart Sorting & Search** — Fast local search and sorting by Date or Filename.
- **Granular Control** — Share images securely or remove them from the vault's scope without altering the underlying OS files.
- **Material 3 Design** — Full Light & Dark mode support for low-light physical engagements.

## 🚀 Build & Deployment Instructions

### Prerequisites
- Android Studio Hedgehog or newer
- Android SDK 35
- Kotlin 2.1.0 / Java 11+

### Compilation (Command Line)
```bash
# Build a debug APK for static/dynamic analysis or field testing
./gradlew assembleDebug

# Output location: app/build/outputs/apk/debug/app-debug.apk
```

### Deployment (Via ADB)
```bash
# Push and install the application to an operator's Android device
adb install app/build/outputs/apk/debug/app-debug.apk
```

## 📊 Technical Details

- **Package Name:** `com.opsec.space`
- **Minimum SDK:** Android 8.0 (API 26)
- **Target SDK:** Android 15 (API 35)
