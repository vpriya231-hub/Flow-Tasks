package com.flowtasks.app.domain.model

enum class RecurrenceType {
    NONE,
    DAILY,
    WEEKLY,
    MONTHLY,
    CUSTOM;

    companion object {
        fun fromString(value: String?): RecurrenceType {
            return entries.firstOrNull { it.name.equals(value, ignoreCase = true) } ?: NONE
        }
    }
}

data class RecurrenceRule(
    val type: RecurrenceType = RecurrenceType.NONE,
    val interval: Int = 1,
    val daysOfWeek: List<Int> = emptyList() // 1 = Monday, ..., 7 = Sunday
) {
    val isRecurring: Boolean
        get() = type != RecurrenceType.NONE

    fun serializeDaysOfWeek(): String {
        return daysOfWeek.joinToString(",")
    }

    companion object {
        fun parseDaysOfWeek(serialized: String?): List<Int> {
            if (serialized.isNullOrBlank()) return emptyList()
            return serialized.split(",").mapNotNull { it.trim().toIntOrNull() }
        }
    }
}
