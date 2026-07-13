package dev.prajwal.waterreminder.notification

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import dev.prajwal.waterreminder.data.SettingsRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

class BootReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent?) {
        if (intent?.action != Intent.ACTION_BOOT_COMPLETED &&
            intent?.action != Intent.ACTION_MY_PACKAGE_REPLACED
        ) {
            return
        }

        val pendingResult = goAsync()
        val appContext = context.applicationContext

        CoroutineScope(Dispatchers.IO).launch {
            try {
                val repository = SettingsRepository(appContext)
                val settings = repository.settings.first()
                if (settings.remindersEnabled) {
                    ReminderScheduler.scheduleNext(appContext, settings)
                }
            } finally {
                pendingResult.finish()
            }
        }
    }
}
