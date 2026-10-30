# Sachin Clicker – Native Android Driver Ride Matching Assistant

Sachin Clicker is a native Android application built with Kotlin, Jetpack Compose, Material 3, and Clean Architecture. It automates ride distance evaluation according to driver-specified Pickup and Drop thresholds, with optional fare filtering, a floating status bubble, and safety fail-safes.

---

## 1. Key Features

- **No Logins or Subscriptions**: Works 100% locally on the device without registration, mobile OTP, Gmail login, or paid locks.
- **Local Settings with Jetpack DataStore**:
  - Maximum Pickup Distance (KM): default `3.0 KM`
  - Maximum Drop Distance (KM): default `5.0 KM`
  - Decimal fields with quick preset chips (`0.5, 1, 2, 3, 4, 5, 10, 12, 20 KM`)
- **Strict Matching Rule Engine**:
  ```text
  actualPickupKm <= maxPickupKm AND actualDropKm <= maxDropKm
  ```
  Both conditions must strictly evaluate to `true`.
- **Optional Price / Bonus Filter**:
  - If disabled, distance matching alone determines eligibility.
  - If enabled, price condition (`actualPrice >= minPrice`) is strictly verified.
  - Never invents price information when not visible on screen.
- **Accessibility Service (`SachinAccessibilityService`)**:
  - Observes visible UI changes **only** while automation is actively turned ON.
  - Parses structured labels (`Pickup`, `Pick up`, `From`, `Drop`, `Drop off`, `To`) rather than guessing numbers.
  - Identifies genuine Accept button nodes with debouncing, rate-limiting, and duplicate prevention.
  - Never clicks random screen coordinates.
- **Floating Controls Bubble (`FloatingOverlayService`)**:
  - Draggable overlay pill with real-time status indicator (Active / Idle).
  - Quick Start / Stop toggle and high-priority Emergency Stop.
  - Non-blocking layout (`FLAG_NOT_FOCUSABLE`).
- **Foreground Safety Service (`AutomationForegroundService`)**:
  - Prevents background system kills.
  - Provides instant "Stop Automation" and "Emergency Stop" actions in the notification shade.
- **Built-in Interactive Ride Simulator**:
  - Enables immediate on-device testing and verification of the parser, regex rules, and matching engine without requiring third-party apps.
- **Local Activity Audit Log**:
  - Fully local SQLite/Room database recording all evaluations, matches, rejections, and actions.

---

## 2. Project Architecture

```
com.example/
├── SachinApplication.kt          # Global Application instance & repository initializers
├── MainActivity.kt               # Single-activity Compose UI & tab navigation
│
├── data/
│   ├── model/
│   │   ├── AutomationSettings.kt # Settings schema
│   │   ├── RideDetails.kt        # Parsed ride structure & MatchResult
│   │   └── ActivityLogEntry.kt   # Room database entity
│   ├── preferences/
│   │   └── SettingsRepository.kt # Jetpack DataStore Preferences
│   └── log/
│       ├── LogDao.kt             # Room Data Access Object
│       ├── AppDatabase.kt        # Room Database
│       └── ActivityLogRepository.kt
│
├── domain/
│   ├── AutomationManager.kt      # Singleton coordinator & safety state machine
│   ├── RideMatchingEngine.kt     # Pure matching rule evaluation logic
│   └── RideParser.kt             # Context-aware text & accessibility node parser
│
├── service/
│   ├── SachinAccessibilityService.kt # Accessibility event listener & node clicker
│   ├── AutomationForegroundService.kt # Foreground notification with quick stop actions
│   └── FloatingOverlayService.kt     # WindowManager draggable floating pill
│
└── ui/
    ├── MainViewModel.kt          # StateFlow view model
    ├── components/
    │   ├── CommonUi.kt           # Badges, chips, emergency button
    │   └── PermissionUtils.kt    # Permission checkers & settings intent launchers
    ├── screens/
    │   ├── DashboardScreen.kt    # Main dashboard with live controls
    │   ├── DistanceSettingsScreen.kt # Distance inputs & price filter settings
    │   ├── SimulationScreen.kt   # Interactive test playground
    │   └── ActivityLogScreen.kt  # Filterable audit log
    └── theme/
        ├── Color.kt
        ├── Theme.kt
        └── Type.kt
```

---

## 3. Build & Run Instructions

### Prerequisites
- Android Studio Ladybug or newer
- JDK 17 or JDK 21
- Android SDK 36 (Minimum SDK 26)

### Building via Gradle
```bash
# Build debug APK
gradle assembleDebug

# Run unit tests
gradle :app:testDebugUnitTest
```

The compiled APK will be generated at:
`app/build/outputs/apk/debug/app-debug.apk`

---

## 4. Permissions Setup on Device

1. **Accessibility Service**:
   - Open App > Dashboard > Tap **"Enable in Android Settings"**.
   - Navigate to **Installed Apps / Downloaded Apps** > **Sachin Clicker Automation Service** > Toggle **ON**.
2. **Floating Overlay (Display over other apps)**:
   - Dashboard > Tap **"Grant Overlay Permission"** > Allow display over other apps.
3. **Notifications**:
   - Granted automatically or via prompt on Android 13+.

---

## 5. Third-Party App Compatibility & Safety Notice

> **Important Real-Device Testing Note**:
> Accessibility node structures vary across different ride-hailing applications (such as Uber, Ola, Rapido, inDrive) and may update frequently.
> 
> - Sachin Clicker does **not** bypass CAPTCHA, biometric verification, or anti-bot protections.
> - Always test your ride app's UI layout using the built-in **Rule Simulator** before driving.
> - Ensure compliance with your platform's Terms of Service and local driving safety regulations.
