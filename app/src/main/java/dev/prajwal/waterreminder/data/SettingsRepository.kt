package dev.prajwal.waterreminder.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "water_reminder_settings")

class SettingsRepository(private val context: Context) {

    val settings: Flow<UserSettings> = context.dataStore.data.map { prefs ->
        UserSettings(
            remindersEnabled = prefs[KEY_REMINDERS_ENABLED] ?: false,
            startHour = prefs[KEY_START_HOUR] ?: 8,
            startMinute = prefs[KEY_START_MINUTE] ?: 0,
            endHour = prefs[KEY_END_HOUR] ?: 22,
            endMinute = prefs[KEY_END_MINUTE] ?: 0,
            intervalMinutes = prefs[KEY_INTERVAL_MINUTES] ?: 30,
            waterTargetMl = prefs[KEY_WATER_TARGET_ML] ?: 2000,
            waterIntakeToday = prefs[KEY_WATER_INTAKE_TODAY] ?: 0,
            lastLogDate = prefs[KEY_LAST_LOG_DATE] ?: "",
            waterLogHistory = prefs[KEY_WATER_LOG_HISTORY] ?: "",
            enableHydrationReminders = prefs[KEY_ENABLE_HYDRATION_REMINDERS] ?: true,
            enableEyeReminders = prefs[KEY_ENABLE_EYE_REMINDERS] ?: true,
            enablePostureReminders = prefs[KEY_ENABLE_POSTURE_REMINDERS] ?: true,
            lastEyeCareTime = prefs[KEY_LAST_EYE_CARE_TIME] ?: 0L,
            lastStretchTime = prefs[KEY_LAST_STRETCH_TIME] ?: 0L,
            lastBreathingTime = prefs[KEY_LAST_BREATHING_TIME] ?: 0L
        )
    }

    suspend fun saveSettings(settings: UserSettings) {
        context.dataStore.edit { prefs ->
            prefs[KEY_REMINDERS_ENABLED] = settings.remindersEnabled
            prefs[KEY_START_HOUR] = settings.startHour
            prefs[KEY_START_MINUTE] = settings.startMinute
            prefs[KEY_END_HOUR] = settings.endHour
            prefs[KEY_END_MINUTE] = settings.endMinute
            prefs[KEY_INTERVAL_MINUTES] = settings.intervalMinutes
            prefs[KEY_WATER_TARGET_ML] = settings.waterTargetMl
            prefs[KEY_WATER_INTAKE_TODAY] = settings.waterIntakeToday
            prefs[KEY_LAST_LOG_DATE] = settings.lastLogDate
            prefs[KEY_WATER_LOG_HISTORY] = settings.waterLogHistory
            prefs[KEY_ENABLE_HYDRATION_REMINDERS] = settings.enableHydrationReminders
            prefs[KEY_ENABLE_EYE_REMINDERS] = settings.enableEyeReminders
            prefs[KEY_ENABLE_POSTURE_REMINDERS] = settings.enablePostureReminders
            prefs[KEY_LAST_EYE_CARE_TIME] = settings.lastEyeCareTime
            prefs[KEY_LAST_STRETCH_TIME] = settings.lastStretchTime
            prefs[KEY_LAST_BREATHING_TIME] = settings.lastBreathingTime
        }
    }

    companion object {
        private val KEY_REMINDERS_ENABLED = booleanPreferencesKey("reminders_enabled")
        private val KEY_START_HOUR = intPreferencesKey("start_hour")
        private val KEY_START_MINUTE = intPreferencesKey("start_minute")
        private val KEY_END_HOUR = intPreferencesKey("end_hour")
        private val KEY_END_MINUTE = intPreferencesKey("end_minute")
        private val KEY_INTERVAL_MINUTES = intPreferencesKey("interval_minutes")
        private val KEY_WATER_TARGET_ML = intPreferencesKey("water_target_ml")
        private val KEY_WATER_INTAKE_TODAY = intPreferencesKey("water_intake_today")
        private val KEY_LAST_LOG_DATE = stringPreferencesKey("last_log_date")
        private val KEY_WATER_LOG_HISTORY = stringPreferencesKey("water_log_history")
        private val KEY_ENABLE_HYDRATION_REMINDERS = booleanPreferencesKey("enable_hydration_reminders")
        private val KEY_ENABLE_EYE_REMINDERS = booleanPreferencesKey("enable_eye_reminders")
        private val KEY_ENABLE_POSTURE_REMINDERS = booleanPreferencesKey("enable_posture_reminders")
        private val KEY_LAST_EYE_CARE_TIME = longPreferencesKey("last_eye_care_time")
        private val KEY_LAST_STRETCH_TIME = longPreferencesKey("last_stretch_time")
        private val KEY_LAST_BREATHING_TIME = longPreferencesKey("last_breathing_time")
    }
}
