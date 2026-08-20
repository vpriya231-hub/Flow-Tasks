package com.flowtasks.app.data.repository

import com.flowtasks.app.core.common.DispatcherProvider
import com.flowtasks.app.data.local.dao.TaskListDao
import com.flowtasks.app.data.mapper.TaskMapper
import com.flowtasks.app.domain.model.TaskList
import com.flowtasks.app.domain.repository.TaskListRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext

class TaskListRepositoryImpl(
    private val taskListDao: TaskListDao,
    private val dispatchers: DispatcherProvider
) : TaskListRepository {

    override fun getAllLists(): Flow<List<TaskList>> {
        return taskListDao.getAllLists().map { list ->
            list.map { entity ->
                val taskCount = taskListDao.getTaskCountForListDirect(entity.id)
                TaskMapper.toDomain(entity, taskCount)
            }
        }.flowOn(dispatchers.io)
    }

    override fun getListById(id: Long): Flow<TaskList?> {
        return taskListDao.getListById(id).map { entity ->
            if (entity == null) null
            else {
                val taskCount = taskListDao.getTaskCountForListDirect(entity.id)
                TaskMapper.toDomain(entity, taskCount)
            }
        }.flowOn(dispatchers.io)
    }

    override suspend fun getListByIdDirect(id: Long): TaskList? = withContext(dispatchers.io) {
        val entity = taskListDao.getListByIdDirect(id) ?: return@withContext null
        val taskCount = taskListDao.getTaskCountForListDirect(entity.id)
        TaskMapper.toDomain(entity, taskCount)
    }

    override suspend fun createList(taskList: TaskList): Long = withContext(dispatchers.io) {
        val now = System.currentTimeMillis()
        val entity = TaskMapper.toEntity(taskList.copy(createdAt = now, updatedAt = now))
        taskListDao.insertList(entity)
    }

    override suspend fun updateList(taskList: TaskList) = withContext(dispatchers.io) {
        val now = System.currentTimeMillis()
        val entity = TaskMapper.toEntity(taskList.copy(updatedAt = now))
        taskListDao.updateList(entity)
    }

    override suspend fun deleteList(id: Long) = withContext(dispatchers.io) {
        taskListDao.deleteListById(id)
    }

    override suspend fun updateSortOrder(id: Long, sortOrder: Int) = withContext(dispatchers.io) {
        val now = System.currentTimeMillis()
        taskListDao.updateSortOrder(id, sortOrder, now)
    }

    override suspend fun reorderLists(listIds: List<Long>) = withContext(dispatchers.io) {
        val now = System.currentTimeMillis()
        listIds.forEachIndexed { index, listId ->
            taskListDao.updateSortOrder(listId, index, now)
        }
    }
}
