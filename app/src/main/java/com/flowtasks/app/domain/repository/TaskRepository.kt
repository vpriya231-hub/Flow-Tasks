package com.flowtasks.app.domain.repository

import com.flowtasks.app.domain.model.Task
import com.flowtasks.app.domain.model.TaskFilter
import com.flowtasks.app.domain.model.TaskSortOrder
import kotlinx.coroutines.flow.Flow

interface TaskRepository {
    fun getTasks(filter: TaskFilter = TaskFilter.All, sortOrder: TaskSortOrder = TaskSortOrder.DEFAULT_SORT_ORDER): Flow<List<Task>>
    fun getTaskById(id: Long): Flow<Task?>
    suspend fun getTaskByIdDirect(id: Long): Task?
    fun getSubtasks(parentId: Long): Flow<List<Task>>
    suspend fun getSubtasksDirect(parentId: Long): List<Task>
    fun searchTasks(query: String): Flow<List<Task>>
    fun getTotalTaskCount(): Flow<Int>
    fun getCompletedTaskCount(): Flow<Int>
    suspend fun getTasksWithUpcomingReminders(currentTime: Long): List<Task>
    suspend fun createTask(task: Task): Long
    suspend fun updateTask(task: Task)
    suspend fun deleteTask(id: Long)
    suspend fun toggleTaskCompletion(id: Long, isCompleted: Boolean)
    suspend fun toggleTaskStar(id: Long, isStarred: Boolean)
    suspend fun updateSortOrder(id: Long, sortOrder: Int)
    suspend fun reorderTasks(taskIds: List<Long>)
    suspend fun moveTasksToInbox(listId: Long)
    suspend fun deleteTasksByListId(listId: Long)
}
