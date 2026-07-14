package dev.prajwal.waterreminder.ui

import android.Manifest
import android.app.TimePickerDialog
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dev.prajwal.waterreminder.R
import dev.prajwal.waterreminder.data.IntervalOptions
import dev.prajwal.waterreminder.data.UserSettings
import dev.prajwal.waterreminder.data.WaterLogEntry
import dev.prajwal.waterreminder.viewmodel.SettingsViewModel
import kotlinx.coroutines.delay
import java.time.format.DateTimeFormatter
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    viewModel: SettingsViewModel,
    initialTab: String? = null
) {
    val settings by viewModel.settings.collectAsStateWithLifecycle()
    val context = LocalContext.current

    val initialIndex = when (initialTab) {
        "dashboard" -> 0
        "guides" -> 1
        "settings" -> 2
        else -> 0
    }
    var selectedTabIndex by remember { mutableStateOf(initialIndex) }

    LaunchedEffect(initialTab) {
        if (initialTab != null) {
            selectedTabIndex = when (initialTab) {
                "dashboard" -> 0
                "guides" -> 1
                "settings" -> 2
                else -> 0
            }
        }
    }

    val notificationPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) {
            viewModel.updateRemindersEnabled(true)
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.WaterDrop,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary
                            )
                            Text(
                                text = stringResource(R.string.app_name),
                                modifier = Modifier.padding(start = 8.dp),
                                fontWeight = FontWeight.Bold
                            )
                        }
                        Text(
                            text = stringResource(R.string.app_tagline),
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(start = 32.dp, top = 2.dp)
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        },
        bottomBar = {
            NavigationBar {
                NavigationBarItem(
                    selected = selectedTabIndex == 0,
                    onClick = { selectedTabIndex = 0 },
                    icon = { Icon(Icons.Default.Dashboard, contentDescription = null) },
                    label = { Text(stringResource(R.string.tab_dashboard)) }
                )
                NavigationBarItem(
                    selected = selectedTabIndex == 1,
                    onClick = { selectedTabIndex = 1 },
                    icon = { Icon(Icons.Default.SelfImprovement, contentDescription = null) },
                    label = { Text(stringResource(R.string.tab_guides)) }
                )
                NavigationBarItem(
                    selected = selectedTabIndex == 2,
                    onClick = { selectedTabIndex = 2 },
                    icon = { Icon(Icons.Default.Settings, contentDescription = null) },
                    label = { Text(stringResource(R.string.tab_settings)) }
                )
            }
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            when (selectedTabIndex) {
                0 -> DashboardTab(viewModel = viewModel, settings = settings)
                1 -> GuidesTab(viewModel = viewModel, settings = settings)
                2 -> SettingsTab(
                    viewModel = viewModel,
                    settings = settings,
                    notificationPermissionLauncher = notificationPermissionLauncher
                )
            }
        }
    }
}

@Composable
fun DashboardTab(
    viewModel: SettingsViewModel,
    settings: UserSettings
) {
    var showCustomIntakeDialog by remember { mutableStateOf(false) }

    if (showCustomIntakeDialog) {
        CustomIntakeDialog(
            onDismiss = { showCustomIntakeDialog = false },
            onConfirm = { amount ->
                viewModel.logWater(amount)
                showCustomIntakeDialog = false
            }
        )
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp, vertical = 16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(20.dp)
    ) {
        val progress = if (settings.waterTargetMl > 0) {
            settings.waterIntakeToday.toFloat() / settings.waterTargetMl
        } else {
            0f
        }

        Text(
            text = stringResource(R.string.hydration_progress_title),
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.align(Alignment.Start)
        )

        HydrationProgressRing(
            progress = progress,
            current = settings.waterIntakeToday,
            target = settings.waterTargetMl
        )

        if (settings.waterIntakeToday >= settings.waterTargetMl && settings.waterTargetMl > 0) {
            Card(
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f)
                ),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = stringResource(R.string.hydration_goal_reached),
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.primary,
                    textAlign = TextAlign.Center,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                )
            }
        }

        Card(
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = stringResource(R.string.quick_add_title),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Button(
                        onClick = { viewModel.logWater(250) },
                        modifier = Modifier.weight(1f),
                        contentPadding = PaddingValues(horizontal = 8.dp)
                    ) {
                        Icon(Icons.Default.LocalDrink, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(stringResource(R.string.cup_label), style = MaterialTheme.typography.labelMedium)
                    }

                    Button(
                        onClick = { viewModel.logWater(500) },
                        modifier = Modifier.weight(1f),
                        contentPadding = PaddingValues(horizontal = 8.dp)
                    ) {
                        Icon(Icons.Default.WaterDrop, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(stringResource(R.string.bottle_label), style = MaterialTheme.typography.labelMedium)
                    }

                    OutlinedButton(
                        onClick = { showCustomIntakeDialog = true },
                        modifier = Modifier.weight(1f),
                        contentPadding = PaddingValues(horizontal = 8.dp)
                    ) {
                        Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(stringResource(R.string.custom_label), style = MaterialTheme.typography.labelMedium)
                    }
                }
            }
        }

        Card(
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = stringResource(R.string.log_history_title),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold
                )

                val logs = settings.getLogEntries()

                if (logs.isEmpty()) {
                    Text(
                        text = stringResource(R.string.no_logs_yet),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(vertical = 8.dp)
                    )
                } else {
                    logs.forEachIndexed { index, entry ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.LocalDrink,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(24.dp)
                                )
                                Spacer(modifier = Modifier.width(12.dp))
                                Column {
                                    Text(
                                        text = "${entry.amount} ml",
                                        style = MaterialTheme.typography.bodyLarge,
                                        fontWeight = FontWeight.Medium
                                    )
                                    Text(
                                        text = entry.time,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }

                            IconButton(
                                onClick = { viewModel.deleteLogEntry(entry.id) }
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Delete,
                                    contentDescription = stringResource(R.string.delete_entry_desc),
                                    tint = MaterialTheme.colorScheme.error
                                )
                            }
                        }

                        if (index < logs.size - 1) {
                            HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun HydrationProgressRing(
    progress: Float,
    current: Int,
    target: Int,
    modifier: Modifier = Modifier
) {
    val primaryColor = MaterialTheme.colorScheme.primary
    val trackColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.35f)

    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier
            .size(220.dp)
            .padding(10.dp)
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            drawCircle(
                color = trackColor,
                style = Stroke(width = 16.dp.toPx(), cap = StrokeCap.Round)
            )
            drawArc(
                color = primaryColor,
                startAngle = -90f,
                sweepAngle = progress.coerceIn(0f, 1f) * 360f,
                useCenter = false,
                style = Stroke(width = 16.dp.toPx(), cap = StrokeCap.Round)
            )
        }
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            val displayPercent = if (target > 0) (progress * 100).toInt() else 0
            Text(
                text = "$displayPercent%",
                style = MaterialTheme.typography.displayMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "$current / $target ml",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
fun CustomIntakeDialog(
    onDismiss: () -> Unit,
    onConfirm: (Int) -> Unit
) {
    var amountText by remember { mutableStateOf("") }
    var showError by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.custom_intake_title)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = stringResource(R.string.custom_intake_hint),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                OutlinedTextField(
                    value = amountText,
                    onValueChange = {
                        amountText = it.filter { ch -> ch.isDigit() }.take(4)
                        showError = false
                    },
                    label = { Text(stringResource(R.string.custom_intake_label)) },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    isError = showError,
                    modifier = Modifier.fillMaxWidth()
                )
                if (showError) {
                    Text(
                        text = stringResource(R.string.custom_intake_error),
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val amount = amountText.toIntOrNull() ?: 0
                    if (amount < 50 || amount > 2000) {
                        showError = true
                    } else {
                        onConfirm(amount)
                    }
                }
            ) {
                Text(stringResource(R.string.save))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.cancel))
            }
        }
    )
}

@Composable
fun GuidesTab(
    viewModel: SettingsViewModel,
    settings: UserSettings
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp, vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Column {
            Text(
                text = stringResource(R.string.guides_title),
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = stringResource(R.string.guides_subtitle),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 4.dp)
            )
        }

        EyeCareCard(viewModel = viewModel, lastCompleted = settings.lastEyeCareTime)
        StretchCard(viewModel = viewModel, lastCompleted = settings.lastStretchTime)
        BreathingCard(viewModel = viewModel, lastCompleted = settings.lastBreathingTime)
    }
}

@Composable
fun EyeCareCard(
    viewModel: SettingsViewModel,
    lastCompleted: Long
) {
    var timerRemaining by remember { mutableStateOf(20) }
    var isRunning by remember { mutableStateOf(false) }
    var isDone by remember { mutableStateOf(false) }

    LaunchedEffect(isRunning, timerRemaining) {
        if (isRunning && timerRemaining > 0) {
            delay(1000)
            timerRemaining -= 1
            if (timerRemaining == 0) {
                isRunning = false
                isDone = true
                viewModel.updateLastEyeCareTime(System.currentTimeMillis())
            }
        }
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .animateContentSize(),
        colors = CardDefaults.cardColors(
            containerColor = if (isRunning) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.25f) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
        )
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.Visibility,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary
                )
                Text(
                    text = stringResource(R.string.eye_care_title),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(start = 8.dp)
                )
            }
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = stringResource(R.string.eye_care_desc),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = "Last completed: ${formatLastCompletedTime(lastCompleted)}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                modifier = Modifier.padding(top = 6.dp)
            )
            Spacer(modifier = Modifier.height(16.dp))

            if (isRunning) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = stringResource(R.string.eye_timer_running),
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Text(
                            text = "$timerRemaining seconds remaining",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Button(
                        onClick = {
                            isRunning = false
                            timerRemaining = 20
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                    ) {
                        Text(stringResource(R.string.cancel))
                    }
                }
                Spacer(modifier = Modifier.height(12.dp))
                LinearProgressIndicator(
                    progress = { timerRemaining.toFloat() / 20f },
                    modifier = Modifier.fillMaxWidth(),
                    strokeCap = StrokeCap.Round
                )
            } else if (isDone) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = stringResource(R.string.eye_timer_done),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.SemiBold
                    )
                    Button(
                        onClick = {
                            isDone = false
                            timerRemaining = 20
                            isRunning = true
                        }
                    ) {
                        Text("Restart")
                    }
                }
            } else {
                Button(
                    onClick = { isRunning = true }
                ) {
                    Text(stringResource(R.string.start_eye_timer))
                }
            }
        }
    }
}

@Composable
fun StretchCard(
    viewModel: SettingsViewModel,
    lastCompleted: Long
) {
    var timerRemaining by remember { mutableStateOf(30) }
    var isRunning by remember { mutableStateOf(false) }
    var isDone by remember { mutableStateOf(false) }

    LaunchedEffect(isRunning, timerRemaining) {
        if (isRunning && timerRemaining > 0) {
            delay(1000)
            timerRemaining -= 1
            if (timerRemaining == 0) {
                isRunning = false
                isDone = true
                viewModel.updateLastStretchTime(System.currentTimeMillis())
            }
        }
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .animateContentSize(),
        colors = CardDefaults.cardColors(
            containerColor = if (isRunning) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.25f) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
        )
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.Accessibility,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary
                )
                Text(
                    text = stringResource(R.string.stretch_title),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(start = 8.dp)
                )
            }
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = stringResource(R.string.stretch_desc),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = "Last completed: ${formatLastCompletedTime(lastCompleted)}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                modifier = Modifier.padding(top = 6.dp)
            )
            Spacer(modifier = Modifier.height(16.dp))

            if (isRunning) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = stringResource(R.string.stretch_timer_running),
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Text(
                            text = "$timerRemaining seconds remaining",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Button(
                        onClick = {
                            isRunning = false
                            timerRemaining = 30
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                    ) {
                        Text(stringResource(R.string.cancel))
                    }
                }
                Spacer(modifier = Modifier.height(12.dp))
                LinearProgressIndicator(
                    progress = { timerRemaining.toFloat() / 30f },
                    modifier = Modifier.fillMaxWidth(),
                    strokeCap = StrokeCap.Round
                )
            } else if (isDone) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = stringResource(R.string.stretch_timer_done),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.SemiBold
                    )
                    Button(
                        onClick = {
                            isDone = false
                            timerRemaining = 30
                            isRunning = true
                        }
                    ) {
                        Text("Restart")
                    }
                }
            } else {
                Button(
                    onClick = { isRunning = true }
                ) {
                    Text(stringResource(R.string.start_stretch_timer))
                }
            }
        }
    }
}

@Composable
fun BreathingCard(
    viewModel: SettingsViewModel,
    lastCompleted: Long
) {
    var breathingPhase by remember { mutableStateOf(0) } // 0=idle, 1=inhale, 2=hold, 3=exhale, 4=done
    var phaseSecondsRemaining by remember { mutableStateOf(4) }
    var cyclesRemaining by remember { mutableStateOf(4) }

    LaunchedEffect(breathingPhase, phaseSecondsRemaining) {
        if (breathingPhase in 1..3) {
            delay(1000)
            if (phaseSecondsRemaining > 1) {
                phaseSecondsRemaining -= 1
            } else {
                when (breathingPhase) {
                    1 -> {
                        breathingPhase = 2
                        phaseSecondsRemaining = 4
                    }
                    2 -> {
                        breathingPhase = 3
                        phaseSecondsRemaining = 4
                    }
                    3 -> {
                        if (cyclesRemaining > 1) {
                            cyclesRemaining -= 1
                            breathingPhase = 1
                            phaseSecondsRemaining = 4
                        } else {
                            breathingPhase = 4
                            viewModel.updateLastBreathingTime(System.currentTimeMillis())
                        }
                    }
                }
            }
        }
    }

    val scaleTarget = when (breathingPhase) {
        1 -> 1.5f
        2 -> 1.5f
        3 -> 0.7f
        else -> 1.0f
    }
    val scale by animateFloatAsState(
        targetValue = scaleTarget,
        animationSpec = tween(
            durationMillis = if (breathingPhase == 1 || breathingPhase == 3) 4000 else 500,
            easing = LinearEasing
        ),
        label = "Breathing Circle Scale"
    )

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .animateContentSize(),
        colors = CardDefaults.cardColors(
            containerColor = if (breathingPhase in 1..3) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.25f) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Start
            ) {
                Icon(
                    imageVector = Icons.Default.SelfImprovement,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary
                )
                Text(
                    text = stringResource(R.string.breathing_title),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(start = 8.dp)
                )
            }
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = stringResource(R.string.breathing_desc),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.fillMaxWidth()
            )
            Text(
                text = "Last completed: ${formatLastCompletedTime(lastCompleted)}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                modifier = Modifier.fillMaxWidth().padding(top = 6.dp)
            )
            Spacer(modifier = Modifier.height(16.dp))

            if (breathingPhase in 1..3) {
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .size(160.dp)
                        .padding(10.dp)
                ) {
                    Surface(
                        modifier = Modifier
                            .size(80.dp * scale)
                            .clip(CircleShape),
                        color = MaterialTheme.colorScheme.primary.copy(alpha = 0.35f),
                        border = androidx.compose.foundation.BorderStroke(2.dp, MaterialTheme.colorScheme.primary)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Text(
                                text = "$phaseSecondsRemaining",
                                style = MaterialTheme.typography.titleLarge,
                                color = MaterialTheme.colorScheme.primary,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                val instructionText = when (breathingPhase) {
                    1 -> stringResource(R.string.breathing_inhale)
                    2 -> stringResource(R.string.breathing_hold)
                    3 -> stringResource(R.string.breathing_exhale)
                    else -> ""
                }

                Text(
                    text = instructionText,
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )

                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Cycle ${5 - cyclesRemaining} of 4",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Spacer(modifier = Modifier.height(16.dp))
                Button(
                    onClick = { breathingPhase = 0 },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) {
                    Text(stringResource(R.string.stop_session))
                }
            } else if (breathingPhase == 4) {
                Text(
                    text = stringResource(R.string.breathing_done),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.SemiBold,
                    textAlign = TextAlign.Center
                )
                Spacer(modifier = Modifier.height(16.dp))
                Button(
                    onClick = {
                        cyclesRemaining = 4
                        phaseSecondsRemaining = 4
                        breathingPhase = 1
                    }
                ) {
                    Text("Start Again")
                }
            } else {
                Button(
                    onClick = {
                        cyclesRemaining = 4
                        phaseSecondsRemaining = 4
                        breathingPhase = 1
                    },
                    modifier = Modifier.align(Alignment.Start)
                ) {
                    Text(stringResource(R.string.start_breathing))
                }
            }
        }
    }
}

private fun formatLastCompletedTime(timestamp: Long): String {
    if (timestamp == 0L) return "Never"
    val lastTime = java.time.Instant.ofEpochMilli(timestamp)
        .atZone(java.time.ZoneId.systemDefault())
        .toLocalDateTime()
    val today = java.time.LocalDate.now()
    val lastDate = lastTime.toLocalDate()

    val timeFormatter = java.time.format.DateTimeFormatter.ofPattern("h:mm a")
    val timeStr = lastTime.format(timeFormatter)

    return when {
        lastDate == today -> "Today at $timeStr"
        lastDate == today.minusDays(1) -> "Yesterday at $timeStr"
        else -> {
            val dateFormatter = java.time.format.DateTimeFormatter.ofPattern("MMM d, h:mm a")
            lastTime.format(dateFormatter)
        }
    }
}

@Composable
fun SettingsTab(
    viewModel: SettingsViewModel,
    settings: UserSettings,
    notificationPermissionLauncher: androidx.activity.result.ActivityResultLauncher<String>
) {
    val context = LocalContext.current

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp, vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        RemindersToggleCard(
            enabled = settings.remindersEnabled,
            onToggle = { enabled ->
                if (!enabled) {
                    viewModel.updateRemindersEnabled(false)
                    return@RemindersToggleCard
                }
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    val granted = ContextCompat.checkSelfPermission(
                        context,
                        Manifest.permission.POST_NOTIFICATIONS
                    ) == PackageManager.PERMISSION_GRANTED
                    if (granted) {
                        viewModel.updateRemindersEnabled(true)
                    } else {
                        notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                    }
                } else {
                    viewModel.updateRemindersEnabled(true)
                }
            }
        )

        if (settings.remindersEnabled) {
            WellnessPreferencesCard(
                settings = settings,
                onWaterGoalChange = viewModel::updateWaterTarget,
                onReminderTogglesChange = viewModel::updateReminderToggles
            )

            ScheduleCard(
                settings = settings,
                onStartTimeClick = {
                    showTimePicker(
                        context = context,
                        hour = settings.startHour,
                        minute = settings.startMinute,
                        onTimeSelected = viewModel::updateStartTime
                    )
                },
                onEndTimeClick = {
                    showTimePicker(
                        context = context,
                        hour = settings.endHour,
                        minute = settings.endMinute,
                        onTimeSelected = viewModel::updateEndTime
                    )
                },
                onIntervalSelected = viewModel::updateInterval
            )

            InfoCard(text = stringResource(R.string.wearable_info))
        }

        if (settings.remindersEnabled && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val alarmManager = context.getSystemService(android.app.AlarmManager::class.java)
            if (alarmManager != null && !alarmManager.canScheduleExactAlarms()) {
                ExactAlarmCard(
                    onOpenSettings = {
                        val intent = Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM).apply {
                            data = Uri.parse("package:${context.packageName}")
                        }
                        context.startActivity(intent)
                    }
                )
            }
        }
    }
}

@Composable
private fun WellnessPreferencesCard(
    settings: UserSettings,
    onWaterGoalChange: (Int) -> Unit,
    onReminderTogglesChange: (Boolean, Boolean, Boolean) -> Unit
) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(
                text = stringResource(R.string.preferences_title),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold
            )

            Column {
                Text(
                    text = stringResource(R.string.daily_water_goal_label, settings.waterTargetMl),
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Medium
                )
                Slider(
                    value = settings.waterTargetMl.toFloat(),
                    onValueChange = { onWaterGoalChange(it.toInt()) },
                    valueRange = 1000f..4000f,
                    steps = 11, // step 250ml: 1000 to 4000 is 3000 range. 3000 / 250 = 12 steps total, meaning 11 steps between
                    modifier = Modifier.fillMaxWidth()
                )
            }

            HorizontalDivider()

            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = stringResource(R.string.reminder_types_label),
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Medium
                )

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Checkbox(
                        checked = settings.enableHydrationReminders,
                        onCheckedChange = { checked ->
                            onReminderTogglesChange(checked, settings.enableEyeReminders, settings.enablePostureReminders)
                        }
                    )
                    Text(
                        text = stringResource(R.string.reminder_type_hydration),
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.padding(start = 8.dp)
                    )
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Checkbox(
                        checked = settings.enableEyeReminders,
                        onCheckedChange = { checked ->
                            onReminderTogglesChange(settings.enableHydrationReminders, checked, settings.enablePostureReminders)
                        }
                    )
                    Text(
                        text = stringResource(R.string.reminder_type_eye),
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.padding(start = 8.dp)
                    )
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Checkbox(
                        checked = settings.enablePostureReminders,
                        onCheckedChange = { checked ->
                            onReminderTogglesChange(settings.enableHydrationReminders, settings.enableEyeReminders, checked)
                        }
                    )
                    Text(
                        text = stringResource(R.string.reminder_type_posture),
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.padding(start = 8.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun RemindersToggleCard(
    enabled: Boolean,
    onToggle: (Boolean) -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.35f)
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Notifications,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        text = stringResource(R.string.reminders_toggle_label),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.padding(start = 8.dp)
                    )
                }
                Text(
                    text = stringResource(
                        if (enabled) R.string.reminders_on_subtitle else R.string.reminders_off_subtitle
                    ),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 4.dp)
                )
            }
            Switch(checked = enabled, onCheckedChange = onToggle)
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ScheduleCard(
    settings: UserSettings,
    onStartTimeClick: () -> Unit,
    onEndTimeClick: () -> Unit,
    onIntervalSelected: (Int) -> Unit
) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(20.dp)) {
            Text(
                text = stringResource(R.string.schedule_title),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold
            )
            Spacer(modifier = Modifier.height(16.dp))

            TimePickerRow(
                label = stringResource(R.string.start_time_label),
                timeText = formatTime(settings.startHour, settings.startMinute),
                onClick = onStartTimeClick
            )

            HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp))

            TimePickerRow(
                label = stringResource(R.string.end_time_label),
                timeText = formatTime(settings.endHour, settings.endMinute),
                onClick = onEndTimeClick
            )

            HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp))

            IntervalDropdown(
                selectedMinutes = settings.intervalMinutes,
                onIntervalSelected = onIntervalSelected
            )
        }
    }
}

@Composable
private fun TimePickerRow(
    label: String,
    timeText: String,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column {
            Text(
                text = label,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = timeText,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Medium
            )
        }
        TextButton(onClick = onClick) {
            Icon(Icons.Default.AccessTime, contentDescription = null)
            Text(text = stringResource(R.string.change), modifier = Modifier.padding(start = 4.dp))
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun IntervalDropdown(
    selectedMinutes: Int,
    onIntervalSelected: (Int) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }
    var showCustomDialog by remember { mutableStateOf(false) }

    val selectedLabel = if (IntervalOptions.isPreset(selectedMinutes)) {
        val presetLabel = IntervalOptions.presets.first { it.first == selectedMinutes }.second
        presetLabel
    } else {
        stringResource(
            R.string.interval_custom,
            IntervalOptions.formatInterval(selectedMinutes)
        )
    }

    if (showCustomDialog) {
        CustomIntervalDialog(
            currentMinutes = selectedMinutes,
            onDismiss = { showCustomDialog = false },
            onSave = { minutes ->
                onIntervalSelected(minutes)
                showCustomDialog = false
            }
        )
    }

    ExposedDropdownMenuBox(
        expanded = expanded,
        onExpandedChange = { expanded = it }
    ) {
        OutlinedTextField(
            value = selectedLabel,
            onValueChange = {},
            readOnly = true,
            label = { Text(stringResource(R.string.interval_label)) },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
            modifier = Modifier
                .menuAnchor()
                .fillMaxWidth()
        )
        ExposedDropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false }
        ) {
            IntervalOptions.presets.forEach { (minutes, label) ->
                DropdownMenuItem(
                    text = { Text(label) },
                    onClick = {
                        onIntervalSelected(minutes)
                        expanded = false
                    }
                )
            }
            DropdownMenuItem(
                text = { Text(stringResource(R.string.interval_custom_option)) },
                onClick = {
                    expanded = false
                    showCustomDialog = true
                }
            )
        }
    }
}

@Composable
private fun CustomIntervalDialog(
    currentMinutes: Int,
    onDismiss: () -> Unit,
    onSave: (Int) -> Unit
) {
    var hoursText by remember(currentMinutes) {
        mutableStateOf((currentMinutes / 60).toString())
    }
    var minutesText by remember(currentMinutes) {
        mutableStateOf((currentMinutes % 60).toString())
    }
    var showError by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.custom_interval_title)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(
                    text = stringResource(R.string.custom_interval_hint),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    OutlinedTextField(
                        value = hoursText,
                        onValueChange = {
                            hoursText = it.filter { ch -> ch.isDigit() }.take(2)
                            showError = false
                        },
                        label = { Text(stringResource(R.string.custom_interval_hours)) },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                    OutlinedTextField(
                        value = minutesText,
                        onValueChange = {
                            minutesText = it.filter { ch -> ch.isDigit() }.take(2)
                            showError = false
                        },
                        label = { Text(stringResource(R.string.custom_interval_minutes)) },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                }
                if (showError) {
                    Text(
                        text = stringResource(R.string.custom_interval_error),
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val hours = hoursText.toIntOrNull() ?: 0
                    val minutes = minutesText.toIntOrNull() ?: 0
                    val total = (hours * 60) + minutes
                    if (total < IntervalOptions.MIN_MINUTES || total > IntervalOptions.MAX_MINUTES) {
                        showError = true
                    } else {
                        onSave(total)
                    }
                }
            ) {
                Text(stringResource(R.string.save))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.cancel))
            }
        }
    )
}

@Composable
private fun InfoCard(text: String) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
        )
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(16.dp)
        )
    }
}

@Composable
private fun ExactAlarmCard(onOpenSettings: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.4f)
        )
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = stringResource(R.string.exact_alarm_title),
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold
            )
            Text(
                text = stringResource(R.string.exact_alarm_body),
                style = MaterialTheme.typography.bodySmall,
                modifier = Modifier.padding(top = 4.dp, bottom = 8.dp)
            )
            TextButton(onClick = onOpenSettings) {
                Text(stringResource(R.string.open_settings))
            }
        }
    }
}

private fun showTimePicker(
    context: android.content.Context,
    hour: Int,
    minute: Int,
    onTimeSelected: (Int, Int) -> Unit
) {
    TimePickerDialog(
        context,
        { _, selectedHour, selectedMinute ->
            onTimeSelected(selectedHour, selectedMinute)
        },
        hour,
        minute,
        false
    ).show()
}

private fun formatTime(hour: Int, minute: Int): String {
    val formatter = DateTimeFormatter.ofPattern("h:mm a", Locale.getDefault())
    return java.time.LocalTime.of(hour, minute).format(formatter)
}
