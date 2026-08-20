package com.flowtasks.app.data.repository

import com.flowtasks.app.core.common.DispatcherProvider
import com.flowtasks.app.data.local.dao.FocusSessionDao
import com.flowtasks.app.data.local.dao.TaskDao
import com.flowtasks.app.data.local.entity.FocusSessionEntity
import com.flowtasks.app.domain.model.FocusSession
import com.flowtasks.app.domain.model.FocusSessionStatus
import com.flowtasks.app.domain.repository.FocusSessionRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext

class FocusSessionRepositoryImpl(
    private val focusSessionDao: FocusSessionDao,
    private val taskDao: TaskDao,
    private val dispatchers: DispatcherProvider
) : FocusSessionRepository {

    override suspend fun saveSession(session: FocusSession): Long = withContext(dispatchers.io) {
        val entity = FocusSessionEntity(
            id = session.id,
            taskId = session.taskId,
            startedAt = session.startedAt,
            endedAt = session.endedAt ?: System.currentTimeMillis(),
            durationSeconds = session.durationSeconds,
            targetDurationMinutes = session.targetDurationMinutes,
            status = session.status.name,
            notes = session.notes
        )
        val newId = focusSessionDao.insertSession(entity)

        // If session is associated with a task and completed, update the task's actualDurationMinutes
        if (session.taskId != null && session.status == FocusSessionStatus.COMPLETED) {
            val sessions = focusSessionDao.getCompletedSessionsByTaskDirect(session.taskId)
            val totalSeconds = sessions.sumOf { it.durationSeconds }
            val totalMinutes = (totalSeconds + 59) / 60
            val taskEntity = taskDao.getTaskByIdDirect(session.taskId)
            if (taskEntity != null) {
                taskDao.updateTask(taskEntity.copy(actualDurationMinutes = totalMinutes))
            }
        }

        newId
    }

    override suspend fun deleteSession(id: Long) = withContext(dispatchers.io) {
        focusSessionDao.deleteSession(id)
    }

    override fun getAllCompletedSessions(): Flow<List<FocusSession>> {
        return focusSessionDao.getAllCompletedSessions().map { entities ->
            entities.map { mapToDomain(it) }
        }.flowOn(dispatchers.io)
    }

    override fun getCompletedSessionsByTask(taskId: Long): Flow<List<FocusSession>> {
        return focusSessionDao.getCompletedSessionsByTask(taskId).map { entities ->
            entities.map { mapToDomain(it) }
        }.flowOn(dispatchers.io)
    }

    override fun getTotalFocusSecondsByTask(taskId: Long): Flow<Int> {
        return focusSessionDao.getTotalFocusSecondsByTask(taskId).map { it ?: 0 }.flowOn(dispatchers.io)
    }

    override fun getTotalFocusSecondsAll(): Flow<Int> {
        return focusSessionDao.getTotalFocusSecondsAll().map { it ?: 0 }.flowOn(dispatchers.io)
    }

    override fun getCompletedSessionsInRange(
        startTime: Long,
        endTime: Long
    ): Flow<List<FocusSession>> {
        return focusSessionDao.getCompletedSessionsInRange(startTime, endTime).map { entities ->
            entities.map { mapToDomain(it) }
        }.flowOn(dispatchers.io)
    }

    override fun getCompletedSessionCount(): Flow<Int> {
        return focusSessionDao.getCompletedSessionCount().flowOn(dispatchers.io)
    }

    private suspend fun mapToDomain(entity: FocusSessionEntity): FocusSession {
        val taskTitle = if (entity.taskId != null) {
            taskDao.getTaskByIdDirect(entity.taskId)?.title
        } else null

        val status = try {
            FocusSessionStatus.valueOf(entity.status)
        } catch (e: Exception) {
            FocusSessionStatus.COMPLETED
        }

        return FocusSession(
            id = entity.id,
            taskId = entity.taskId,
            taskTitle = taskTitle,
            startedAt = entity.startedAt,
            endedAt = entity.endedAt,
            durationSeconds = entity.durationSeconds,
            targetDurationMinutes = entity.targetDurationMinutes,
            status = status,
            notes = entity.notes
        )
    }
}
