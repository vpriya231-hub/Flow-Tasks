package com.flowtasks.app.domain.repository

import com.flowtasks.app.domain.model.TaskList
import kotlinx.coroutines.flow.Flow

interface TaskListRepository {
    fun getAllLists(): Flow<List<TaskList>>
    fun getListById(id: Long): Flow<TaskList?>
    suspend fun getListByIdDirect(id: Long): TaskList?
    suspend fun createList(taskList: TaskList): Long
    suspend fun updateList(taskList: TaskList)
    suspend fun deleteList(id: Long)
    suspend fun updateSortOrder(id: Long, sortOrder: Int)
    suspend fun reorderLists(listIds: List<Long>)
}
