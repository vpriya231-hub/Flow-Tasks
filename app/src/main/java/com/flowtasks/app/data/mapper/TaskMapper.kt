package com.flowtasks.app.data.mapper

import com.flowtasks.app.data.local.entity.TaskEntity
import com.flowtasks.app.data.local.entity.TaskListEntity
import com.flowtasks.app.domain.model.RecurrenceRule
import com.flowtasks.app.domain.model.RecurrenceType
import com.flowtasks.app.domain.model.ReminderType
import com.flowtasks.app.domain.model.Task
import com.flowtasks.app.domain.model.TaskList

object TaskMapper {
    fun toDomain(
        entity: TaskEntity,
        subtaskCount: Int = 0,
        completedSubtaskCount: Int = 0,
        projectTitle: String? = null,
        goalTitle: String? = null
    ): Task {
        return Task(
            id = entity.id,
            listId = entity.listId,
            parentTaskId = entity.parentTaskId,
            projectId = entity.projectId,
            projectTitle = projectTitle,
            goalId = entity.goalId,
            goalTitle = goalTitle,
            title = entity.title,
            description = entity.description,
            notes = entity.notes,
            isCompleted = entity.isCompleted,
            priority = entity.priority,
            createdAt = entity.createdAt,
            updatedAt = entity.updatedAt,
            dueDate = entity.dueDate,
            dueTime = entity.dueTime,
            completedAt = entity.completedAt,
            sortOrder = entity.sortOrder,
            isStarred = entity.isStarred,
            recurrence = RecurrenceRule(
                type = RecurrenceType.fromString(entity.recurrenceType),
                interval = if (entity.recurrenceInterval <= 0) 1 else entity.recurrenceInterval,
                daysOfWeek = RecurrenceRule.parseDaysOfWeek(entity.recurrenceDaysOfWeek)
            ),
            reminderType = ReminderType.fromString(entity.reminderType),
            reminderTime = entity.reminderTime,
            estimatedDurationMinutes = entity.estimatedDurationMinutes,
            actualDurationMinutes = entity.actualDurationMinutes,
            subtaskCount = subtaskCount,
            completedSubtaskCount = completedSubtaskCount
        )
    }

    fun toEntity(domain: Task): TaskEntity {
        return TaskEntity(
            id = domain.id,
            listId = domain.listId,
            projectId = domain.projectId,
            goalId = domain.goalId,
            parentTaskId = domain.parentTaskId,
            title = domain.title,
            description = domain.description,
            notes = domain.notes,
            isCompleted = domain.isCompleted,
            priority = domain.priority,
            createdAt = domain.createdAt,
            updatedAt = domain.updatedAt,
            dueDate = domain.dueDate,
            dueTime = domain.dueTime,
            completedAt = domain.completedAt,
            sortOrder = domain.sortOrder,
            isStarred = domain.isStarred,
            recurrenceType = domain.recurrence.type.name,
            recurrenceInterval = domain.recurrence.interval,
            recurrenceDaysOfWeek = domain.recurrence.serializeDaysOfWeek(),
            reminderType = domain.reminderType.name,
            reminderTime = domain.reminderTime,
            estimatedDurationMinutes = domain.estimatedDurationMinutes,
            actualDurationMinutes = domain.actualDurationMinutes
        )
    }

    fun toDomain(entity: TaskListEntity, taskCount: Int = 0): TaskList {
        return TaskList(
            id = entity.id,
            name = entity.name,
            colorHex = entity.colorHex,
            iconName = entity.iconName,
            createdAt = entity.createdAt,
            updatedAt = entity.updatedAt,
            sortOrder = entity.sortOrder,
            taskCount = taskCount
        )
    }

    fun toEntity(domain: TaskList): TaskListEntity {
        return TaskListEntity(
            id = domain.id,
            name = domain.name,
            colorHex = domain.colorHex,
            iconName = domain.iconName,
            createdAt = domain.createdAt,
            updatedAt = domain.updatedAt,
            sortOrder = domain.sortOrder
        )
    }
}
