# 🔥 NeverMiss - Modern Android Streak & Habit Tracker

![Android SDK](https://img.shields.io/badge/API-26%2B-brightgreen.svg?style=for-the-badge&logo=android)
![Kotlin](https://img.shields.io/badge/Kotlin-1.9.22-7F52FF.svg?style=for-the-badge&logo=kotlin)
![Jetpack Compose](https://img.shields.io/badge/UI-Jetpack%20Compose-4285F4.svg?style=for-the-badge&logo=jetpackcompose)
![Material 3](https://img.shields.io/badge/Material--UI-Material%203-6750A4.svg?style=for-the-badge&logo=materialdesign)
![Hilt](https://img.shields.io/badge/DI-Hilt-0052CC.svg?style=for-the-badge&logo=google)
![License](https://img.shields.io/badge/License-MIT-yellow.svg?style=for-the-badge)

**NeverMiss** is a feature-packed, offline-first Android habit and streak tracking application built with **Jetpack Compose**, **Material Design 3**, **Room**, **Hilt**, and **WorkManager**. Designed to help users build consistency, NeverMiss provides dynamic reminders, category customization, interactive confetti celebrations, and rich analytics.

> [!WARNING]
> **Experimental / Work-in-Progress Warning**  
> This application currently features cutting-edge Material 3 UI implementations and complex background scheduling mechanisms. **It is actively under development and exhibits known stability issues** (especially with exact alarms, background workers, and state synchronization across OS power-saving modes). See [Known Instabilities & Issues](#-known-instabilities--issues) before using in production.

---

## 📸 Overview & Features

### 🌟 Key Features
- **Flexible Habit Scheduling**: Create daily habits, custom interval streaks (e.g., every 3 days), or specific weekday routines (Mon, Wed, Fri).
- **Smart Alarm & Notification System**:
  - Exact Alarm scheduling (`AlarmManager` & `AlarmReceiver`).
  - Persistent & repeating reminder options until task completion.
  - "Danger Reminder" alerts before habit cutoff time.
  - Grace period support (up to 3:00 AM post-midnight completion).
- **Material 3 Aesthetics & Micro-Animations**:
  - Custom Canvas particle confetti engine for celebrating streak milestones.
  - Glassmorphic card overlays, dynamic color accent picker, and haptic feedback.
  - Category-based custom icons and emoji selectors.
- **Analytics & History**:
  - Visual completion history and calendar heatmaps.
  - Best streak vs. current streak metrics and progress tracking.
- **Home Screen Widget Integration**:
  - Android AppWidget provider (`StreakWidgetProvider`) for quick at-a-glance status updates.
- **Privacy First & Offline Engine**:
  - Local-first data architecture powered by SQLite & Room Database. No cloud account required.

---

## 🛠️ Architecture & Tech Stack

NeverMiss follows **Clean Architecture** principles combined with modern Android development practices:

```
                  ┌────────────────────────┐
                  │       UI Layer         │
                  │ (Compose / Material 3) │
                  └───────────┬────────────┘
                              │
                  ┌───────────▼────────────┐
                  │    ViewModel Layer     │
                  │   (StateFlow / Hilt)   │
                  └───────────┬────────────┘
                              │
                  ┌───────────▼────────────┐
                  │      Domain Layer      │
                  │ (Use Cases / Models)   │
                  └───────────┬────────────┘
                              │
                  ┌───────────▼────────────┐
                  │       Data Layer       │
                  │ (Room / Preferences)   │
                  └────────────────────────┘
```

| Domain | Technology / Library |
| :--- | :--- |
| **Language** | [Kotlin 1.9+](https://kotlinlang.org/) |
| **UI Framework** | [Jetpack Compose](https://developer.android.com/jetpack/compose) (Material Design 3) |
| **Dependency Injection** | [Dagger Hilt](https://dagger.dev/hilt/) + `hilt-navigation-compose` |
| **Local Database** | [Room Database](https://developer.android.com/training/data-storage/room) + KSP |
| **Preferences** | [Jetpack DataStore](https://developer.android.com/topic/libraries/architecture/datastore) (Preferences) |
| **Background Processing** | [WorkManager](https://developer.android.com/topic/libraries/architecture/workmanager) + `AlarmManager` |
| **Image Loading** | [Coil Compose](https://coil-kt.github.io/coil/compose/) |
| **Asynchronous Engine** | Kotlin Coroutines & `Flow` |

---

## ⚠️ Known Instabilities & Issues

The project is undergoing active refactoring. Below are current known operational bottlenecks:

> [!CAUTION]
> 1. **Background Worker & Battery Optimization (Doze Mode)**
>    - OEM OS customizations (Xiaomi MIUI, Samsung OneUI, OnePlus OxygenOS) aggressively kill `DailyStreakWorker` and `AlarmReceiver`.
>    - Background resets for missed streaks may fail to execute at midnight unless manual battery optimization exclusions are granted.
>
> 2. **Exact Alarm Permission Exceptions (Android 13+ / API 33+)**
>    - On Android 13/14, if `SCHEDULE_EXACT_ALARM` permission is not explicitly granted by the user in system settings, reminder setup may crash or silently fail.
>
> 3. **UI Re-composition Stuttering on Canvas Confetti**
>    - High-density particle spawns in `HomeScreen` during streak completion can trigger frame drops on devices with lower GPU clock speeds.
>
> 4. **Home Screen Widget Out-of-Sync**
>    - `StreakWidgetProvider` does not always reflect Room database updates immediately unless refreshed manually or via foreground app launch.
>
> 5. **Grace Period (3:00 AM Cutoff) Edge Cases**
>    - Calculation logic in `StreakCalculator` around month-end or daylight saving boundaries can miscalculate streak continuity.

---

## 📂 Project Structure

```text
com.devson.NeverMiss/
├── App.kt                           # Application entrypoint (Hilt & WorkManager init)
├── MainActivity.kt                  # Single-activity Compose host
├── core/
│   ├── di/                          # Hilt Modules (DatabaseModule, RepositoryModule)
│   ├── reminder/                    # AlarmReceiver, BootReceiver, NotificationHelper, DailyStreakWorker
│   ├── util/                        # AppClock, Date format helpers
│   └── widget/                      # StreakWidgetProvider
├── data/
│   ├── local/
│   │   ├── db/                      # Room Entities (StreakEntity, CompletionHistoryEntity, StreakDao)
│   │   └── preferences/             # DataStore UserPreferences
│   └── repository/                  # StreakRepositoryImpl
├── domain/
│   ├── model/                       # Streak, StreakCategory, StreakCalculator
│   ├── repository/                  # StreakRepository contract
│   └── usecase/                     # CreateStreak, LogCompletion, CheckAndResetMissedStreaks
├── navigation/                      # NeverMissNavigation routes
├── ui/
│   ├── screens/                     # HomeScreen, CreateStreakScreen, AnalyticsScreen, SettingsScreen
│   ├── theme/                       # Color, Type, Material 3 Theme setup
│   └── util/                        # CategoryIconHelper
└── viewmodel/                       # HomeViewModel, CreateStreakViewModel, AnalyticsViewModel, SettingsViewModel
```

---

## 🚀 Getting Started & Building

### Prerequisites
- **Android Studio**: Studio Ladybug (2024.2.1+) or newer.
- **JDK**: Version 17.
- **Android SDK**: `compileSdk 36`, `minSdk 26`.

### Building from Source

1. **Clone the Repository**
   ```bash
   git clone https://github.com/DevSon1024/NeverMiss.git
   cd NeverMiss
   ```

2. **Build Debug APK**
   ```bash
   ./gradlew assembleDebug
   ```

3. **Run Unit Tests**
   ```bash
   ./gradlew test
   ```

4. **Install on Connected Device / Emulator**
   ```bash
   ./gradlew installDebug
   ```

---

## 🤝 Contributing & Feedback

Contributions, bug reports, and stability fixes are welcome! If you encounter crashes related to `WorkManager` or `AlarmReceiver`, please attach the logcat output when opening an issue.

---

## 📄 License

This project is licensed under the **MIT License**.

```text
MIT License

Copyright (c) 2026 DevSon1024

Permission is hereby granted, free of charge, to any person obtaining a copy
of this software and associated documentation files (the "Software"), to deal
in the Software without restriction, including without limitation the rights
to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
copies of the Software, and to permit persons to whom the Software is
furnished to do so, subject to the following conditions:

The above copyright notice and this permission notice shall be included in all
copies or substantial portions of the Software.

THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
SOFTWARE.
```
