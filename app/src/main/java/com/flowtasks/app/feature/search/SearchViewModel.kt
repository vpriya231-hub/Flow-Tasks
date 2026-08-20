package com.flowtasks.app.feature.search

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.flowtasks.app.domain.model.Task
import com.flowtasks.app.domain.usecase.SearchTasksUseCase
import com.flowtasks.app.domain.usecase.ToggleTaskCompletionUseCase
import com.flowtasks.app.domain.usecase.ToggleTaskStarUseCase
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class SearchUiState(
    val query: String = "",
    val results: List<Task> = emptyList(),
    val isSearching: Boolean = false
)

@OptIn(FlowPreview::class, ExperimentalCoroutinesApi::class)
class SearchViewModel(
    private val searchTasksUseCase: SearchTasksUseCase,
    private val toggleTaskCompletionUseCase: ToggleTaskCompletionUseCase,
    private val toggleTaskStarUseCase: ToggleTaskStarUseCase
) : ViewModel() {

    val query = MutableStateFlow("")

    val uiState: StateFlow<SearchUiState> = query
        .debounce(250)
        .distinctUntilChanged()
        .flatMapLatest { q ->
            if (q.isBlank()) {
                flowOf(SearchUiState(query = q, results = emptyList(), isSearching = false))
            } else {
                searchTasksUseCase(q).map { tasks ->
                    SearchUiState(query = q, results = tasks, isSearching = false)
                }
            }
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = SearchUiState()
        )

    fun onQueryChange(newQuery: String) {
        query.value = newQuery
    }

    fun toggleTaskCompletion(task: Task, isCompleted: Boolean) {
        viewModelScope.launch {
            toggleTaskCompletionUseCase(task.id, isCompleted)
        }
    }

    fun toggleTaskStar(task: Task, isStarred: Boolean) {
        viewModelScope.launch {
            toggleTaskStarUseCase(task.id, isStarred)
        }
    }

    class Factory(
        private val searchTasksUseCase: SearchTasksUseCase,
        private val toggleTaskCompletionUseCase: ToggleTaskCompletionUseCase,
        private val toggleTaskStarUseCase: ToggleTaskStarUseCase
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return SearchViewModel(
                searchTasksUseCase,
                toggleTaskCompletionUseCase,
                toggleTaskStarUseCase
            ) as T
        }
    }
}
