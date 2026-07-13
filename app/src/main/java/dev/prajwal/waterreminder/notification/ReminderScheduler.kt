package dev.prajwal.waterreminder.notification

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import dev.prajwal.waterreminder.data.UserSettings
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.ZoneId

object ReminderScheduler {

    const val ACTION_WATER_REMINDER = "dev.prajwal.waterreminder.ACTION_WATER_REMINDER"
    private const val REQUEST_CODE = 42

    fun scheduleNext(context: Context, settings: UserSettings) {
        cancelAll(context)

        if (!settings.remindersEnabled) return

        val nextTrigger = computeNextTriggerTime(settings) ?: return
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val pendingIntent = createPendingIntent(context)

        val triggerAtMillis = nextTrigger
            .atZone(ZoneId.systemDefault())
            .toInstant()
            .toEpochMilli()

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && !alarmManager.canScheduleExactAlarms()) {
            alarmManager.setAndAllowWhileIdle(
                AlarmManager.RTC_WAKEUP,
                triggerAtMillis,
                pendingIntent
            )
        } else {
            alarmManager.setExactAndAllowWhileIdle(
                AlarmManager.RTC_WAKEUP,
                triggerAtMillis,
                pendingIntent
            )
        }
    }

    fun cancelAll(context: Context) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        alarmManager.cancel(createPendingIntent(context))
    }

    fun computeNextTriggerTime(settings: UserSettings, from: LocalDateTime = LocalDateTime.now()): LocalDateTime? {
        if (!settings.remindersEnabled) return null
        if (settings.intervalMinutes <= 0) return null

        val startTime = LocalTime.of(settings.startHour, settings.startMinute)
        val endTime = LocalTime.of(settings.endHour, settings.endMinute)
        val today = from.toLocalDate()
        val nowTime = from.toLocalTime()

        if (settings.startTimeMinutes >= settings.endTimeMinutes) return null

        val todayStart = LocalDateTime.of(today, startTime)
        val todayEnd = LocalDateTime.of(today, endTime)

        if (from.isAfter(todayEnd)) {
            return firstSlotOnDay(today.plusDays(1), settings)
        }

        if (from.isBefore(todayStart)) {
            return todayStart
        }

        val minutesSinceStart = java.time.Duration.between(todayStart, from).toMinutes()
        val intervalsElapsed = minutesSinceStart / settings.intervalMinutes
        var candidate = todayStart.plusMinutes((intervalsElapsed + 1) * settings.intervalMinutes.toLong())

        if (candidate.isAfter(todayEnd)) {
            return firstSlotOnDay(today.plusDays(1), settings)
        }

        if (candidate.isBefore(from) || candidate.isEqual(from)) {
            candidate = candidate.plusMinutes(settings.intervalMinutes.toLong())
            if (candidate.isAfter(todayEnd)) {
                return firstSlotOnDay(today.plusDays(1), settings)
            }
        }

        return candidate
    }

    private fun firstSlotOnDay(date: LocalDate, settings: UserSettings): LocalDateTime {
        return LocalDateTime.of(
            date,
            LocalTime.of(settings.startHour, settings.startMinute)
        )
    }

    private fun createPendingIntent(context: Context): PendingIntent {
        val intent = Intent(context, ReminderReceiver::class.java).apply {
            action = ACTION_WATER_REMINDER
        }
        return PendingIntent.getBroadcast(
            context,
            REQUEST_CODE,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }
}
