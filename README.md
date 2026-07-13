# Water Reminder

A lightweight native Android app built with **Kotlin** and **Jetpack Compose** that schedules local hydration reminders within a daily time window.

## Features

- Toggle reminders on/off
- Configurable start time, end time, and interval (30 min / 1 hr / 2 hr)
- High-priority notifications with sound and vibration
- Automatic mirroring to paired **Wear OS** watches
- Persistent settings via **DataStore**
- Survives device reboot (reschedules on boot)

## Project Structure

```
app/src/main/java/dev/prajwal/waterreminder/
├── MainActivity.kt                 # Entry point
├── data/
│   ├── UserSettings.kt             # Settings data model
│   └── SettingsRepository.kt       # DataStore persistence
├── notification/
│   ├── NotificationHelper.kt       # High-priority channel & alerts
│   ├── ReminderScheduler.kt        # AlarmManager scheduling logic
│   ├── ReminderReceiver.kt         # Fires notification + reschedules
│   └── BootReceiver.kt             # Reschedule after reboot
├── ui/
│   ├── SettingsScreen.kt           # Compose settings UI
│   └── theme/                      # Material 3 theme
└── viewmodel/
    └── SettingsViewModel.kt        # UI state & persistence
```

## Requirements

- Android Studio Ladybug or newer
- JDK 11+
- Android device or emulator (API 24+)

## Setup & Run

1. Open the project in Android Studio.
2. Sync Gradle (dependencies are declared in `gradle/libs.versions.toml`).
3. Connect a device or start an emulator.
4. Run the app (▶).

Or from the terminal:

```bash
./gradlew assembleDebug
./gradlew installDebug
```

## Dependencies

| Package | Purpose |
|---------|---------|
| `androidx.datastore:datastore-preferences` | Persist user settings |
| `androidx.lifecycle:lifecycle-viewmodel-compose` | ViewModel integration |
| `androidx.lifecycle:lifecycle-runtime-compose` | Lifecycle-aware state |
| `androidx.compose.material3` | UI components |

No third-party notification libraries are required — the app uses `AlarmManager` and `NotificationCompat`.

## How Scheduling Works

1. When reminders are enabled, `ReminderScheduler` computes the **next** valid trigger time.
2. Reminders only fire between the configured start and end times.
3. If the current time is past the end time, the first reminder is scheduled for **start time the next day**.
4. After each notification fires, `ReminderReceiver` shows the alert and schedules the next one.
5. `BootReceiver` restores the schedule after a reboot.

## Smartwatch / Wear OS

On Android, high-importance notifications with vibration automatically mirror to paired Wear OS devices. This app configures:

- `NotificationManager.IMPORTANCE_HIGH` channel
- `NotificationCompat.PRIORITY_MAX`
- Default sound + custom vibration pattern (`0, 500, 200, 500, 200, 500` ms)
- `CATEGORY_REMINDER` for proper classification

> **Note:** Apple Watch requires an iOS app. This Kotlin project targets **Android / Wear OS** only.

## Permissions

| Permission | When needed |
|------------|-------------|
| `POST_NOTIFICATIONS` | Android 13+ — requested when enabling reminders |
| `SCHEDULE_EXACT_ALARM` | Android 12+ — for precise reminder timing |
| `RECEIVE_BOOT_COMPLETED` | Reschedule after device restart |
| `VIBRATE` | Vibration on notification |

## Testing Reminders

1. Enable reminders in the app.
2. Set start time to 1–2 minutes from now.
3. Set interval to 30 minutes.
4. Grant notification permission when prompted.
5. On Android 12+, allow exact alarms if prompted.
6. Lock the screen and wait for the notification.

## License

MIT
