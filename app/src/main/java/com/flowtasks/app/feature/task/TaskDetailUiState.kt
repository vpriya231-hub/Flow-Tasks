package com.flowtasks.app.feature.task

import com.flowtasks.app.domain.model.RecurrenceRule
import com.flowtasks.app.domain.model.ReminderType
import com.flowtasks.app.domain.model.Task
import com.flowtasks.app.domain.model.TaskList
import com.flowtasks.app.domain.model.TaskPriority

data class TaskDetailUiState(
    val isLoading: Boolean = true,
    val taskId: Long? = null,
    val title: String = "",
    val description: String = "",
    val notes: String = "",
    val isCompleted: Boolean = false,
    val priority: TaskPriority = TaskPriority.NONE,
    val dueDate: Long? = null,
    val dueTime: String? = null,
    val isStarred: Boolean = false,
    val selectedListId: Long? = null,
    val projectId: Long? = null,
    val projectTitle: String? = null,
    val goalId: Long? = null,
    val goalTitle: String? = null,
    val parentTaskId: Long? = null,
    val recurrence: RecurrenceRule = RecurrenceRule(),
    val reminderType: ReminderType = ReminderType.NONE,
    val reminderTime: Long? = null,
    val estimatedDurationMinutes: Int? = null,
    val actualDurationMinutes: Int = 0,
    val taskLists: List<TaskList> = emptyList(),
    val subtasks: List<Task> = emptyList(),
    val isSaved: Boolean = false,
    val isDeleted: Boolean = false,
    val errorMessage: String? = null,
    val isAILoading: Boolean = false,
    val aiActionMessage: String? = null,
    val aiSuccessMessage: String? = null,
    val suggestedSubtasks: List<String> = emptyList(),
    val showSubtasksDialog: Boolean = false,
    val isAIEnabled: Boolean = false
) {
    val completedSubtasksCount: Int
        get() = subtasks.count { it.isCompleted }

    val totalSubtasksCount: Int
        get() = subtasks.size
}
