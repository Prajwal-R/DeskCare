package dev.prajwal.waterreminder.notification

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.widget.Toast
import dev.prajwal.waterreminder.data.SettingsRepository
import dev.prajwal.waterreminder.data.WaterLogEntry
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.time.LocalDate
import java.time.LocalTime
import java.time.format.DateTimeFormatter

class ReminderReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent?) {
        val action = intent?.action ?: return
        val appContext = context.applicationContext

        if (action == ReminderScheduler.ACTION_WATER_REMINDER) {
            val pendingResult = goAsync()
            CoroutineScope(Dispatchers.IO).launch {
                try {
                    val repository = SettingsRepository(appContext)
                    val settings = repository.settings.first()

                    if (settings.remindersEnabled) {
                        val enabledTypes = mutableListOf<NotificationHelper.ReminderType>()
                        if (settings.enableHydrationReminders) enabledTypes.add(NotificationHelper.ReminderType.HYDRATION)
                        if (settings.enableEyeReminders) enabledTypes.add(NotificationHelper.ReminderType.EYE_CARE)
                        if (settings.enablePostureReminders) enabledTypes.add(NotificationHelper.ReminderType.POSTURE)

                        if (enabledTypes.isNotEmpty()) {
                            val selectedType = enabledTypes.random()
                            NotificationHelper.showReminder(appContext, selectedType)
                        }
                        ReminderScheduler.scheduleNext(appContext, settings)
                    }
                } finally {
                    pendingResult.finish()
                }
            }
        } else if (action == ACTION_QUICK_LOG_WATER) {
            val pendingResult = goAsync()
            val amount = intent.getIntExtra(EXTRA_WATER_AMOUNT, 250)
            CoroutineScope(Dispatchers.IO).launch {
                try {
                    val repository = SettingsRepository(appContext)
                    val settings = repository.settings.first()

                    val today = LocalDate.now().toString()
                    val isNewDay = settings.lastLogDate != today

                    val newIntake = if (isNewDay) amount else settings.waterIntakeToday + amount
                    val currentLogs = if (isNewDay) emptyList() else settings.getLogEntries()

                    val nowTimeStr = LocalTime.now().format(DateTimeFormatter.ofPattern("h:mm a"))
                    val newEntry = WaterLogEntry(
                        time = nowTimeStr,
                        amount = amount,
                        id = System.currentTimeMillis()
                    )

                    val updatedHistory = WaterLogEntry.serializeLogEntries(currentLogs + newEntry)

                    val updatedSettings = settings.copy(
                        waterIntakeToday = newIntake,
                        waterLogHistory = updatedHistory,
                        lastLogDate = today
                    )
                    repository.saveSettings(updatedSettings)

                    // Dismiss notification
                    val notificationManager = appContext.getSystemService(Context.NOTIFICATION_SERVICE) as android.app.NotificationManager
                    notificationManager.cancel(NotificationHelper.NOTIFICATION_ID)

                    withContext(Dispatchers.Main) {
                        Toast.makeText(appContext, "Logged +${amount}ml water! 💧", Toast.LENGTH_SHORT).show()
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                } finally {
                    pendingResult.finish()
                }
            }
        }
    }

    companion object {
        const val ACTION_QUICK_LOG_WATER = "dev.prajwal.waterreminder.ACTION_QUICK_LOG_WATER"
        const val EXTRA_WATER_AMOUNT = "extra_water_amount"
    }
}
