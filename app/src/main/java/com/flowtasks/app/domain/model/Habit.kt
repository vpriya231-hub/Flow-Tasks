package com.flowtasks.app.domain.model

enum class HabitFrequencyType {
    DAILY,
    WEEKLY,
    CUSTOM_DAYS
}

enum class HabitStatus {
    ACTIVE,
    ARCHIVED
}

data class HabitDayStatus(
    val dateMillis: Long,
    val dayOfWeek: Int, // 1..7 (Monday..Sunday)
    val dayLabel: String,
    val isCompleted: Boolean,
    val isScheduled: Boolean,
    val isToday: Boolean
)

data class Habit(
    val id: Long = 0,
    val title: String,
    val description: String = "",
    val frequencyType: HabitFrequencyType = HabitFrequencyType.DAILY,
    val frequencyDays: List<Int> = emptyList(), // 1=Mon, 2=Tue, 3=Wed, 4=Thu, 5=Fri, 6=Sat, 7=Sun
    val targetCountPerPeriod: Int = 1,
    val status: HabitStatus = HabitStatus.ACTIVE,
    val colorHex: String = "#10B981",
    val sortOrder: Int = 0,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis(),
    val isCompletedToday: Boolean = false,
    val currentStreak: Int = 0,
    val bestStreak: Int = 0,
    val totalCompletions: Int = 0,
    val weeklyHistory: List<HabitDayStatus> = emptyList()
)

data class HabitCompletion(
    val id: Long = 0,
    val habitId: Long,
    val completedDate: Long, // Start of day in epoch millis
    val completedAt: Long = System.currentTimeMillis(),
    val notes: String? = null
)
