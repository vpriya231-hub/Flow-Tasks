package com.flowtasks.app.data.repository

import com.flowtasks.app.core.common.DispatcherProvider
import com.flowtasks.app.data.local.dao.TaskDao
import com.flowtasks.app.data.mapper.TaskMapper
import com.flowtasks.app.domain.model.Task
import com.flowtasks.app.domain.model.TaskFilter
import com.flowtasks.app.domain.model.TaskSortOrder
import com.flowtasks.app.domain.repository.TaskRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import java.util.Calendar

class TaskRepositoryImpl(
    private val taskDao: TaskDao,
    private val dispatchers: DispatcherProvider
) : TaskRepository {

    override fun getTasks(
        filter: TaskFilter,
        sortOrder: TaskSortOrder
    ): Flow<List<Task>> {
        val baseFlow = when (filter) {
            is TaskFilter.All -> taskDao.getAllRootTasks()
            is TaskFilter.Today -> taskDao.getAllRootTasks().map { list ->
                val (startOfDay, endOfDay) = getTodayBounds()
                list.filter { task ->
                    task.dueDate != null && task.dueDate in startOfDay..endOfDay
                }
            }
            is TaskFilter.Starred -> taskDao.getStarredRootTasks()
            is TaskFilter.Overdue -> taskDao.getPendingRootTasks().map { list ->
                val now = System.currentTimeMillis()
                list.filter { entity ->
                    val domainTask = TaskMapper.toDomain(entity)
                    domainTask.isOverdue(now)
                }
            }
            is TaskFilter.Completed -> taskDao.getCompletedRootTasks()
            is TaskFilter.ByList -> taskDao.getRootTasksByList(filter.listId)
            is TaskFilter.ByProject -> taskDao.getRootTasksByProject(filter.projectId)
            is TaskFilter.ByGoal -> taskDao.getRootTasksByGoal(filter.goalId)
        }

        return baseFlow.map { entities ->
            val tasks = entities.map { entity ->
                val subtaskCount = taskDao.getSubtaskCount(entity.id)
                val completedSubtaskCount = taskDao.getCompletedSubtaskCount(entity.id)
                TaskMapper.toDomain(entity, subtaskCount, completedSubtaskCount)
            }
            sortTasks(tasks, sortOrder)
        }.flowOn(dispatchers.io)
    }

    private fun sortTasks(tasks: List<Task>, sortOrder: TaskSortOrder): List<Task> {
        return when (sortOrder) {
            TaskSortOrder.DEFAULT_SORT_ORDER -> tasks.sortedWith(
                compareBy<Task> { it.isCompleted }
                    .thenBy { it.sortOrder }
                    .thenByDescending { it.createdAt }
            )
            TaskSortOrder.DUE_DATE_ASC -> tasks.sortedWith(
                compareBy<Task> { it.isCompleted }
                    .thenBy(nullsLast()) { it.getTargetDueTimeMillis() ?: it.dueDate }
                    .thenByDescending { it.priority.level }
            )
            TaskSortOrder.PRIORITY_DESC -> tasks.sortedWith(
                compareBy<Task> { it.isCompleted }
                    .thenByDescending { it.priority.level }
                    .thenBy(nullsLast()) { it.getTargetDueTimeMillis() ?: it.dueDate }
            )
            TaskSortOrder.CREATED_AT_DESC -> tasks.sortedWith(
                compareBy<Task> { it.isCompleted }
                    .thenByDescending { it.createdAt }
            )
            TaskSortOrder.TITLE_ASC -> tasks.sortedWith(
                compareBy<Task> { it.isCompleted }
                    .thenBy(String.CASE_INSENSITIVE_ORDER) { it.title }
            )
        }
    }

    private fun getTodayBounds(): Pair<Long, Long> {
        val calendar = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        val startOfDay = calendar.timeInMillis
        calendar.apply {
            set(Calendar.HOUR_OF_DAY, 23)
            set(Calendar.MINUTE, 59)
            set(Calendar.SECOND, 59)
            set(Calendar.MILLISECOND, 999)
        }
        val endOfDay = calendar.timeInMillis
        return Pair(startOfDay, endOfDay)
    }

    override fun getTaskById(id: Long): Flow<Task?> {
        return taskDao.getTaskById(id).map { entity ->
            if (entity == null) null
            else {
                val subtaskCount = taskDao.getSubtaskCount(entity.id)
                val completedSubtaskCount = taskDao.getCompletedSubtaskCount(entity.id)
                TaskMapper.toDomain(entity, subtaskCount, completedSubtaskCount)
            }
        }.flowOn(dispatchers.io)
    }

    override suspend fun getTaskByIdDirect(id: Long): Task? = withContext(dispatchers.io) {
        val entity = taskDao.getTaskByIdDirect(id) ?: return@withContext null
        val subtaskCount = taskDao.getSubtaskCount(entity.id)
        val completedSubtaskCount = taskDao.getCompletedSubtaskCount(entity.id)
        TaskMapper.toDomain(entity, subtaskCount, completedSubtaskCount)
    }

    override fun getSubtasks(parentId: Long): Flow<List<Task>> {
        return taskDao.getSubtasks(parentId).map { list ->
            list.map { TaskMapper.toDomain(it) }
        }.flowOn(dispatchers.io)
    }

    override suspend fun getSubtasksDirect(parentId: Long): List<Task> = withContext(dispatchers.io) {
        taskDao.getSubtasksDirect(parentId).map { TaskMapper.toDomain(it) }
    }

    override fun searchTasks(query: String): Flow<List<Task>> {
        return taskDao.searchTasks(query).map { list ->
            list.map { entity ->
                val subtaskCount = taskDao.getSubtaskCount(entity.id)
                val completedSubtaskCount = taskDao.getCompletedSubtaskCount(entity.id)
                TaskMapper.toDomain(entity, subtaskCount, completedSubtaskCount)
            }
        }.flowOn(dispatchers.io)
    }

    override fun getTotalTaskCount(): Flow<Int> = taskDao.getTotalTaskCount().flowOn(dispatchers.io)

    override fun getCompletedTaskCount(): Flow<Int> = taskDao.getCompletedTaskCount().flowOn(dispatchers.io)

    override suspend fun getTasksWithUpcomingReminders(currentTime: Long): List<Task> = withContext(dispatchers.io) {
        taskDao.getPendingTasksWithReminders()
            .map { TaskMapper.toDomain(it) }
            .filter { task ->
                val trigger = task.calculateReminderTriggerTime()
                trigger != null && trigger > currentTime
            }
    }

    override suspend fun createTask(task: Task): Long = withContext(dispatchers.io) {
        val now = System.currentTimeMillis()
        val entity = TaskMapper.toEntity(task.copy(createdAt = now, updatedAt = now))
        taskDao.insertTask(entity)
    }

    override suspend fun updateTask(task: Task) = withContext(dispatchers.io) {
        val now = System.currentTimeMillis()
        val entity = TaskMapper.toEntity(task.copy(updatedAt = now))
        taskDao.updateTask(entity)
    }

    override suspend fun deleteTask(id: Long) = withContext(dispatchers.io) {
        taskDao.deleteSubtasksByParentId(id)
        taskDao.deleteTaskById(id)
    }

    override suspend fun toggleTaskCompletion(id: Long, isCompleted: Boolean) = withContext(dispatchers.io) {
        val now = System.currentTimeMillis()
        val completedAt = if (isCompleted) now else null
        taskDao.updateCompletionStatus(id, isCompleted, completedAt, now)
    }

    override suspend fun toggleTaskStar(id: Long, isStarred: Boolean) = withContext(dispatchers.io) {
        val now = System.currentTimeMillis()
        taskDao.updateStarStatus(id, isStarred, now)
    }

    override suspend fun updateSortOrder(id: Long, sortOrder: Int) = withContext(dispatchers.io) {
        val now = System.currentTimeMillis()
        taskDao.updateSortOrder(id, sortOrder, now)
    }

    override suspend fun reorderTasks(taskIds: List<Long>) = withContext(dispatchers.io) {
        val now = System.currentTimeMillis()
        taskIds.forEachIndexed { index, taskId ->
            taskDao.updateSortOrder(taskId, index, now)
        }
    }

    override suspend fun moveTasksToInbox(listId: Long) = withContext(dispatchers.io) {
        val now = System.currentTimeMillis()
        taskDao.moveTasksToInbox(listId, now)
    }

    override suspend fun deleteTasksByListId(listId: Long) = withContext(dispatchers.io) {
        taskDao.deleteTasksByListId(listId)
    }
}
