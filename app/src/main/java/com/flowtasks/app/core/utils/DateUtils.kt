package com.flowtasks.app.core.utils

import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

object DateUtils {

    fun formatRelativeDueDate(dueDateEpoch: Long, dueTime: String? = null): String {
        val targetCalendar = Calendar.getInstance().apply {
            timeInMillis = dueDateEpoch
        }

        val todayCalendar = Calendar.getInstance()
        val tomorrowCalendar = Calendar.getInstance().apply {
            add(Calendar.DAY_OF_YEAR, 1)
        }
        val yesterdayCalendar = Calendar.getInstance().apply {
            add(Calendar.DAY_OF_YEAR, -1)
        }

        val dateString = when {
            isSameDay(targetCalendar, todayCalendar) -> "Today"
            isSameDay(targetCalendar, tomorrowCalendar) -> "Tomorrow"
            isSameDay(targetCalendar, yesterdayCalendar) -> "Yesterday"
            targetCalendar.get(Calendar.YEAR) == todayCalendar.get(Calendar.YEAR) -> {
                SimpleDateFormat("MMM d", Locale.getDefault()).format(Date(dueDateEpoch))
            }
            else -> {
                SimpleDateFormat("MMM d, yyyy", Locale.getDefault()).format(Date(dueDateEpoch))
            }
        }

        return if (!dueTime.isNullOrBlank()) {
            "$dateString at $dueTime"
        } else {
            dateString
        }
    }

    fun formatDate(epochMillis: Long): String {
        return SimpleDateFormat("EEE, MMM d, yyyy", Locale.getDefault()).format(Date(epochMillis))
    }

    fun formatShortDate(epochMillis: Long): String {
        return SimpleDateFormat("MMM d", Locale.getDefault()).format(Date(epochMillis))
    }

    fun isSameDay(cal1: Calendar, cal2: Calendar): Boolean {
        return cal1.get(Calendar.YEAR) == cal2.get(Calendar.YEAR) &&
                cal1.get(Calendar.DAY_OF_YEAR) == cal2.get(Calendar.DAY_OF_YEAR)
    }

    fun getTodayStartEpoch(): Long {
        return Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }.timeInMillis
    }

    fun getTomorrowStartEpoch(): Long {
        return Calendar.getInstance().apply {
            add(Calendar.DAY_OF_YEAR, 1)
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }.timeInMillis
    }

    fun utcDateMillisToLocalStartOfDay(utcMillis: Long): Long {
        val utcCal = Calendar.getInstance(java.util.TimeZone.getTimeZone("UTC")).apply {
            timeInMillis = utcMillis
        }
        val year = utcCal.get(Calendar.YEAR)
        val month = utcCal.get(Calendar.MONTH)
        val day = utcCal.get(Calendar.DAY_OF_MONTH)

        return Calendar.getInstance().apply {
            set(Calendar.YEAR, year)
            set(Calendar.MONTH, month)
            set(Calendar.DAY_OF_MONTH, day)
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }.timeInMillis
    }

    fun localDateMillisToUtcMidnight(localMillis: Long): Long {
        val localCal = Calendar.getInstance().apply {
            timeInMillis = localMillis
        }
        val year = localCal.get(Calendar.YEAR)
        val month = localCal.get(Calendar.MONTH)
        val day = localCal.get(Calendar.DAY_OF_MONTH)

        return Calendar.getInstance(java.util.TimeZone.getTimeZone("UTC")).apply {
            set(Calendar.YEAR, year)
            set(Calendar.MONTH, month)
            set(Calendar.DAY_OF_MONTH, day)
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }.timeInMillis
    }
}
