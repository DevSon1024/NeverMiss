# NeverMiss - Android Streak and Habit Tracker

<p align="center">
  <img src="Screenshot/NeverMiss Logo.png" alt="NeverMiss Logo" width="160"/>
</p>

<p align="center">
  <img src="https://img.shields.io/badge/API-26%2B-brightgreen.svg?style=for-the-badge&logo=android" alt="API 26+"/>
  <img src="https://img.shields.io/badge/Kotlin-1.9.22-7F52FF.svg?style=for-the-badge&logo=kotlin" alt="Kotlin"/>
  <img src="https://img.shields.io/badge/UI-Jetpack%20Compose-4285F4.svg?style=for-the-badge&logo=jetpackcompose" alt="Jetpack Compose"/>
  <img src="https://img.shields.io/badge/Material--UI-Material%203-6750A4.svg?style=for-the-badge&logo=materialdesign" alt="Material 3"/>
  <img src="https://img.shields.io/badge/DI-Hilt-0052CC.svg?style=for-the-badge&logo=google" alt="Hilt"/>
  <img src="https://img.shields.io/badge/License-MIT-yellow.svg?style=for-the-badge" alt="MIT License"/>
</p>

**NeverMiss** is a feature-packed, offline-first Android habit and streak tracking application built with Jetpack Compose, Material Design 3, Room, Hilt, WorkManager, and AlarmManager. It helps users build lasting consistency through automatic app usage tracking, interval timers, smart reminders, and rich progress analytics.

> [!WARNING]
> This application is actively under development. Known stability issues exist around exact alarms, background workers, and state synchronization across OS power-saving modes. See [Known Issues](#known-issues) before deploying to production.

---

## Table of Contents

- [What's New](#whats-new)
- [Features](#features)
- [Architecture and Tech Stack](#architecture-and-tech-stack)
- [Project Structure](#project-structure)
- [Getting Started](#getting-started)
- [Known Issues](#known-issues)
- [Contributing](#contributing)
- [License](#license)

## Features

### Automatic App Streak Tracking

- Detects daily interaction with installed applications (Duolingo, GitHub, Reddit, and others) using `UsageStatsManager`.
- Background usage event normalization across Android API 21 through 36.
- 2-second debounce window preventing duplicate completions on task switching.

### Multi-Account Sub-Streaks

- Independent named sub-streaks (Personal, Developer, Anonymous) under a single tracked application.
- Explicit attribution flow: NeverMiss prompts for confirmation after an app session and only credits the selected account.

### App Streak Dashboard and Analytics

- Today completion badges and active streak counters.
- Per-app and per-account monthly completion calendar heatmaps.
- Detailed sub-streak management: add, rename, enable, disable, delete.

### Interval Timer and Countdown System

- Countdown timers for single-use timed tasks.
- Repeating interval reminders with finite or unlimited repetition counts.
- Full lifecycle control: start, pause, resume, stop, restart.
- Persistent timer state across app restarts and device reboots.
- Exact and inexact AlarmManager scheduling with automatic fallback.
- Notifications with actionable buttons (Done, Snooze, Stop, Dismiss).
- Sustained audio and vibration alerts via foreground service.
- Configurable alert sound, vibration pattern, alert duration, and snooze duration through a dedicated settings screen.
- Automatic alarm recovery after reboot, time change, or permission change.

### Background Reliability and Diagnostics

- 15-minute periodic reconciliation via `WorkManager` (`AppUsageReconciliationWorker`) with exponential backoff.
- Fast-path foreground reconciliation on `MainActivity.onResume` for instant credit.
- Device reboot restoration via `BootReceiver`.
- Built-in Tracking Diagnostics screen to audit permissions, WorkManager state, and OEM battery optimization guidance.

### Flexible Habit Scheduling

- Daily habits, custom interval streaks (every 3 days), or specific weekday routines.
- Exact alarm scheduling via `AlarmManager`.
- Persistent and repeating reminder options until task completion.
- Danger Reminder alerts before habit cutoff time.
- Grace period support (up to 3:00 AM post-midnight completion).

### Material 3 UI

- Custom Canvas particle confetti engine for celebrating streak milestones.
- Glassmorphic card overlays, dynamic color accent picker, and haptic feedback.
- Category-based custom icons and emoji selectors.
- Live countdown ticker and status badge system in timer cards.

### Analytics and History

- Visual completion history and calendar heatmaps.
- Best streak vs. current streak metrics and progress tracking.

### Home Screen Widget

- `StreakWidgetProvider` for quick at-a-glance habit status.

### Privacy and Offline Engine

- 100% on-device local storage powered by Room Database (v5). Zero network telemetry, zero cloud tracking.
- Narrow `<queries>` visibility to avoid broad permission risks.

---

## Architecture and Tech Stack

NeverMiss follows Clean Architecture principles combined with modern Android development practices.

```
            UI Layer
        (Compose / Material 3)
                |
        ViewModel Layer
        (StateFlow / Hilt)
                |
        Domain Layer
      (Use Cases / Models)
                |
         Data Layer
     (Room / DataStore)
```

| Domain | Technology |
| :--- | :--- |
| Language | Kotlin 1.9+ |
| UI Framework | Jetpack Compose with Material Design 3 |
| Dependency Injection | Dagger Hilt with hilt-navigation-compose |
| Local Database | Room Database v5 with KSP |
| Preferences | Jetpack DataStore (Preferences) |
| Usage Tracking | Android UsageStatsManager and UsageEvents |
| Alarm Scheduling | AlarmManager with exact and inexact fallback |
| Background Processing | WorkManager and AlarmManager |
| Foreground Services | MediaPlayback foreground service for alert audio |
| Image Loading | Coil Compose |
| Asynchronous Engine | Kotlin Coroutines and Flow |

---

## Project Structure

```text
com.devson.NeverMiss/
+-- App.kt                               Application entry point (Hilt and WorkManager init)
+-- MainActivity.kt                      Single-activity Compose host (with onResume catch-up)
+-- core/
|   +-- apptracking/                     Usage tracking engine, normalizer, permissions, worker
|   |   +-- engine/                      AppUsageTrackingEngine, UsageEventNormalizer
|   |   +-- notification/               AccountConfirmationNotifier
|   |   +-- permission/                 UsageAccessPermissionHelper
|   |   +-- worker/                     AppUsageReconciliationWorker, AppTrackingScheduler
|   +-- di/                             Hilt modules (DatabaseModule, RepositoryModule)
|   +-- reminder/
|   |   +-- interval/                   IntervalTimerAlarmScheduler, Receiver, Processor
|   |   |                               RecoveryCoordinator, NotificationHelper
|   |   |                               AlertPlayback, AlertService, AlertManager
|   |   |                               PermissionManager, SettingsRepository
|   |   +-- AlarmReceiver.kt
|   |   +-- BootReceiver.kt             Handles boot, time change, and interval timer recovery
|   |   +-- NotificationHelper.kt
|   |   +-- DailyStreakWorker.kt
|   +-- util/                           AppClock, TimeProvider, date format helpers
|   +-- widget/                         StreakWidgetProvider
+-- data/
|   +-- local/
|   |   +-- apps/                       InstalledAppScanner, InstalledAppInfo
|   |   +-- db/                         Room Database v5, Entities, DAOs, Migrations
|   |   |                               IntervalTimerEntity, IntervalTimerDao
|   |   +-- preferences/               DataStore UserPreferences
|   +-- repository/                     StreakRepositoryImpl, AppStreakRepositoryImpl
|                                       IntervalTimerRepositoryImpl
+-- domain/
|   +-- calculator/                     AppStreakCalculator
|   +-- model/                          Streak, IntervalTimer, IntervalTimerStateMachine
|   |                                   TrackedApp, TrackedAccount, PendingConfirmation
|   +-- repository/                     StreakRepository, AppStreakRepository
|   |                                   IntervalTimerRepository
|   +-- usecase/
|       +-- intervaltimer/              Full suite of 12 interval timer use cases
|       +-- appstreaks/                 App tracking and account use cases
+-- navigation/                         NeverMissNavGraph, Screen routes
+-- ui/
|   +-- screens/
|   |   +-- intervaltimer/             IntervalTimerScreen, IntervalTimerCard
|   |   |                              CreateEditIntervalTimerDialog
|   |   |                              IntervalTimerSettingsScreen
|   |   +-- appstreaks/                Dashboard, Detail, SubStreak, Picker, Diagnostics
|   |   +-- HomeScreen.kt
|   |   +-- AnalyticsScreen.kt
|   |   +-- SettingsScreen.kt
|   +-- theme/                         Color, Type, Material 3 Theme
|   +-- util/                          CategoryIconHelper
+-- viewmodel/
    +-- intervaltimer/                 IntervalTimerViewModel
    +-- HomeViewModel.kt
    +-- (Habit and App Streak ViewModels)
```

---

## Getting Started

### Prerequisites

- Android Studio Ladybug (2024.2.1) or newer.
- JDK 17.
- Android SDK with `compileSdk 36` and `minSdk 26`.

### Building from Source

1. Clone the repository.

   ```bash
   git clone https://github.com/DevSon1024/NeverMiss.git
   cd NeverMiss
   ```

2. Build the debug APK.

   ```bash
   ./gradlew assembleDebug
   ```

3. Run unit tests.

   ```bash
   ./gradlew testDebugUnitTest
   ```

4. Install on a connected device or emulator.

   ```bash
   ./gradlew installDebug
   ```

---

## Known Issues

> [!CAUTION]
> 1. **Background Worker and Battery Optimization (Doze Mode)**
>    OEM customizations (Xiaomi MIUI, Samsung OneUI, OnePlus OxygenOS) aggressively kill `DailyStreakWorker` and `AlarmReceiver`. Background resets for missed streaks may fail at midnight unless manual battery optimization exclusions are granted.
>
> 2. **Exact Alarm Permission on Android 12 and Later**
>    If the `SCHEDULE_EXACT_ALARM` permission is denied, timers fall back to inexact scheduling. Reminder delivery may be delayed by several minutes depending on system load and Doze mode.
>
> 3. **OEM Aggressive App Killers**
>    Interval timer alarms and foreground alert services may be terminated by aggressive battery savers on Xiaomi HyperOS, Huawei EMUI, and similar OEM overlays unless the app is added to the system battery whitelist.
>
> 4. **Canvas Confetti Frame Drops**
>    High-density particle spawns in `HomeScreen` during streak completion can trigger frame drops on devices with lower GPU clock speeds.
>
> 5. **Home Screen Widget Out of Sync**
>    `StreakWidgetProvider` does not always reflect Room database updates immediately unless the app is brought to the foreground.
>
> 6. **Grace Period Edge Cases**
>    Calculation logic in `StreakCalculator` around month-end or daylight saving time boundaries can miscalculate streak continuity.