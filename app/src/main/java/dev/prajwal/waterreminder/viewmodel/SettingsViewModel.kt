package dev.prajwal.waterreminder.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import dev.prajwal.waterreminder.data.IntervalOptions
import dev.prajwal.waterreminder.data.SettingsRepository
import dev.prajwal.waterreminder.data.UserSettings
import dev.prajwal.waterreminder.data.WaterLogEntry
import dev.prajwal.waterreminder.notification.ReminderScheduler
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.LocalTime
import java.time.format.DateTimeFormatter

class SettingsViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = SettingsRepository(application)

    val settings: StateFlow<UserSettings> = repository.settings
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = UserSettings()
        )

    init {
        checkAndResetDailyIntake()
    }

    fun checkAndResetDailyIntake() {
        viewModelScope.launch {
            val today = LocalDate.now().toString()
            val current = repository.settings.first()
            if (current.lastLogDate != today) {
                val updated = current.copy(
                    waterIntakeToday = 0,
                    waterLogHistory = "",
                    lastLogDate = today
                )
                repository.saveSettings(updated)
            }
        }
    }

    fun logWater(amountMl: Int) {
        viewModelScope.launch {
            val today = LocalDate.now().toString()
            val current = repository.settings.first()
            val isNewDay = current.lastLogDate != today

            val newIntake = if (isNewDay) amountMl else current.waterIntakeToday + amountMl
            val currentLogs = if (isNewDay) emptyList() else current.getLogEntries()

            val nowTimeStr = LocalTime.now().format(DateTimeFormatter.ofPattern("h:mm a"))
            val newEntry = WaterLogEntry(
                time = nowTimeStr,
                amount = amountMl,
                id = System.currentTimeMillis()
            )

            val updatedHistory = WaterLogEntry.serializeLogEntries(currentLogs + newEntry)

            updateSettings(current.copy(
                waterIntakeToday = newIntake,
                waterLogHistory = updatedHistory,
                lastLogDate = today
            ))
        }
    }

    fun deleteLogEntry(id: Long) {
        viewModelScope.launch {
            val current = repository.settings.first()
            val currentLogs = current.getLogEntries()
            val entryToDelete = currentLogs.find { it.id == id } ?: return@launch
            val newLogs = currentLogs.filter { it.id != id }
            val newIntake = (current.waterIntakeToday - entryToDelete.amount).coerceAtLeast(0)
            val updatedHistory = WaterLogEntry.serializeLogEntries(newLogs)

            updateSettings(current.copy(
                waterIntakeToday = newIntake,
                waterLogHistory = updatedHistory
            ))
        }
    }

    fun updateWaterTarget(targetMl: Int) {
        updateSettings(settings.value.copy(waterTargetMl = targetMl))
    }

    fun updateReminderToggles(hydration: Boolean, eye: Boolean, posture: Boolean) {
        updateSettings(settings.value.copy(
            enableHydrationReminders = hydration,
            enableEyeReminders = eye,
            enablePostureReminders = posture
        ))
    }

    fun updateLastEyeCareTime(timestamp: Long) {
        updateSettings(settings.value.copy(lastEyeCareTime = timestamp))
    }

    fun updateLastStretchTime(timestamp: Long) {
        updateSettings(settings.value.copy(lastStretchTime = timestamp))
    }

    fun updateLastBreathingTime(timestamp: Long) {
        updateSettings(settings.value.copy(lastBreathingTime = timestamp))
    }

    fun updateRemindersEnabled(enabled: Boolean) {
        updateSettings(settings.value.copy(remindersEnabled = enabled))
    }

    fun updateStartTime(hour: Int, minute: Int) {
        updateSettings(settings.value.copy(startHour = hour, startMinute = minute))
    }

    fun updateEndTime(hour: Int, minute: Int) {
        updateSettings(settings.value.copy(endHour = hour, endMinute = minute))
    }

    fun updateInterval(intervalMinutes: Int) {
        val clamped = intervalMinutes.coerceIn(IntervalOptions.MIN_MINUTES, IntervalOptions.MAX_MINUTES)
        updateSettings(settings.value.copy(intervalMinutes = clamped))
    }

    private fun updateSettings(newSettings: UserSettings) {
        val validated = validateSettings(newSettings)
        viewModelScope.launch {
            repository.saveSettings(validated)
            if (validated.remindersEnabled && (validated.enableHydrationReminders || validated.enableEyeReminders || validated.enablePostureReminders)) {
                ReminderScheduler.scheduleNext(getApplication(), validated)
            } else {
                ReminderScheduler.cancelAll(getApplication())
            }
        }
    }

    private fun validateSettings(settings: UserSettings): UserSettings {
        if (settings.startTimeMinutes >= settings.endTimeMinutes) {
            return settings.copy(
                endHour = settings.startHour + 1,
                endMinute = settings.startMinute
            )
        }
        return settings
    }
}
