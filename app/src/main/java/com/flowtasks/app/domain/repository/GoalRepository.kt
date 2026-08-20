package com.flowtasks.app.domain.repository

import com.flowtasks.app.domain.model.Goal
import kotlinx.coroutines.flow.Flow

interface GoalRepository {
    fun getGoals(includeArchived: Boolean = false): Flow<List<Goal>>
    fun getGoalById(id: Long): Flow<Goal?>
    suspend fun getGoalByIdDirect(id: Long): Goal?
    fun getActiveGoalCount(): Flow<Int>
    suspend fun createGoal(goal: Goal): Long
    suspend fun updateGoal(goal: Goal)
    suspend fun deleteGoal(id: Long)
    suspend fun updateGoalStatus(id: Long, isCompleted: Boolean)
    suspend fun updateSortOrder(id: Long, sortOrder: Int)
}
