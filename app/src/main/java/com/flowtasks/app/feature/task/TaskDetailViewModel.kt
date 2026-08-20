package com.flowtasks.app.feature.task

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.flowtasks.app.core.notification.TaskReminderScheduler
import com.flowtasks.app.domain.ai.AIError
import com.flowtasks.app.domain.ai.AIResult
import com.flowtasks.app.domain.model.RecurrenceRule
import com.flowtasks.app.domain.model.ReminderType
import com.flowtasks.app.domain.model.Task
import com.flowtasks.app.domain.model.TaskPriority
import com.flowtasks.app.domain.usecase.CreateTaskUseCase
import com.flowtasks.app.domain.usecase.DeleteTaskUseCase
import com.flowtasks.app.domain.usecase.GetSubtasksUseCase
import com.flowtasks.app.domain.usecase.GetTaskByIdUseCase
import com.flowtasks.app.domain.usecase.GetTaskListsUseCase
import com.flowtasks.app.domain.usecase.ToggleTaskCompletionUseCase
import com.flowtasks.app.domain.usecase.UpdateTaskUseCase
import com.flowtasks.app.domain.usecase.ai.AITaskAssistantUseCase
import com.flowtasks.app.domain.usecase.ai.GetAIConfigUseCase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class TaskDetailViewModel(
    private val taskId: Long?,
    private val initialListId: Long? = null,
    private val getTaskByIdUseCase: GetTaskByIdUseCase,
    private val getSubtasksUseCase: GetSubtasksUseCase,
    private val getTaskListsUseCase: GetTaskListsUseCase,
    private val createTaskUseCase: CreateTaskUseCase,
    private val updateTaskUseCase: UpdateTaskUseCase,
    private val deleteTaskUseCase: DeleteTaskUseCase,
    private val toggleTaskCompletionUseCase: ToggleTaskCompletionUseCase,
    private val reminderScheduler: TaskReminderScheduler? = null,
    private val aiTaskAssistantUseCase: AITaskAssistantUseCase? = null,
    private val getAIConfigUseCase: GetAIConfigUseCase? = null
) : ViewModel() {

    private val _uiState = MutableStateFlow(
        TaskDetailUiState(
            taskId = taskId,
            selectedListId = if (taskId == null || taskId <= 0) initialListId else null,
            isLoading = taskId != null && taskId > 0
        )
    )
    val uiState: StateFlow<TaskDetailUiState> = _uiState.asStateFlow()

    init {
        loadTaskLists()
        observeAIConfig()
        if (taskId != null && taskId > 0) {
            loadTask(taskId)
        }
    }

    private fun observeAIConfig() {
        if (getAIConfigUseCase != null) {
            viewModelScope.launch {
                getAIConfigUseCase().collect { config ->
                    _uiState.update { it.copy(isAIEnabled = config.isEnabled) }
                }
            }
        }
    }

    private fun loadTaskLists() {
        viewModelScope.launch {
            getTaskListsUseCase().collect { lists ->
                _uiState.update { it.copy(taskLists = lists) }
            }
        }
    }

    private fun loadTask(id: Long) {
        viewModelScope.launch {
            combine(
                getTaskByIdUseCase(id),
                getSubtasksUseCase(id)
            ) { task, subtasks ->
                Pair(task, subtasks)
            }.collect { (task, subtasks) ->
                if (task != null) {
                    _uiState.update { state ->
                        state.copy(
                            isLoading = false,
                            title = task.title,
                            description = task.description,
                            notes = task.notes,
                            isCompleted = task.isCompleted,
                            priority = task.priority,
                            dueDate = task.dueDate,
                            dueTime = task.dueTime,
                            isStarred = task.isStarred,
                            selectedListId = task.listId,
                            projectId = task.projectId,
                            projectTitle = task.projectTitle,
                            goalId = task.goalId,
                            goalTitle = task.goalTitle,
                            parentTaskId = task.parentTaskId,
                            recurrence = task.recurrence,
                            reminderType = task.reminderType,
                            reminderTime = task.reminderTime,
                            estimatedDurationMinutes = task.estimatedDurationMinutes,
                            actualDurationMinutes = task.actualDurationMinutes ?: 0,
                            subtasks = subtasks
                        )
                    }
                } else {
                    _uiState.update { it.copy(isLoading = false) }
                }
            }
        }
    }

    fun updateTitle(title: String) {
        _uiState.update { it.copy(title = title) }
    }

    fun updateDescription(description: String) {
        _uiState.update { it.copy(description = description) }
    }

    fun updateNotes(notes: String) {
        _uiState.update { it.copy(notes = notes) }
    }

    fun updatePriority(priority: TaskPriority) {
        _uiState.update { it.copy(priority = priority) }
    }

    fun updateDueDate(dueDate: Long?) {
        _uiState.update { state ->
            if (dueDate == null) {
                state.copy(dueDate = null, dueTime = null, reminderType = ReminderType.NONE, reminderTime = null)
            } else {
                state.copy(dueDate = dueDate)
            }
        }
    }

    fun updateDueTime(dueTime: String?) {
        _uiState.update { it.copy(dueTime = dueTime) }
    }

    fun updateSelectedList(listId: Long?) {
        _uiState.update { it.copy(selectedListId = listId) }
    }

    fun updateRecurrence(recurrence: RecurrenceRule) {
        _uiState.update { it.copy(recurrence = recurrence) }
    }

    fun updateReminderType(reminderType: ReminderType, customTime: Long? = null) {
        _uiState.update { it.copy(reminderType = reminderType, reminderTime = customTime) }
    }

    fun updateEstimatedDuration(minutes: Int?) {
        _uiState.update { it.copy(estimatedDurationMinutes = minutes) }
    }

    fun toggleStarred() {
        _uiState.update { it.copy(isStarred = !it.isStarred) }
    }

    fun toggleCompletion() {
        val current = _uiState.value.isCompleted
        _uiState.update { it.copy(isCompleted = !current) }
    }

    fun addSubtask(title: String) {
        if (title.isBlank()) return
        val currentTaskId = _uiState.value.taskId ?: return
        viewModelScope.launch {
            try {
                val subtask = Task(
                    title = title.trim(),
                    parentTaskId = currentTaskId
                )
                createTaskUseCase(subtask)
            } catch (e: Exception) {
                _uiState.update { it.copy(errorMessage = "Failed to add subtask: ${e.localizedMessage}") }
            }
        }
    }

    fun toggleSubtaskCompletion(subtaskId: Long, isCompleted: Boolean) {
        viewModelScope.launch {
            try {
                toggleTaskCompletionUseCase(subtaskId, isCompleted)
            } catch (e: Exception) {
                _uiState.update { it.copy(errorMessage = "Failed to update subtask: ${e.localizedMessage}") }
            }
        }
    }

    fun deleteSubtask(subtaskId: Long) {
        viewModelScope.launch {
            try {
                deleteTaskUseCase(subtaskId)
            } catch (e: Exception) {
                _uiState.update { it.copy(errorMessage = "Failed to delete subtask: ${e.localizedMessage}") }
            }
        }
    }

    // AI Assistant Actions
    fun generateTaskFromPrompt(prompt: String) {
        if (prompt.isBlank() || aiTaskAssistantUseCase == null) return
        viewModelScope.launch {
            _uiState.update { it.copy(isAILoading = true, aiActionMessage = "Drafting task with AI...") }
            when (val result = aiTaskAssistantUseCase.generateTaskFromPrompt(prompt)) {
                is AIResult.Success -> {
                    val suggestion = result.data
                    _uiState.update { state ->
                        state.copy(
                            isAILoading = false,
                            aiActionMessage = null,
                            aiSuccessMessage = "Task proposal generated. Review and adjust below before saving.",
                            title = suggestion.title,
                            description = if (suggestion.description.isNotBlank()) suggestion.description else state.description,
                            priority = if (suggestion.priority != TaskPriority.NONE) suggestion.priority else state.priority,
                            estimatedDurationMinutes = suggestion.estimatedMinutes ?: state.estimatedDurationMinutes
                        )
                    }
                }
                is AIResult.Error -> {
                    _uiState.update { it.copy(isAILoading = false, aiActionMessage = null, errorMessage = formatAIError(result.error)) }
                }
                else -> {}
            }
        }
    }

    fun improveTaskWithAI() {
        val state = _uiState.value
        if (state.title.isBlank() || aiTaskAssistantUseCase == null) {
            _uiState.update { it.copy(errorMessage = "Enter a task title first to improve it.") }
            return
        }
        viewModelScope.launch {
            _uiState.update { it.copy(isAILoading = true, aiActionMessage = "Refining task details...") }
            when (val result = aiTaskAssistantUseCase.improveTask(state.title, state.description, state.notes)) {
                is AIResult.Success -> {
                    val suggestion = result.data
                    _uiState.update {
                        it.copy(
                            isAILoading = false,
                            aiActionMessage = null,
                            aiSuccessMessage = "Task refined. Review and adjust below before saving.",
                            title = suggestion.title,
                            description = suggestion.description,
                            notes = if (suggestion.suggestedNotes.isNotBlank()) suggestion.suggestedNotes else it.notes
                        )
                    }
                }
                is AIResult.Error -> {
                    _uiState.update { it.copy(isAILoading = false, aiActionMessage = null, errorMessage = formatAIError(result.error)) }
                }
                else -> {}
            }
        }
    }

    fun generateSubtasksWithAI() {
        val state = _uiState.value
        if (state.title.isBlank() || aiTaskAssistantUseCase == null) {
            _uiState.update { it.copy(errorMessage = "Enter a task title first to generate subtasks.") }
            return
        }
        viewModelScope.launch {
            _uiState.update { it.copy(isAILoading = true, aiActionMessage = "Generating subtasks...") }
            when (val result = aiTaskAssistantUseCase.generateSubtasks(state.title, state.description)) {
                is AIResult.Success -> {
                    _uiState.update {
                        it.copy(
                            isAILoading = false,
                            aiActionMessage = null,
                            suggestedSubtasks = result.data,
                            showSubtasksDialog = true
                        )
                    }
                }
                is AIResult.Error -> {
                    _uiState.update { it.copy(isAILoading = false, aiActionMessage = null, errorMessage = formatAIError(result.error)) }
                }
                else -> {}
            }
        }
    }

    fun suggestPriorityAndDurationWithAI() {
        val state = _uiState.value
        if (state.title.isBlank() || aiTaskAssistantUseCase == null) {
            _uiState.update { it.copy(errorMessage = "Enter a task title first to evaluate priority.") }
            return
        }
        viewModelScope.launch {
            _uiState.update { it.copy(isAILoading = true, aiActionMessage = "Evaluating priority and effort...") }
            when (val result = aiTaskAssistantUseCase.suggestPriorityAndDuration(state.title, state.description, state.dueDate)) {
                is AIResult.Success -> {
                    val suggestion = result.data
                    _uiState.update {
                        it.copy(
                            isAILoading = false,
                            aiActionMessage = null,
                            priority = suggestion.priority,
                            estimatedDurationMinutes = suggestion.estimatedMinutes ?: it.estimatedDurationMinutes,
                            aiSuccessMessage = "Suggested ${suggestion.priority.name} (${suggestion.estimatedMinutes}m): ${suggestion.reasoning}"
                        )
                    }
                }
                is AIResult.Error -> {
                    _uiState.update { it.copy(isAILoading = false, aiActionMessage = null, errorMessage = formatAIError(result.error)) }
                }
                else -> {}
            }
        }
    }

    fun applySuggestedSubtask(subtaskTitle: String) {
        addSubtask(subtaskTitle)
        _uiState.update {
            it.copy(suggestedSubtasks = it.suggestedSubtasks.filter { title -> title != subtaskTitle })
        }
    }

    fun applyAllSuggestedSubtasks() {
        val currentTaskId = _uiState.value.taskId
        val suggestions = _uiState.value.suggestedSubtasks
        if (suggestions.isEmpty()) return

        if (currentTaskId != null && currentTaskId > 0) {
            viewModelScope.launch {
                suggestions.forEach { title ->
                    createTaskUseCase(Task(title = title, parentTaskId = currentTaskId))
                }
                _uiState.update { it.copy(showSubtasksDialog = false, suggestedSubtasks = emptyList(), aiSuccessMessage = "Added ${suggestions.size} subtasks.") }
            }
        } else {
            // If task is not saved yet, append subtasks to description checklist
            val formatted = suggestions.joinToString("\n") { "- [ ] $it" }
            val currentDesc = _uiState.value.description
            val newDesc = if (currentDesc.isBlank()) formatted else "$currentDesc\n\nSubtasks:\n$formatted"
            _uiState.update { it.copy(description = newDesc, showSubtasksDialog = false, suggestedSubtasks = emptyList(), aiSuccessMessage = "Appended subtasks to description.") }
        }
    }

    fun dismissSubtasksDialog() {
        _uiState.update { it.copy(showSubtasksDialog = false) }
    }

    fun dismissAIMessage() {
        _uiState.update { it.copy(aiSuccessMessage = null) }
    }

    private fun formatAIError(error: AIError): String {
        return when (error) {
            is AIError.MissingApiKey -> "API key not configured. Open Settings to enter your Gemini API Key."
            is AIError.InvalidApiKey -> "Invalid API key. Please check your API key in Settings."
            is AIError.ProviderDisabled -> "AI features are disabled. Turn them on in Settings."
            is AIError.RateLimitExceeded -> "Rate limit reached. Please try again in a few moments."
            is AIError.NetworkUnavailable -> "Network unreachable: ${error.message}"
            is AIError.Timeout -> "Request timed out. Please try again."
            else -> "AI Assistant error: ${error.message}"
        }
    }

    fun saveTask() {
        val state = _uiState.value
        if (state.title.isBlank()) {
            _uiState.update { it.copy(errorMessage = "Task title cannot be blank") }
            return
        }

        viewModelScope.launch {
            try {
                if (state.taskId == null || state.taskId <= 0) {
                    val newTask = Task(
                        title = state.title.trim(),
                        description = state.description.trim(),
                        notes = state.notes.trim(),
                        isCompleted = state.isCompleted,
                        priority = state.priority,
                        dueDate = state.dueDate,
                        dueTime = state.dueTime,
                        isStarred = state.isStarred,
                        listId = state.selectedListId,
                        recurrence = state.recurrence,
                        reminderType = state.reminderType,
                        reminderTime = state.reminderTime,
                        estimatedDurationMinutes = state.estimatedDurationMinutes,
                        actualDurationMinutes = state.actualDurationMinutes
                    )
                    val newId = createTaskUseCase(newTask)
                    reminderScheduler?.scheduleReminder(newTask.copy(id = newId))
                } else {
                    val updatedTask = Task(
                        id = state.taskId,
                        title = state.title.trim(),
                        description = state.description.trim(),
                        notes = state.notes.trim(),
                        isCompleted = state.isCompleted,
                        priority = state.priority,
                        dueDate = state.dueDate,
                        dueTime = state.dueTime,
                        isStarred = state.isStarred,
                        listId = state.selectedListId,
                        projectId = state.projectId,
                        projectTitle = state.projectTitle,
                        goalId = state.goalId,
                        goalTitle = state.goalTitle,
                        parentTaskId = state.parentTaskId,
                        recurrence = state.recurrence,
                        reminderType = state.reminderType,
                        reminderTime = state.reminderTime,
                        estimatedDurationMinutes = state.estimatedDurationMinutes,
                        actualDurationMinutes = state.actualDurationMinutes
                    )
                    updateTaskUseCase(updatedTask)
                    if (updatedTask.isCompleted) {
                        reminderScheduler?.cancelReminder(updatedTask.id)
                    } else {
                        reminderScheduler?.scheduleReminder(updatedTask)
                    }
                }
                _uiState.update { it.copy(isSaved = true) }
            } catch (e: Exception) {
                _uiState.update { it.copy(errorMessage = "Failed to save task: ${e.localizedMessage}") }
            }
        }
    }

    fun deleteTask() {
        val currentTaskId = _uiState.value.taskId ?: return
        viewModelScope.launch {
            try {
                reminderScheduler?.cancelReminder(currentTaskId)
                deleteTaskUseCase(currentTaskId)
                _uiState.update { it.copy(isDeleted = true) }
            } catch (e: Exception) {
                _uiState.update { it.copy(errorMessage = "Failed to delete task: ${e.localizedMessage}") }
            }
        }
    }

    fun dismissError() {
        _uiState.update { it.copy(errorMessage = null) }
    }

    class Factory(
        private val taskId: Long?,
        private val initialListId: Long? = null,
        private val getTaskByIdUseCase: GetTaskByIdUseCase,
        private val getSubtasksUseCase: GetSubtasksUseCase,
        private val getTaskListsUseCase: GetTaskListsUseCase,
        private val createTaskUseCase: CreateTaskUseCase,
        private val updateTaskUseCase: UpdateTaskUseCase,
        private val deleteTaskUseCase: DeleteTaskUseCase,
        private val toggleTaskCompletionUseCase: ToggleTaskCompletionUseCase,
        private val reminderScheduler: TaskReminderScheduler? = null,
        private val aiTaskAssistantUseCase: AITaskAssistantUseCase? = null,
        private val getAIConfigUseCase: GetAIConfigUseCase? = null
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return TaskDetailViewModel(
                taskId,
                initialListId,
                getTaskByIdUseCase,
                getSubtasksUseCase,
                getTaskListsUseCase,
                createTaskUseCase,
                updateTaskUseCase,
                deleteTaskUseCase,
                toggleTaskCompletionUseCase,
                reminderScheduler,
                aiTaskAssistantUseCase,
                getAIConfigUseCase
            ) as T
        }
    }
}

