package com.flowtasks.app.feature.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.flowtasks.app.domain.model.RecurrenceRule
import com.flowtasks.app.domain.model.ReminderType
import com.flowtasks.app.domain.model.Task
import com.flowtasks.app.domain.model.TaskFilter
import com.flowtasks.app.domain.model.TaskList
import com.flowtasks.app.domain.model.TaskPriority
import com.flowtasks.app.domain.model.TaskSortOrder
import com.flowtasks.app.domain.usecase.CreateTaskListUseCase
import com.flowtasks.app.domain.usecase.CreateTaskUseCase
import com.flowtasks.app.domain.usecase.DeleteTaskListUseCase
import com.flowtasks.app.domain.usecase.DeleteListStrategy
import com.flowtasks.app.domain.usecase.DeleteTaskUseCase
import com.flowtasks.app.domain.usecase.GetSettingsUseCase
import com.flowtasks.app.domain.usecase.GetTaskListsUseCase
import com.flowtasks.app.domain.usecase.GetTasksUseCase
import com.flowtasks.app.domain.usecase.ReorderTasksUseCase
import com.flowtasks.app.domain.usecase.ToggleTaskCompletionUseCase
import com.flowtasks.app.domain.usecase.ToggleTaskStarUseCase
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

@OptIn(ExperimentalCoroutinesApi::class)
class HomeViewModel(
    private val getTasksUseCase: GetTasksUseCase,
    private val getTaskListsUseCase: GetTaskListsUseCase,
    private val createTaskUseCase: CreateTaskUseCase,
    private val deleteTaskUseCase: DeleteTaskUseCase,
    private val toggleTaskCompletionUseCase: ToggleTaskCompletionUseCase,
    private val toggleTaskStarUseCase: ToggleTaskStarUseCase,
    private val createTaskListUseCase: CreateTaskListUseCase,
    private val deleteTaskListUseCase: DeleteTaskListUseCase,
    private val getSettingsUseCase: GetSettingsUseCase,
    private val reorderTasksUseCase: ReorderTasksUseCase? = null
) : ViewModel() {

    private val _currentFilter = MutableStateFlow<TaskFilter>(TaskFilter.All)
    private val _currentSortOrder = MutableStateFlow(TaskSortOrder.DEFAULT_SORT_ORDER)
    private val _isCreateListDialogOpen = MutableStateFlow(false)
    private val _isQuickAddSheetOpen = MutableStateFlow(false)
    private val _errorMessage = MutableStateFlow<String?>(null)

    private val _filterAndSort = combine(_currentFilter, _currentSortOrder) { filter, sort ->
        filter to sort
    }

    private val _tasksFlow = _filterAndSort.flatMapLatest { (filter, sort) ->
        getTasksUseCase(filter, sort)
    }

    private data class DialogState(
        val isListDialog: Boolean,
        val isQuickAdd: Boolean,
        val error: String?
    )

    private val _dialogState = combine(
        _isCreateListDialogOpen,
        _isQuickAddSheetOpen,
        _errorMessage
    ) { isListDialog, isQuickAdd, error ->
        DialogState(isListDialog, isQuickAdd, error)
    }

    val uiState: StateFlow<HomeUiState> = combine(
        _tasksFlow,
        getTaskListsUseCase(),
        getSettingsUseCase(),
        _filterAndSort,
        _dialogState
    ) { tasks, lists, settings, filterSort, dialogs ->
        val (filter, sort) = filterSort
        val filteredTasks = if (settings.showCompletedTasks) tasks else tasks.filter { !it.isCompleted }
        val totalCount = tasks.size
        val completedCount = tasks.count { it.isCompleted }

        HomeUiState(
            isLoading = false,
            tasks = filteredTasks,
            taskLists = lists,
            currentFilter = filter,
            currentSortOrder = sort,
            showCompletedTasks = settings.showCompletedTasks,
            totalTaskCount = totalCount,
            completedTaskCount = completedCount,
            isCreateListDialogOpen = dialogs.isListDialog,
            isQuickAddSheetOpen = dialogs.isQuickAdd,
            errorMessage = dialogs.error
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = HomeUiState(isLoading = true)
    )

    fun setFilter(filter: TaskFilter) {
        _currentFilter.value = filter
    }

    fun setSortOrder(sortOrder: TaskSortOrder) {
        _currentSortOrder.value = sortOrder
    }

    fun toggleTaskCompletion(task: Task, isCompleted: Boolean) {
        viewModelScope.launch {
            try {
                toggleTaskCompletionUseCase(task.id, isCompleted)
            } catch (e: Exception) {
                _errorMessage.value = "Failed to update task: ${e.localizedMessage}"
            }
        }
    }

    fun toggleTaskStar(task: Task, isStarred: Boolean) {
        viewModelScope.launch {
            try {
                toggleTaskStarUseCase(task.id, isStarred)
            } catch (e: Exception) {
                _errorMessage.value = "Failed to star task: ${e.localizedMessage}"
            }
        }
    }

    fun deleteTask(taskId: Long) {
        viewModelScope.launch {
            try {
                deleteTaskUseCase(taskId)
            } catch (e: Exception) {
                _errorMessage.value = "Failed to delete task: ${e.localizedMessage}"
            }
        }
    }

    fun reorderTasks(taskIds: List<Long>) {
        viewModelScope.launch {
            try {
                reorderTasksUseCase?.invoke(taskIds)
            } catch (e: Exception) {
                _errorMessage.value = "Failed to reorder tasks: ${e.localizedMessage}"
            }
        }
    }

    fun quickCreateTask(
        title: String,
        description: String = "",
        dueDate: Long? = null,
        dueTime: String? = null,
        priority: TaskPriority = TaskPriority.NONE,
        listId: Long? = null,
        recurrence: RecurrenceRule = RecurrenceRule(),
        reminderType: ReminderType = ReminderType.NONE
    ) {
        if (title.isBlank()) return
        viewModelScope.launch {
            try {
                val assignedListId = listId ?: when (val f = _currentFilter.value) {
                    is TaskFilter.ByList -> f.listId
                    else -> null
                }
                val task = Task(
                    title = title.trim(),
                    description = description.trim(),
                    dueDate = dueDate,
                    dueTime = dueTime,
                    priority = priority,
                    listId = assignedListId,
                    recurrence = recurrence,
                    reminderType = reminderType
                )
                createTaskUseCase(task)
                _isQuickAddSheetOpen.value = false
            } catch (e: Exception) {
                _errorMessage.value = "Failed to create task: ${e.localizedMessage}"
            }
        }
    }

    fun createList(name: String, colorHex: String? = null) {
        if (name.isBlank()) return
        viewModelScope.launch {
            try {
                val id = createTaskListUseCase(name, colorHex)
                _isCreateListDialogOpen.value = false
                _currentFilter.value = TaskFilter.ByList(id, name.trim())
            } catch (e: Exception) {
                _errorMessage.value = "Failed to create list: ${e.localizedMessage}"
            }
        }
    }

    fun deleteList(listId: Long, strategy: DeleteListStrategy = DeleteListStrategy.MOVE_TO_INBOX) {
        viewModelScope.launch {
            try {
                deleteTaskListUseCase(listId, strategy)
                if (_currentFilter.value is TaskFilter.ByList && (_currentFilter.value as TaskFilter.ByList).listId == listId) {
                    _currentFilter.value = TaskFilter.All
                }
            } catch (e: Exception) {
                _errorMessage.value = "Failed to delete list: ${e.localizedMessage}"
            }
        }
    }

    fun showCreateListDialog(show: Boolean) {
        _isCreateListDialogOpen.value = show
    }

    fun showQuickAddSheet(show: Boolean) {
        _isQuickAddSheetOpen.value = show
    }

    fun dismissError() {
        _errorMessage.value = null
    }

    class Factory(
        private val getTasksUseCase: GetTasksUseCase,
        private val getTaskListsUseCase: GetTaskListsUseCase,
        private val createTaskUseCase: CreateTaskUseCase,
        private val deleteTaskUseCase: DeleteTaskUseCase,
        private val toggleTaskCompletionUseCase: ToggleTaskCompletionUseCase,
        private val toggleTaskStarUseCase: ToggleTaskStarUseCase,
        private val createTaskListUseCase: CreateTaskListUseCase,
        private val deleteTaskListUseCase: DeleteTaskListUseCase,
        private val getSettingsUseCase: GetSettingsUseCase,
        private val reorderTasksUseCase: ReorderTasksUseCase? = null
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return HomeViewModel(
                getTasksUseCase,
                getTaskListsUseCase,
                createTaskUseCase,
                deleteTaskUseCase,
                toggleTaskCompletionUseCase,
                toggleTaskStarUseCase,
                createTaskListUseCase,
                deleteTaskListUseCase,
                getSettingsUseCase,
                reorderTasksUseCase
            ) as T
        }
    }
}
