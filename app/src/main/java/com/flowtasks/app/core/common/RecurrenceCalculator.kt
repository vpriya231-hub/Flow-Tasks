package com.flowtasks.app.core.common

import com.flowtasks.app.domain.model.RecurrenceRule
import com.flowtasks.app.domain.model.RecurrenceType
import java.util.Calendar

object RecurrenceCalculator {

    fun calculateNextDueDate(
        currentDueDate: Long?,
        recurrence: RecurrenceRule,
        fromTimeMillis: Long = System.currentTimeMillis()
    ): Long {
        val baseDate = currentDueDate ?: fromTimeMillis
        val calendar = Calendar.getInstance().apply {
            timeInMillis = baseDate
        }

        val interval = if (recurrence.interval <= 0) 1 else recurrence.interval

        when (recurrence.type) {
            RecurrenceType.NONE -> return baseDate
            RecurrenceType.DAILY -> {
                calendar.add(Calendar.DAY_OF_YEAR, interval)
                // Ensure it moves into future relative to now if it was past due
                while (calendar.timeInMillis < fromTimeMillis) {
                    calendar.add(Calendar.DAY_OF_YEAR, interval)
                }
            }
            RecurrenceType.WEEKLY -> {
                if (recurrence.daysOfWeek.isNotEmpty()) {
                    // Find next matching day of week
                    // Mapping: 1=Mon(Calendar.MONDAY), 2=Tue, ..., 7=Sun(Calendar.SUNDAY)
                    val targetCalendarDays = recurrence.daysOfWeek.map { toCalendarDay(it) }.sorted()
                    var daysChecked = 0
                    do {
                        calendar.add(Calendar.DAY_OF_YEAR, 1)
                        daysChecked++
                        val currentDay = calendar.get(Calendar.DAY_OF_WEEK)
                    } while (!targetCalendarDays.contains(currentDay) && daysChecked < 14)
                } else {
                    calendar.add(Calendar.WEEK_OF_YEAR, interval)
                    while (calendar.timeInMillis < fromTimeMillis) {
                        calendar.add(Calendar.WEEK_OF_YEAR, interval)
                    }
                }
            }
            RecurrenceType.MONTHLY -> {
                calendar.add(Calendar.MONTH, interval)
                while (calendar.timeInMillis < fromTimeMillis) {
                    calendar.add(Calendar.MONTH, interval)
                }
            }
            RecurrenceType.CUSTOM -> {
                calendar.add(Calendar.DAY_OF_YEAR, interval)
                while (calendar.timeInMillis < fromTimeMillis) {
                    calendar.add(Calendar.DAY_OF_YEAR, interval)
                }
            }
        }
        return calendar.timeInMillis
    }

    private fun toCalendarDay(dayOfWeek: Int): Int {
        return when (dayOfWeek) {
            1 -> Calendar.MONDAY
            2 -> Calendar.TUESDAY
            3 -> Calendar.WEDNESDAY
            4 -> Calendar.THURSDAY
            5 -> Calendar.FRIDAY
            6 -> Calendar.SATURDAY
            7 -> Calendar.SUNDAY
            else -> Calendar.MONDAY
        }
    }
}
