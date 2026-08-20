package com.flowtasks.app.domain.model

import java.util.Calendar

data class Task(
    val id: Long = 0,
    val listId: Long? = null,
    val parentTaskId: Long? = null,
    val projectId: Long? = null,
    val projectTitle: String? = null,
    val goalId: Long? = null,
    val goalTitle: String? = null,
    val title: String,
    val description: String = "",
    val notes: String = "",
    val isCompleted: Boolean = false,
    val priority: TaskPriority = TaskPriority.NONE,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis(),
    val dueDate: Long? = null,
    val dueTime: String? = null,
    val completedAt: Long? = null,
    val sortOrder: Int = 0,
    val isStarred: Boolean = false,
    val recurrence: RecurrenceRule = RecurrenceRule(),
    val reminderType: ReminderType = ReminderType.NONE,
    val reminderTime: Long? = null,
    val estimatedDurationMinutes: Int? = null,
    val actualDurationMinutes: Int? = null,
    val subtaskCount: Int = 0,
    val completedSubtaskCount: Int = 0
) {
    val isSubtask: Boolean
        get() = parentTaskId != null

    fun getTargetDueTimeMillis(): Long? {
        if (dueDate == null) return null
        if (dueTime.isNullOrBlank()) {
            val cal = Calendar.getInstance().apply {
                timeInMillis = dueDate
                set(Calendar.HOUR_OF_DAY, 23)
                set(Calendar.MINUTE, 59)
                set(Calendar.SECOND, 59)
                set(Calendar.MILLISECOND, 999)
            }
            return cal.timeInMillis
        }
        return try {
            val parts = dueTime.split(":")
            val hour = parts[0].toInt()
            val minute = parts[1].toInt()
            val cal = Calendar.getInstance().apply {
                timeInMillis = dueDate
                set(Calendar.HOUR_OF_DAY, hour)
                set(Calendar.MINUTE, minute)
                set(Calendar.SECOND, 0)
                set(Calendar.MILLISECOND, 0)
            }
            cal.timeInMillis
        } catch (e: Exception) {
            dueDate
        }
    }

    fun isOverdue(currentTimeMillis: Long = System.currentTimeMillis()): Boolean {
        if (isCompleted || dueDate == null) return false
        val targetMillis = getTargetDueTimeMillis() ?: return false
        return targetMillis < currentTimeMillis
    }

    fun calculateReminderTriggerTime(): Long? {
        if (reminderType == ReminderType.NONE && reminderTime == null) return null
        if (reminderType == ReminderType.CUSTOM && reminderTime != null) {
            return reminderTime
        }
        val targetDue = getTargetDueTimeMillis() ?: return null
        return when (reminderType) {
            ReminderType.NONE -> null
            ReminderType.AT_TIME -> targetDue
            ReminderType.MINUTES_10_BEFORE -> targetDue - (10 * 60 * 1000L)
            ReminderType.MINUTES_30_BEFORE -> targetDue - (30 * 60 * 1000L)
            ReminderType.HOUR_1_BEFORE -> targetDue - (60 * 60 * 1000L)
            ReminderType.CUSTOM -> reminderTime ?: targetDue
        }
    }
}
