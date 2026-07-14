package dev.prajwal.waterreminder.data

data class UserSettings(
    val remindersEnabled: Boolean = false,
    val startHour: Int = 8,
    val startMinute: Int = 0,
    val endHour: Int = 22,
    val endMinute: Int = 0,
    val intervalMinutes: Int = 30,
    val waterTargetMl: Int = 2000,
    val waterIntakeToday: Int = 0,
    val lastLogDate: String = "",
    val waterLogHistory: String = "",
    val enableHydrationReminders: Boolean = true,
    val enableEyeReminders: Boolean = true,
    val enablePostureReminders: Boolean = true,
    val lastEyeCareTime: Long = 0L,
    val lastStretchTime: Long = 0L,
    val lastBreathingTime: Long = 0L
) {
    val startTimeMinutes: Int get() = startHour * 60 + startMinute
    val endTimeMinutes: Int get() = endHour * 60 + endMinute

    fun getLogEntries(): List<WaterLogEntry> {
        if (waterLogHistory.isEmpty()) return emptyList()
        return waterLogHistory.split(",").mapNotNull { entryStr ->
            val parts = entryStr.split("|")
            if (parts.size == 3) {
                val time = parts[0]
                val amount = parts[1].toIntOrNull() ?: 0
                val id = parts[2].toLongOrNull() ?: 0L
                WaterLogEntry(time, amount, id)
            } else {
                null
            }
        }
    }
}

data class WaterLogEntry(
    val time: String,
    val amount: Int,
    val id: Long
) {
    companion object {
        fun serializeLogEntries(entries: List<WaterLogEntry>): String {
            return entries.joinToString(",") { "${it.time}|${it.amount}|${it.id}" }
        }
    }
}


object IntervalOptions {
    const val CUSTOM = -1
    const val MIN_MINUTES = 5
    const val MAX_MINUTES = 720

    val presets = listOf(
        30 to "Every 30 minutes",
        60 to "Every 1 hour",
        120 to "Every 2 hours"
    )

    fun isPreset(minutes: Int): Boolean = presets.any { it.first == minutes }

    fun formatInterval(minutes: Int): String {
        if (minutes < 60) return "$minutes min"
        val hours = minutes / 60
        val remaining = minutes % 60
        return when {
            remaining == 0 -> if (hours == 1) "1 hour" else "$hours hours"
            hours == 0 -> "$remaining min"
            else -> "${hours}h ${remaining}m"
        }
    }
}
