package com.flowtasks.app.domain.model

data class DailyProductivityPoint(
    val dayOfWeek: Int, // 1=Mon .. 7=Sun
    val dayLabel: String,
    val dateMillis: Long,
    val tasksCompleted: Int,
    val focusMinutes: Int,
    val isToday: Boolean
)

data class ProductivityStats(
    // Today
    val tasksDueToday: Int = 0,
    val completedToday: Int = 0,
    val remainingToday: Int = 0,
    val overdueTasksCount: Int = 0,

    // This Week
    val completedThisWeek: Int = 0,
    val totalTasksThisWeek: Int = 0,
    val completionRatePercentage: Int = 0,
    val totalFocusSecondsThisWeek: Int = 0,
    val totalFocusSecondsAllTime: Int = 0,
    val totalFocusSessionsCompleted: Int = 0,

    // All Time
    val totalTasksCount: Int = 0,
    val totalCompletedTasksCount: Int = 0,

    // Habits
    val totalHabitsCount: Int = 0,
    val habitsCompletedTodayCount: Int = 0,
    val longestHabitStreak: Int = 0,

    // Goals & Projects
    val activeGoalsCount: Int = 0,
    val completedGoalsCount: Int = 0,
    val totalGoalsCount: Int = 0,
    val goalsCompletionRatePercentage: Int = 0,
    val activeProjectsCount: Int = 0,
    val completedProjectsCount: Int = 0,
    val totalProjectsCount: Int = 0,
    val projectsCompletionRatePercentage: Int = 0,

    // 7-day Weekly Trend (Mon..Sun)
    val weeklyTrend: List<DailyProductivityPoint> = emptyList(),

    // Flag for empty dashboard
    val hasAnyData: Boolean = false
) {
    val formattedWeeklyFocusTime: String
        get() = formatSeconds(totalFocusSecondsThisWeek)

    val formattedAllTimeFocusTime: String
        get() = formatSeconds(totalFocusSecondsAllTime)

    private fun formatSeconds(totalSeconds: Int): String {
        val totalMinutes = totalSeconds / 60
        val hours = totalMinutes / 60
        val minutes = totalMinutes % 60
        return when {
            hours > 0 && minutes > 0 -> "${hours}h ${minutes}m"
            hours > 0 -> "${hours}h"
            minutes > 0 -> "${minutes}m"
            else -> "0m"
        }
    }
}
