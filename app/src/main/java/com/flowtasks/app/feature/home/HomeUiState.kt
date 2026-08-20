package com.flowtasks.app.feature.home

import com.flowtasks.app.domain.model.Task
import com.flowtasks.app.domain.model.TaskFilter
import com.flowtasks.app.domain.model.TaskList
import com.flowtasks.app.domain.model.TaskSortOrder

data class HomeUiState(
    val isLoading: Boolean = true,
    val tasks: List<Task> = emptyList(),
    val taskLists: List<TaskList> = emptyList(),
    val currentFilter: TaskFilter = TaskFilter.All,
    val currentSortOrder: TaskSortOrder = TaskSortOrder.DEFAULT_SORT_ORDER,
    val showCompletedTasks: Boolean = true,
    val totalTaskCount: Int = 0,
    val completedTaskCount: Int = 0,
    val isCreateListDialogOpen: Boolean = false,
    val isQuickAddSheetOpen: Boolean = false,
    val errorMessage: String? = null
)
