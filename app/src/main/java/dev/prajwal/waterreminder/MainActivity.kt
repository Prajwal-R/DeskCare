package dev.prajwal.waterreminder

import android.app.Application
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import dev.prajwal.waterreminder.data.SettingsRepository
import dev.prajwal.waterreminder.notification.NotificationHelper
import dev.prajwal.waterreminder.notification.ReminderScheduler
import dev.prajwal.waterreminder.ui.SettingsScreen
import dev.prajwal.waterreminder.ui.theme.WaterReminderTheme
import dev.prajwal.waterreminder.viewmodel.SettingsViewModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {

    private val viewModel: SettingsViewModel by viewModels {
        SettingsViewModelFactory(application)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        NotificationHelper.createNotificationChannel(this)
        rescheduleIfNeeded()
        enableEdgeToEdge()

        val initialTab = intent?.getStringExtra("target_tab")

        setContent {
            WaterReminderTheme {
                Surface(modifier = Modifier.fillMaxSize()) {
                    SettingsScreen(viewModel = viewModel, initialTab = initialTab)
                }
            }
        }
    }

    private fun rescheduleIfNeeded() {
        CoroutineScope(Dispatchers.IO).launch {
            val repository = SettingsRepository(application)
            val settings = repository.settings.first()
            if (settings.remindersEnabled) {
                ReminderScheduler.scheduleNext(application, settings)
            }
        }
    }
}

class SettingsViewModelFactory(
    private val application: Application
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(SettingsViewModel::class.java)) {
            return SettingsViewModel(application) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
