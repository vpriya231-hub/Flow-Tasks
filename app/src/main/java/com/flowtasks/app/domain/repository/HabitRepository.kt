package com.flowtasks.app.domain.repository

import com.flowtasks.app.domain.model.Habit
import com.flowtasks.app.domain.model.HabitCompletion
import kotlinx.coroutines.flow.Flow

interface HabitRepository {
    fun getHabits(includeArchived: Boolean = false): Flow<List<Habit>>
    fun getHabitById(id: Long): Flow<Habit?>
    suspend fun getHabitByIdDirect(id: Long): Habit?
    fun getCompletionsByHabit(habitId: Long): Flow<List<HabitCompletion>>
    suspend fun createHabit(habit: Habit): Long
    suspend fun updateHabit(habit: Habit)
    suspend fun deleteHabit(id: Long)
    suspend fun toggleHabitCompletionToday(habitId: Long)
    suspend fun toggleHabitCompletionForDate(habitId: Long, dateMillis: Long)
    suspend fun updateSortOrder(id: Long, sortOrder: Int)
}
