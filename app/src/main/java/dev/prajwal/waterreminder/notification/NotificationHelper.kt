package dev.prajwal.waterreminder.notification

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import dev.prajwal.waterreminder.MainActivity
import dev.prajwal.waterreminder.R

object NotificationHelper {

    const val CHANNEL_ID = "deskcare_reminder_vibrate"
    private const val CHANNEL_NAME = "DeskCare Reminders"
    private const val CHANNEL_DESCRIPTION = "High-priority desk wellness and hydration reminders"
    const val NOTIFICATION_ID = 1001

    private val vibrationPattern = longArrayOf(0, 500, 200, 500, 200, 500)

    enum class ReminderType {
        HYDRATION,
        EYE_CARE,
        POSTURE
    }

    fun createNotificationChannel(context: Context) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return

        val manager = context.getSystemService(NotificationManager::class.java) ?: return

        // Clean up legacy channel if it exists
        manager.deleteNotificationChannel("aquacue_reminder_vibrate")
        manager.deleteNotificationChannel("water_reminder_alerts")

        val channel = NotificationChannel(
            CHANNEL_ID,
            CHANNEL_NAME,
            NotificationManager.IMPORTANCE_HIGH
        ).apply {
            description = CHANNEL_DESCRIPTION
            enableVibration(true)
            vibrationPattern = vibrationPattern
            enableLights(true)
            setSound(null, null)
            setBypassDnd(false)
            lockscreenVisibility = android.app.Notification.VISIBILITY_PUBLIC
        }

        manager.createNotificationChannel(channel)
    }

    fun showReminder(context: Context, type: ReminderType) {
        createNotificationChannel(context)

        val launchIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            // Let the app know which tab to open
            putExtra("target_tab", when (type) {
                ReminderType.HYDRATION -> "dashboard"
                else -> "guides"
            })
        }
        val pendingIntent = PendingIntent.getActivity(
            context,
            0,
            launchIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val title = when (type) {
            ReminderType.HYDRATION -> context.getString(R.string.notification_title_hydration)
            ReminderType.EYE_CARE -> context.getString(R.string.notification_title_eye)
            ReminderType.POSTURE -> context.getString(R.string.notification_title_posture)
        }

        val body = when (type) {
            ReminderType.HYDRATION -> context.getString(R.string.notification_body_hydration)
            ReminderType.EYE_CARE -> context.getString(R.string.notification_body_eye)
            ReminderType.POSTURE -> context.getString(R.string.notification_body_posture)
        }

        val builder = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_water_drop)
            .setContentTitle(title)
            .setContentText(body)
            .setStyle(NotificationCompat.BigTextStyle().bigText(body))
            .setPriority(NotificationCompat.PRIORITY_MAX)
            .setCategory(NotificationCompat.CATEGORY_REMINDER)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .setSilent(true)
            .setSound(null)
            .setVibrate(vibrationPattern)
            .setDefaults(NotificationCompat.DEFAULT_VIBRATE)
            .setOnlyAlertOnce(false)

        if (type == ReminderType.HYDRATION) {
            val quickLogIntent = Intent(context, ReminderReceiver::class.java).apply {
                action = ReminderReceiver.ACTION_QUICK_LOG_WATER
                putExtra(ReminderReceiver.EXTRA_WATER_AMOUNT, 250)
            }
            val quickLogPendingIntent = PendingIntent.getBroadcast(
                context,
                101,
                quickLogIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            builder.addAction(
                R.drawable.ic_water_drop,
                context.getString(R.string.quick_log_action_label),
                quickLogPendingIntent
            )
        }

        try {
            NotificationManagerCompat.from(context).notify(NOTIFICATION_ID, builder.build())
        } catch (e: SecurityException) {
            // Permission missing
        }
    }
}
