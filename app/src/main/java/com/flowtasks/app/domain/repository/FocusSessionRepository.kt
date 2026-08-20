package com.flowtasks.app.domain.repository

import com.flowtasks.app.domain.model.FocusSession
import kotlinx.coroutines.flow.Flow

interface FocusSessionRepository {
    suspend fun saveSession(session: FocusSession): Long
    suspend fun deleteSession(id: Long)
    fun getAllCompletedSessions(): Flow<List<FocusSession>>
    fun getCompletedSessionsByTask(taskId: Long): Flow<List<FocusSession>>
    fun getTotalFocusSecondsByTask(taskId: Long): Flow<Int>
    fun getTotalFocusSecondsAll(): Flow<Int>
    fun getCompletedSessionsInRange(startTime: Long, endTime: Long): Flow<List<FocusSession>>
    fun getCompletedSessionCount(): Flow<Int>
}
