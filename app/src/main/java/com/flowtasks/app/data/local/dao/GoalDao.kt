package com.flowtasks.app.data.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.flowtasks.app.data.local.entity.GoalEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface GoalDao {
    @Query("SELECT * FROM goals WHERE status != 'ARCHIVED' ORDER BY sort_order ASC, created_at DESC")
    fun getAllActiveGoals(): Flow<List<GoalEntity>>

    @Query("SELECT * FROM goals ORDER BY sort_order ASC, created_at DESC")
    fun getAllGoals(): Flow<List<GoalEntity>>

    @Query("SELECT * FROM goals WHERE id = :id")
    fun getGoalById(id: Long): Flow<GoalEntity?>

    @Query("SELECT * FROM goals WHERE id = :id")
    suspend fun getGoalByIdDirect(id: Long): GoalEntity?

    @Query("SELECT COUNT(*) FROM goals WHERE status = 'ACTIVE'")
    fun getActiveGoalCount(): Flow<Int>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertGoal(goal: GoalEntity): Long

    @Update
    suspend fun updateGoal(goal: GoalEntity)

    @Delete
    suspend fun deleteGoal(goal: GoalEntity)

    @Query("DELETE FROM goals WHERE id = :id")
    suspend fun deleteGoalById(id: Long)

    @Query("UPDATE goals SET sort_order = :sortOrder, updated_at = :updatedAt WHERE id = :id")
    suspend fun updateSortOrder(id: Long, sortOrder: Int, updatedAt: Long)

    @Query("UPDATE goals SET status = :status, updated_at = :updatedAt WHERE id = :id")
    suspend fun updateGoalStatus(id: Long, status: String, updatedAt: Long)
}
