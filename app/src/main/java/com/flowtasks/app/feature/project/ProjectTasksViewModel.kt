package com.flowtasks.app.feature.project

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.flowtasks.app.domain.model.Task
import com.flowtasks.app.domain.model.TaskFilter
import com.flowtasks.app.domain.model.TaskPriority
import com.flowtasks.app.domain.usecase.CreateTaskUseCase
import com.flowtasks.app.domain.usecase.DeleteTaskUseCase
import com.flowtasks.app.domain.usecase.GetTasksUseCase
import com.flowtasks.app.domain.usecase.ToggleTaskCompletionUseCase
import com.flowtasks.app.domain.usecase.ToggleTaskStarUseCase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class ProjectTasksUiState(
    val projectId: Long,
    val projectTitle: String,
    val tasks: List<Task> = emptyList(),
    val isAddTaskSheetOpen: Boolean = false,
    val completedCount: Int = 0,
    val totalCount: Int = 0
)

class ProjectTasksViewModel(
    private val projectId: Long,
    private val projectTitle: String,
    private val getTasksUseCase: GetTasksUseCase,
    private val createTaskUseCase: CreateTaskUseCase,
    private val toggleTaskCompletionUseCase: ToggleTaskCompletionUseCase,
    private val toggleTaskStarUseCase: ToggleTaskStarUseCase,
    private val deleteTaskUseCase: DeleteTaskUseCase
) : ViewModel() {

    private val _isAddTaskSheetOpen = MutableStateFlow(false)

    val uiState: StateFlow<ProjectTasksUiState> = combine(
        getTasksUseCase(TaskFilter.ByProject(projectId, projectTitle)),
        _isAddTaskSheetOpen
    ) { tasks, isAddOpen ->
        val completed = tasks.count { it.isCompleted }
        ProjectTasksUiState(
            projectId = projectId,
            projectTitle = projectTitle,
            tasks = tasks,
            isAddTaskSheetOpen = isAddOpen,
            completedCount = completed,
            totalCount = tasks.size
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = ProjectTasksUiState(projectId = projectId, projectTitle = projectTitle)
    )

    fun openAddTaskSheet() {
        _isAddTaskSheetOpen.value = true
    }

    fun closeAddTaskSheet() {
        _isAddTaskSheetOpen.value = false
    }

    fun addTask(
        title: String,
        description: String,
        dueDate: Long?,
        dueTime: String?,
        priority: TaskPriority
    ) {
        viewModelScope.launch {
            val task = Task(
                title = title,
                description = description,
                projectId = projectId,
                projectTitle = projectTitle,
                dueDate = dueDate,
                dueTime = dueTime,
                priority = priority
            )
            createTaskUseCase(task)
            closeAddTaskSheet()
        }
    }

    fun toggleCompletion(task: Task, isCompleted: Boolean) {
        viewModelScope.launch {
            toggleTaskCompletionUseCase(task.id, isCompleted)
        }
    }

    fun toggleStar(task: Task, isStarred: Boolean) {
        viewModelScope.launch {
            toggleTaskStarUseCase(task.id, isStarred)
        }
    }

    fun deleteTask(task: Task) {
        viewModelScope.launch {
            deleteTaskUseCase(task.id)
        }
    }

    class Factory(
        private val projectId: Long,
        private val projectTitle: String,
        private val getTasksUseCase: GetTasksUseCase,
        private val createTaskUseCase: CreateTaskUseCase,
        private val toggleTaskCompletionUseCase: ToggleTaskCompletionUseCase,
        private val toggleTaskStarUseCase: ToggleTaskStarUseCase,
        private val deleteTaskUseCase: DeleteTaskUseCase
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return ProjectTasksViewModel(
                projectId = projectId,
                projectTitle = projectTitle,
                getTasksUseCase = getTasksUseCase,
                createTaskUseCase = createTaskUseCase,
                toggleTaskCompletionUseCase = toggleTaskCompletionUseCase,
                toggleTaskStarUseCase = toggleTaskStarUseCase,
                deleteTaskUseCase = deleteTaskUseCase
            ) as T
        }
    }
}
