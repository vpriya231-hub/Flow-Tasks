package com.flowtasks.app.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.flowtasks.app.data.local.entity.FocusSessionEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface FocusSessionDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSession(session: FocusSessionEntity): Long

    @Update
    suspend fun updateSession(session: FocusSessionEntity)

    @Query("DELETE FROM focus_sessions WHERE id = :id")
    suspend fun deleteSession(id: Long)

    @Query("SELECT * FROM focus_sessions ORDER BY started_at DESC")
    fun getAllSessions(): Flow<List<FocusSessionEntity>>

    @Query("SELECT * FROM focus_sessions WHERE status = 'COMPLETED' ORDER BY started_at DESC")
    fun getAllCompletedSessions(): Flow<List<FocusSessionEntity>>

    @Query("SELECT * FROM focus_sessions WHERE task_id = :taskId AND status = 'COMPLETED' ORDER BY started_at DESC")
    fun getCompletedSessionsByTask(taskId: Long): Flow<List<FocusSessionEntity>>

    @Query("SELECT * FROM focus_sessions WHERE task_id = :taskId AND status = 'COMPLETED'")
    suspend fun getCompletedSessionsByTaskDirect(taskId: Long): List<FocusSessionEntity>

    @Query("SELECT SUM(duration_seconds) FROM focus_sessions WHERE task_id = :taskId AND status = 'COMPLETED'")
    fun getTotalFocusSecondsByTask(taskId: Long): Flow<Int?>

    @Query("SELECT SUM(duration_seconds) FROM focus_sessions WHERE status = 'COMPLETED'")
    fun getTotalFocusSecondsAll(): Flow<Int?>

    @Query("SELECT * FROM focus_sessions WHERE started_at >= :startTime AND started_at <= :endTime AND status = 'COMPLETED' ORDER BY started_at ASC")
    fun getCompletedSessionsInRange(startTime: Long, endTime: Long): Flow<List<FocusSessionEntity>>

    @Query("SELECT COUNT(*) FROM focus_sessions WHERE status = 'COMPLETED'")
    fun getCompletedSessionCount(): Flow<Int>
}
