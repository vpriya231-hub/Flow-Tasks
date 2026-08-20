package com.flowtasks.app.domain.usecase

import com.flowtasks.app.domain.model.Habit
import com.flowtasks.app.domain.model.HabitFrequencyType
import com.flowtasks.app.domain.repository.HabitRepository
import kotlinx.coroutines.flow.Flow

class GetHabitsUseCase(
    private val habitRepository: HabitRepository
) {
    operator fun invoke(includeArchived: Boolean = false): Flow<List<Habit>> {
        return habitRepository.getHabits(includeArchived)
    }
}

class CreateHabitUseCase(
    private val habitRepository: HabitRepository
) {
    suspend operator fun invoke(
        title: String,
        description: String = "",
        frequencyType: HabitFrequencyType = HabitFrequencyType.DAILY,
        frequencyDays: List<Int> = emptyList(),
        targetCountPerPeriod: Int = 1,
        colorHex: String = "#10B981"
    ): Long {
        if (title.isBlank()) return -1
        val habit = Habit(
            title = title.trim(),
            description = description.trim(),
            frequencyType = frequencyType,
            frequencyDays = frequencyDays,
            targetCountPerPeriod = targetCountPerPeriod,
            colorHex = colorHex
        )
        return habitRepository.createHabit(habit)
    }
}

class UpdateHabitUseCase(
    private val habitRepository: HabitRepository
) {
    suspend operator fun invoke(habit: Habit) {
        if (habit.title.isBlank()) return
        habitRepository.updateHabit(habit)
    }
}

class DeleteHabitUseCase(
    private val habitRepository: HabitRepository
) {
    suspend operator fun invoke(habitId: Long) {
        habitRepository.deleteHabit(habitId)
    }
}

class ToggleHabitCompletionTodayUseCase(
    private val habitRepository: HabitRepository
) {
    suspend operator fun invoke(habitId: Long) {
        habitRepository.toggleHabitCompletionToday(habitId)
    }
}

class ToggleHabitCompletionForDateUseCase(
    private val habitRepository: HabitRepository
) {
    suspend operator fun invoke(habitId: Long, dateMillis: Long) {
        habitRepository.toggleHabitCompletionForDate(habitId, dateMillis)
    }
}
