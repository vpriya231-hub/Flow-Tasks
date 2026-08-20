package com.flowtasks.app.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.flowtasks.app.data.local.entity.HabitCompletionEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface HabitCompletionDao {
    @Query("SELECT * FROM habit_completions WHERE habit_id = :habitId ORDER BY completed_date DESC")
    fun getCompletionsByHabit(habitId: Long): Flow<List<HabitCompletionEntity>>

    @Query("SELECT * FROM habit_completions WHERE habit_id = :habitId ORDER BY completed_date DESC")
    suspend fun getCompletionsByHabitDirect(habitId: Long): List<HabitCompletionEntity>

    @Query("SELECT * FROM habit_completions WHERE completed_date >= :startDate AND completed_date <= :endDate")
    fun getCompletionsInDateRange(startDate: Long, endDate: Long): Flow<List<HabitCompletionEntity>>

    @Query("SELECT * FROM habit_completions WHERE completed_date >= :startDate AND completed_date <= :endDate")
    suspend fun getCompletionsInDateRangeDirect(startDate: Long, endDate: Long): List<HabitCompletionEntity>

    @Query("SELECT * FROM habit_completions WHERE habit_id = :habitId AND completed_date = :completedDate LIMIT 1")
    suspend fun getCompletionForDate(habitId: Long, completedDate: Long): HabitCompletionEntity?

    @Query("SELECT EXISTS(SELECT 1 FROM habit_completions WHERE habit_id = :habitId AND completed_date = :completedDate)")
    fun isHabitCompletedOnDateFlow(habitId: Long, completedDate: Long): Flow<Boolean>

    @Query("SELECT EXISTS(SELECT 1 FROM habit_completions WHERE habit_id = :habitId AND completed_date = :completedDate)")
    suspend fun isHabitCompletedOnDate(habitId: Long, completedDate: Long): Boolean

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCompletion(completion: HabitCompletionEntity): Long

    @Query("DELETE FROM habit_completions WHERE habit_id = :habitId AND completed_date = :completedDate")
    suspend fun deleteCompletionByDate(habitId: Long, completedDate: Long)

    @Query("DELETE FROM habit_completions WHERE habit_id = :habitId")
    suspend fun deleteCompletionsByHabitId(habitId: Long)

    @Query("SELECT * FROM habit_completions ORDER BY completed_date DESC")
    fun getAllCompletionsFlow(): Flow<List<HabitCompletionEntity>>

    @Query("SELECT COUNT(*) FROM habit_completions WHERE habit_id = :habitId")
    fun getTotalCompletionCount(habitId: Long): Flow<Int>
}
