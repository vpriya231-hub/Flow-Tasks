package com.flowtasks.app.feature.habit

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.flowtasks.app.domain.model.Habit
import com.flowtasks.app.domain.model.HabitFrequencyType
import com.flowtasks.app.domain.usecase.CreateHabitUseCase
import com.flowtasks.app.domain.usecase.DeleteHabitUseCase
import com.flowtasks.app.domain.usecase.GetHabitsUseCase
import com.flowtasks.app.domain.usecase.ToggleHabitCompletionForDateUseCase
import com.flowtasks.app.domain.usecase.ToggleHabitCompletionTodayUseCase
import com.flowtasks.app.domain.usecase.UpdateHabitUseCase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class HabitsUiState(
    val habits: List<Habit> = emptyList(),
    val isCreateHabitSheetOpen: Boolean = false,
    val editingHabit: Habit? = null,
    val habitToDelete: Habit? = null,
    val historyHabit: Habit? = null
)

class HabitsViewModel(
    private val getHabitsUseCase: GetHabitsUseCase,
    private val createHabitUseCase: CreateHabitUseCase,
    private val updateHabitUseCase: UpdateHabitUseCase,
    private val deleteHabitUseCase: DeleteHabitUseCase,
    private val toggleHabitCompletionTodayUseCase: ToggleHabitCompletionTodayUseCase,
    private val toggleHabitCompletionForDateUseCase: ToggleHabitCompletionForDateUseCase
) : ViewModel() {

    private val _isCreateHabitSheetOpen = MutableStateFlow(false)
    private val _editingHabit = MutableStateFlow<Habit?>(null)
    private val _habitToDelete = MutableStateFlow<Habit?>(null)
    private val _historyHabitId = MutableStateFlow<Long?>(null)

    val uiState: StateFlow<HabitsUiState> = combine(
        getHabitsUseCase(includeArchived = false),
        _isCreateHabitSheetOpen,
        _editingHabit,
        _habitToDelete,
        _historyHabitId
    ) { habits, isCreateOpen, editingHabit, habitToDelete, historyHabitId ->
        val activeHistoryHabit = if (historyHabitId != null) {
            habits.find { it.id == historyHabitId }
        } else null

        HabitsUiState(
            habits = habits,
            isCreateHabitSheetOpen = isCreateOpen,
            editingHabit = editingHabit,
            habitToDelete = habitToDelete,
            historyHabit = activeHistoryHabit
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = HabitsUiState()
    )

    fun openHabitHistory(habit: Habit) {
        _historyHabitId.value = habit.id
    }

    fun closeHabitHistory() {
        _historyHabitId.value = null
    }

    fun openCreateHabitSheet() {
        _editingHabit.value = null
        _isCreateHabitSheetOpen.value = true
    }

    fun openEditHabitSheet(habit: Habit) {
        _editingHabit.value = habit
        _isCreateHabitSheetOpen.value = true
    }

    fun closeHabitSheet() {
        _isCreateHabitSheetOpen.value = false
        _editingHabit.value = null
    }

    fun saveHabit(
        title: String,
        description: String,
        frequencyType: HabitFrequencyType,
        frequencyDays: List<Int>,
        targetCount: Int,
        colorHex: String
    ) {
        viewModelScope.launch {
            val editing = _editingHabit.value
            if (editing != null) {
                updateHabitUseCase(
                    editing.copy(
                        title = title,
                        description = description,
                        frequencyType = frequencyType,
                        frequencyDays = frequencyDays,
                        targetCountPerPeriod = targetCount,
                        colorHex = colorHex
                    )
                )
            } else {
                createHabitUseCase(
                    title = title,
                    description = description,
                    frequencyType = frequencyType,
                    frequencyDays = frequencyDays,
                    targetCountPerPeriod = targetCount,
                    colorHex = colorHex
                )
            }
            closeHabitSheet()
        }
    }

    fun toggleHabitToday(habitId: Long) {
        viewModelScope.launch {
            toggleHabitCompletionTodayUseCase(habitId)
        }
    }

    fun toggleHabitForDate(habitId: Long, dateMillis: Long) {
        viewModelScope.launch {
            toggleHabitCompletionForDateUseCase(habitId, dateMillis)
        }
    }

    fun confirmDeleteHabit(habit: Habit) {
        _habitToDelete.value = habit
    }

    fun dismissDeleteHabitDialog() {
        _habitToDelete.value = null
    }

    fun deleteHabitConfirmed() {
        val habit = _habitToDelete.value ?: return
        viewModelScope.launch {
            deleteHabitUseCase(habit.id)
            _habitToDelete.value = null
        }
    }

    class Factory(
        private val getHabitsUseCase: GetHabitsUseCase,
        private val createHabitUseCase: CreateHabitUseCase,
        private val updateHabitUseCase: UpdateHabitUseCase,
        private val deleteHabitUseCase: DeleteHabitUseCase,
        private val toggleHabitCompletionTodayUseCase: ToggleHabitCompletionTodayUseCase,
        private val toggleHabitCompletionForDateUseCase: ToggleHabitCompletionForDateUseCase
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return HabitsViewModel(
                getHabitsUseCase = getHabitsUseCase,
                createHabitUseCase = createHabitUseCase,
                updateHabitUseCase = updateHabitUseCase,
                deleteHabitUseCase = deleteHabitUseCase,
                toggleHabitCompletionTodayUseCase = toggleHabitCompletionTodayUseCase,
                toggleHabitCompletionForDateUseCase = toggleHabitCompletionForDateUseCase
            ) as T
        }
    }
}
