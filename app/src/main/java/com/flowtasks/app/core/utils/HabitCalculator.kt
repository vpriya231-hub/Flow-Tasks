package com.flowtasks.app.core.utils

import com.flowtasks.app.data.local.entity.HabitCompletionEntity
import com.flowtasks.app.data.local.entity.HabitEntity
import com.flowtasks.app.domain.model.HabitDayStatus
import com.flowtasks.app.domain.model.HabitFrequencyType
import java.util.Calendar
import java.util.Locale

data class HabitCalculatedMetrics(
    val isCompletedToday: Boolean,
    val currentStreak: Int,
    val bestStreak: Int,
    val totalCompletions: Int,
    val weeklyHistory: List<HabitDayStatus>
)

object HabitCalculator {

    fun normalizeToStartOfDay(millis: Long = System.currentTimeMillis()): Long {
        val cal = Calendar.getInstance().apply {
            timeInMillis = millis
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        return cal.timeInMillis
    }

    private fun getDayOfWeek(millis: Long): Int {
        val cal = Calendar.getInstance().apply { timeInMillis = millis }
        // Calendar.SUNDAY=1, MONDAY=2, ... SATURDAY=7
        // Convert to ISO: Mon=1, Tue=2, Wed=3, Thu=4, Fri=5, Sat=6, Sun=7
        return when (cal.get(Calendar.DAY_OF_WEEK)) {
            Calendar.MONDAY -> 1
            Calendar.TUESDAY -> 2
            Calendar.WEDNESDAY -> 3
            Calendar.THURSDAY -> 4
            Calendar.FRIDAY -> 5
            Calendar.SATURDAY -> 6
            Calendar.SUNDAY -> 7
            else -> 1
        }
    }

    private fun getDayLetter(dayOfWeekIso: Int): String {
        return when (dayOfWeekIso) {
            1 -> "M"
            2 -> "T"
            3 -> "W"
            4 -> "T"
            5 -> "F"
            6 -> "S"
            7 -> "S"
            else -> "D"
        }
    }

    fun parseDaysOfWeek(daysStr: String?): List<Int> {
        if (daysStr.isNullOrBlank()) return emptyList()
        return daysStr.split(",")
            .mapNotNull { it.trim().toIntOrNull() }
            .filter { it in 1..7 }
            .sorted()
    }

    fun serializeDaysOfWeek(days: List<Int>): String {
        return days.filter { it in 1..7 }.distinct().sorted().joinToString(",")
    }

    fun calculateMetrics(
        habit: HabitEntity,
        completions: List<HabitCompletionEntity>,
        nowMillis: Long = System.currentTimeMillis()
    ): HabitCalculatedMetrics {
        val todayStart = normalizeToStartOfDay(nowMillis)
        val completedDatesSet = completions.map { normalizeToStartOfDay(it.completedDate) }.toSet()
        val totalCompletions = completedDatesSet.size

        val frequencyType = try {
            HabitFrequencyType.valueOf(habit.frequencyType)
        } catch (e: Exception) {
            HabitFrequencyType.DAILY
        }

        val frequencyDays = parseDaysOfWeek(habit.frequencyDays)
        val isCompletedToday = completedDatesSet.contains(todayStart)

        // Calculate Weekly History (last 7 days up to today)
        val weeklyHistory = mutableListOf<HabitDayStatus>()
        val cal = Calendar.getInstance().apply {
            timeInMillis = todayStart
            add(Calendar.DAY_OF_YEAR, -6)
        }

        for (i in 0 until 7) {
            val dayMillis = cal.timeInMillis
            val isoDayOfWeek = getDayOfWeek(dayMillis)
            val isScheduled = when (frequencyType) {
                HabitFrequencyType.DAILY -> true
                HabitFrequencyType.CUSTOM_DAYS -> if (frequencyDays.isEmpty()) true else frequencyDays.contains(isoDayOfWeek)
                HabitFrequencyType.WEEKLY -> true
            }
            val isDone = completedDatesSet.contains(dayMillis)
            val isToday = dayMillis == todayStart

            weeklyHistory.add(
                HabitDayStatus(
                    dateMillis = dayMillis,
                    dayOfWeek = isoDayOfWeek,
                    dayLabel = getDayLetter(isoDayOfWeek),
                    isCompleted = isDone,
                    isScheduled = isScheduled,
                    isToday = isToday
                )
            )
            cal.add(Calendar.DAY_OF_YEAR, 1)
        }

        // Calculate Current & Best Streak
        var currentStreak = 0
        var bestStreak = 0

        when (frequencyType) {
            HabitFrequencyType.DAILY -> {
                // Current Streak
                var testCal = Calendar.getInstance().apply { timeInMillis = todayStart }
                if (!isCompletedToday) {
                    // If not completed today, start checking from yesterday
                    testCal.add(Calendar.DAY_OF_YEAR, -1)
                }

                while (completedDatesSet.contains(testCal.timeInMillis)) {
                    currentStreak++
                    testCal.add(Calendar.DAY_OF_YEAR, -1)
                }

                // Best Streak
                if (completedDatesSet.isNotEmpty()) {
                    val sortedDates = completedDatesSet.sorted()
                    var streakCounter = 0
                    var prevDate: Long? = null

                    for (date in sortedDates) {
                        if (prevDate == null) {
                            streakCounter = 1
                        } else {
                            val prevCal = Calendar.getInstance().apply {
                                timeInMillis = prevDate!!
                                add(Calendar.DAY_OF_YEAR, 1)
                            }
                            if (prevCal.timeInMillis == date) {
                                streakCounter++
                            } else {
                                streakCounter = 1
                            }
                        }
                        if (streakCounter > bestStreak) {
                            bestStreak = streakCounter
                        }
                        prevDate = date
                    }
                }
            }

            HabitFrequencyType.CUSTOM_DAYS -> {
                val scheduledDays = if (frequencyDays.isEmpty()) (1..7).toList() else frequencyDays
                val todayIso = getDayOfWeek(todayStart)
                val isTodayScheduled = scheduledDays.contains(todayIso)

                var testCal = Calendar.getInstance().apply { timeInMillis = todayStart }

                if (isTodayScheduled && !isCompletedToday) {
                    // Check previous scheduled day
                    testCal = findPreviousScheduledDay(testCal, scheduledDays)
                } else if (!isTodayScheduled) {
                    testCal = findPreviousScheduledDay(testCal, scheduledDays)
                }

                while (true) {
                    val dayStart = testCal.timeInMillis
                    if (completedDatesSet.contains(dayStart)) {
                        currentStreak++
                        testCal = findPreviousScheduledDay(testCal, scheduledDays)
                    } else {
                        break
                    }
                }

                bestStreak = maxOf(currentStreak, calculateCustomDaysBestStreak(completedDatesSet, scheduledDays))
            }

            HabitFrequencyType.WEEKLY -> {
                // Group completions by ISO year-week
                var checkCal = Calendar.getInstance().apply {
                    timeInMillis = todayStart
                    firstDayOfWeek = Calendar.MONDAY
                }
                var weekStart = getWeekStartMillis(checkCal)
                val completedWeekStarts = completedDatesSet.map {
                    val c = Calendar.getInstance().apply {
                        timeInMillis = it
                        firstDayOfWeek = Calendar.MONDAY
                    }
                    getWeekStartMillis(c)
                }.toSet()

                val isCompletedThisWeek = completedWeekStarts.contains(weekStart)
                if (!isCompletedThisWeek) {
                    checkCal.add(Calendar.WEEK_OF_YEAR, -1)
                    weekStart = getWeekStartMillis(checkCal)
                }

                while (completedWeekStarts.contains(weekStart)) {
                    currentStreak++
                    checkCal.add(Calendar.WEEK_OF_YEAR, -1)
                    weekStart = getWeekStartMillis(checkCal)
                }

                bestStreak = maxOf(currentStreak, totalCompletions)
            }
        }

        bestStreak = maxOf(bestStreak, currentStreak)

        return HabitCalculatedMetrics(
            isCompletedToday = isCompletedToday,
            currentStreak = currentStreak,
            bestStreak = bestStreak,
            totalCompletions = totalCompletions,
            weeklyHistory = weeklyHistory
        )
    }

    private fun findPreviousScheduledDay(cal: Calendar, scheduledDays: List<Int>): Calendar {
        val result = Calendar.getInstance().apply { timeInMillis = cal.timeInMillis }
        do {
            result.add(Calendar.DAY_OF_YEAR, -1)
        } while (!scheduledDays.contains(getDayOfWeek(result.timeInMillis)))
        return result
    }

    private fun getWeekStartMillis(cal: Calendar): Long {
        val c = Calendar.getInstance().apply {
            timeInMillis = cal.timeInMillis
            firstDayOfWeek = Calendar.MONDAY
            set(Calendar.DAY_OF_WEEK, Calendar.MONDAY)
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        return c.timeInMillis
    }

    private fun calculateCustomDaysBestStreak(
        completedDates: Set<Long>,
        scheduledDays: List<Int>
    ): Int {
        if (completedDates.isEmpty()) return 0
        val sorted = completedDates.sorted()
        var maxStreak = 0
        var current = 0
        var prevDate: Long? = null

        for (date in sorted) {
            val iso = getDayOfWeek(date)
            if (!scheduledDays.contains(iso)) continue

            if (prevDate == null) {
                current = 1
            } else {
                val prevCal = Calendar.getInstance().apply { timeInMillis = prevDate!! }
                val nextScheduled = findNextScheduledDay(prevCal, scheduledDays)
                if (nextScheduled.timeInMillis == date) {
                    current++
                } else {
                    current = 1
                }
            }
            if (current > maxStreak) {
                maxStreak = current
            }
            prevDate = date
        }
        return maxStreak
    }

    private fun findNextScheduledDay(cal: Calendar, scheduledDays: List<Int>): Calendar {
        val result = Calendar.getInstance().apply { timeInMillis = cal.timeInMillis }
        do {
            result.add(Calendar.DAY_OF_YEAR, 1)
        } while (!scheduledDays.contains(getDayOfWeek(result.timeInMillis)))
        return result
    }
}
