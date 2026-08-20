package com.flowtasks.app.domain.usecase

import com.flowtasks.app.core.common.RecurrenceCalculator
import com.flowtasks.app.core.notification.TaskReminderScheduler
import com.flowtasks.app.domain.model.Task
import com.flowtasks.app.domain.model.TaskFilter
import com.flowtasks.app.domain.model.TaskSortOrder
import com.flowtasks.app.domain.repository.TaskRepository
import kotlinx.coroutines.flow.Flow

class GetTasksUseCase(private val repository: TaskRepository) {
    operator fun invoke(
        filter: TaskFilter = TaskFilter.All,
        sortOrder: TaskSortOrder = TaskSortOrder.DEFAULT_SORT_ORDER
    ): Flow<List<Task>> = repository.getTasks(filter, sortOrder)
}

class GetTaskByIdUseCase(private val repository: TaskRepository) {
    operator fun invoke(id: Long): Flow<Task?> = repository.getTaskById(id)
    suspend fun getDirect(id: Long): Task? = repository.getTaskByIdDirect(id)
}

class GetSubtasksUseCase(private val repository: TaskRepository) {
    operator fun invoke(parentId: Long): Flow<List<Task>> = repository.getSubtasks(parentId)
    suspend fun getDirect(parentId: Long): List<Task> = repository.getSubtasksDirect(parentId)
}

class CreateTaskUseCase(
    private val repository: TaskRepository,
    private val reminderScheduler: TaskReminderScheduler
) {
    suspend operator fun invoke(task: Task): Long {
        require(task.title.isNotBlank()) { "Task title cannot be blank" }
        val generatedId = repository.createTask(task.copy(title = task.title.trim()))
        val createdTask = task.copy(id = generatedId, title = task.title.trim())
        reminderScheduler.scheduleReminder(createdTask)
        return generatedId
    }
}

class UpdateTaskUseCase(
    private val repository: TaskRepository,
    private val reminderScheduler: TaskReminderScheduler
) {
    suspend operator fun invoke(task: Task) {
        require(task.title.isNotBlank()) { "Task title cannot be blank" }
        val updatedTask = task.copy(title = task.title.trim())
        repository.updateTask(updatedTask)
        if (updatedTask.isCompleted) {
            reminderScheduler.cancelReminder(updatedTask.id)
        } else {
            reminderScheduler.scheduleReminder(updatedTask)
        }
    }
}

class DeleteTaskUseCase(
    private val repository: TaskRepository,
    private val reminderScheduler: TaskReminderScheduler
) {
    suspend operator fun invoke(id: Long) {
        reminderScheduler.cancelReminder(id)
        repository.deleteTask(id)
    }
}

class ToggleTaskCompletionUseCase(
    private val repository: TaskRepository,
    private val reminderScheduler: TaskReminderScheduler
) {
    suspend operator fun invoke(id: Long, isCompleted: Boolean) {
        val existing = repository.getTaskByIdDirect(id)
        if (existing != null && isCompleted && existing.recurrence.isRecurring) {
            // For recurring tasks, calculate next occurrence due date
            val nextDueDate = RecurrenceCalculator.calculateNextDueDate(
                currentDueDate = existing.dueDate,
                recurrence = existing.recurrence
            )
            val updatedTask = existing.copy(
                dueDate = nextDueDate,
                isCompleted = false,
                completedAt = null,
                updatedAt = System.currentTimeMillis()
            )
            repository.updateTask(updatedTask)
            reminderScheduler.scheduleReminder(updatedTask)
        } else {
            repository.toggleTaskCompletion(id, isCompleted)
            if (isCompleted) {
                reminderScheduler.cancelReminder(id)
            } else if (existing != null) {
                reminderScheduler.scheduleReminder(existing.copy(isCompleted = false))
            }
        }
    }
}

class ToggleTaskStarUseCase(private val repository: TaskRepository) {
    suspend operator fun invoke(id: Long, isStarred: Boolean) {
        repository.toggleTaskStar(id, isStarred)
    }
}

class SearchTasksUseCase(private val repository: TaskRepository) {
    operator fun invoke(query: String): Flow<List<Task>> = repository.searchTasks(query.trim())
}

class ReorderTasksUseCase(private val repository: TaskRepository) {
    suspend operator fun invoke(taskIds: List<Long>) {
        repository.reorderTasks(taskIds)
    }
}

class MoveTasksToInboxUseCase(private val repository: TaskRepository) {
    suspend operator fun invoke(listId: Long) {
        repository.moveTasksToInbox(listId)
    }
}

class DeleteTasksByListIdUseCase(private val repository: TaskRepository) {
    suspend operator fun invoke(listId: Long) {
        repository.deleteTasksByListId(listId)
    }
}
