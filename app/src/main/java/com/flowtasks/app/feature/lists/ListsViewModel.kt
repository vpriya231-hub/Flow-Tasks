package com.flowtasks.app.feature.lists

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.flowtasks.app.domain.model.TaskFilter
import com.flowtasks.app.domain.model.TaskList
import com.flowtasks.app.domain.model.TaskSortOrder
import com.flowtasks.app.domain.usecase.CreateTaskListUseCase
import com.flowtasks.app.domain.usecase.DeleteListStrategy
import com.flowtasks.app.domain.usecase.DeleteTaskListUseCase
import com.flowtasks.app.domain.usecase.GetTaskListsUseCase
import com.flowtasks.app.domain.usecase.GetTasksUseCase
import com.flowtasks.app.domain.usecase.ReorderTaskListsUseCase
import com.flowtasks.app.domain.usecase.UpdateTaskListUseCase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class ListItemUiModel(
    val list: TaskList,
    val pendingCount: Int,
    val completedCount: Int
)

data class ListsUiState(
    val isLoading: Boolean = false,
    val lists: List<ListItemUiModel> = emptyList(),
    val isCreateListDialogOpen: Boolean = false,
    val listToEdit: TaskList? = null,
    val errorMessage: String? = null
)

class ListsViewModel(
    private val getTaskListsUseCase: GetTaskListsUseCase,
    private val createTaskListUseCase: CreateTaskListUseCase,
    private val updateTaskListUseCase: UpdateTaskListUseCase,
    private val deleteTaskListUseCase: DeleteTaskListUseCase,
    private val getTasksUseCase: GetTasksUseCase,
    private val reorderTaskListsUseCase: ReorderTaskListsUseCase? = null
) : ViewModel() {

    private val _isCreateListDialogOpen = MutableStateFlow(false)
    private val _listToEdit = MutableStateFlow<TaskList?>(null)
    private val _errorMessage = MutableStateFlow<String?>(null)

    val uiState: StateFlow<ListsUiState> = combine(
        getTaskListsUseCase(),
        getTasksUseCase(TaskFilter.All, TaskSortOrder.DEFAULT_SORT_ORDER),
        _isCreateListDialogOpen,
        _listToEdit,
        _errorMessage
    ) { lists, allTasks, isDialogOpen, editingList, error ->
        val listItems = lists.map { list ->
            val tasksForList = allTasks.filter { it.listId == list.id }
            ListItemUiModel(
                list = list,
                pendingCount = tasksForList.count { !it.isCompleted },
                completedCount = tasksForList.count { it.isCompleted }
            )
        }
        ListsUiState(
            isLoading = false,
            lists = listItems,
            isCreateListDialogOpen = isDialogOpen,
            listToEdit = editingList,
            errorMessage = error
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = ListsUiState(isLoading = true)
    )

    fun showCreateListDialog(show: Boolean) {
        _isCreateListDialogOpen.value = show
    }

    fun showEditListDialog(taskList: TaskList?) {
        _listToEdit.value = taskList
    }

    fun createList(name: String, colorHex: String? = null) {
        if (name.isBlank()) return
        viewModelScope.launch {
            try {
                createTaskListUseCase(name, colorHex)
                _isCreateListDialogOpen.value = false
            } catch (e: Exception) {
                _errorMessage.value = "Failed to create list: ${e.localizedMessage}"
            }
        }
    }

    fun updateList(id: Long, name: String, colorHex: String? = null) {
        if (name.isBlank()) return
        viewModelScope.launch {
            try {
                val existing = _listToEdit.value
                val updated = (existing ?: TaskList(id = id, name = name)).copy(
                    id = id,
                    name = name.trim(),
                    colorHex = colorHex
                )
                updateTaskListUseCase(updated)
                _listToEdit.value = null
            } catch (e: Exception) {
                _errorMessage.value = "Failed to update list: ${e.localizedMessage}"
            }
        }
    }

    fun deleteList(listId: Long, strategy: DeleteListStrategy = DeleteListStrategy.MOVE_TO_INBOX) {
        viewModelScope.launch {
            try {
                deleteTaskListUseCase(listId, strategy)
            } catch (e: Exception) {
                _errorMessage.value = "Failed to delete list: ${e.localizedMessage}"
            }
        }
    }

    fun reorderLists(listIds: List<Long>) {
        viewModelScope.launch {
            try {
                reorderTaskListsUseCase?.invoke(listIds)
            } catch (e: Exception) {
                _errorMessage.value = "Failed to reorder lists: ${e.localizedMessage}"
            }
        }
    }

    fun dismissError() {
        _errorMessage.value = null
    }

    class Factory(
        private val getTaskListsUseCase: GetTaskListsUseCase,
        private val createTaskListUseCase: CreateTaskListUseCase,
        private val updateTaskListUseCase: UpdateTaskListUseCase,
        private val deleteTaskListUseCase: DeleteTaskListUseCase,
        private val getTasksUseCase: GetTasksUseCase,
        private val reorderTaskListsUseCase: ReorderTaskListsUseCase? = null
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return ListsViewModel(
                getTaskListsUseCase,
                createTaskListUseCase,
                updateTaskListUseCase,
                deleteTaskListUseCase,
                getTasksUseCase,
                reorderTaskListsUseCase
            ) as T
        }
    }
}
