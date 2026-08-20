package com.flowtasks.app.domain.model

enum class ReminderType(val label: String, val offsetMinutes: Long) {
    NONE("None", 0L),
    AT_TIME("At time of event", 0L),
    MINUTES_10_BEFORE("10 minutes before", 10L),
    MINUTES_30_BEFORE("30 minutes before", 30L),
    HOUR_1_BEFORE("1 hour before", 60L),
    CUSTOM("Custom", 0L);

    companion object {
        fun fromString(value: String?): ReminderType {
            return entries.firstOrNull { it.name.equals(value, ignoreCase = true) } ?: NONE
        }
    }
}
