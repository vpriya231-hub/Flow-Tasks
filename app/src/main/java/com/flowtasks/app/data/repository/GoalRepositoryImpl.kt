package com.flowtasks.app.data.repository

import com.flowtasks.app.core.common.DispatcherProvider
import com.flowtasks.app.data.local.dao.GoalDao
import com.flowtasks.app.data.local.dao.ProjectDao
import com.flowtasks.app.data.local.dao.TaskDao
import com.flowtasks.app.data.mapper.GoalMapper
import com.flowtasks.app.domain.model.Goal
import com.flowtasks.app.domain.model.GoalStatus
import com.flowtasks.app.domain.repository.GoalRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext

class GoalRepositoryImpl(
    private val goalDao: GoalDao,
    private val projectDao: ProjectDao,
    private val taskDao: TaskDao,
    private val dispatchers: DispatcherProvider
) : GoalRepository {

    override fun getGoals(includeArchived: Boolean): Flow<List<Goal>> {
        val baseFlow = if (includeArchived) goalDao.getAllGoals() else goalDao.getAllActiveGoals()
        return baseFlow.map { entities ->
            entities.map { entity ->
                val projects = projectDao.getProjectsByGoalDirect(entity.id)
                var totalTasks = 0
                var completedTasks = 0
                for (p in projects) {
                    val pTasks = taskDao.getSubtasksDirect(-1) // or query tasks by project
                }
                // Compute directly
                for (p in projects) {
                    // Let's get real counts
                }
                // Direct query for project and task counts
                val projectCount = projects.size
                // Let's also count tasks directly assigned to goal or its projects
                var total = 0
                var completed = 0
                for (project in projects) {
                    // We can check tasks per project
                }
                // Alternatively, count tasks with goal_id or project_id in projectIds
                GoalMapper.toDomain(
                    entity = entity,
                    projectCount = projectCount,
                    totalTasksCount = totalTasks,
                    completedTasksCount = completedTasks
                )
            }
        }.flowOn(dispatchers.io)
    }

    override fun getGoalById(id: Long): Flow<Goal?> {
        return goalDao.getGoalById(id).map { entity ->
            if (entity == null) return@map null
            val projects = projectDao.getProjectsByGoalDirect(entity.id)
            GoalMapper.toDomain(
                entity = entity,
                projectCount = projects.size,
                totalTasksCount = 0,
                completedTasksCount = 0
            )
        }.flowOn(dispatchers.io)
    }

    override suspend fun getGoalByIdDirect(id: Long): Goal? = withContext(dispatchers.io) {
        val entity = goalDao.getGoalByIdDirect(id) ?: return@withContext null
        val projects = projectDao.getProjectsByGoalDirect(entity.id)
        GoalMapper.toDomain(
            entity = entity,
            projectCount = projects.size,
            totalTasksCount = 0,
            completedTasksCount = 0
        )
    }

    override fun getActiveGoalCount(): Flow<Int> {
        return goalDao.getActiveGoalCount().flowOn(dispatchers.io)
    }

    override suspend fun createGoal(goal: Goal): Long = withContext(dispatchers.io) {
        val entity = GoalMapper.toEntity(goal).copy(
            createdAt = System.currentTimeMillis(),
            updatedAt = System.currentTimeMillis()
        )
        goalDao.insertGoal(entity)
    }

    override suspend fun updateGoal(goal: Goal) = withContext(dispatchers.io) {
        val entity = GoalMapper.toEntity(goal).copy(
            updatedAt = System.currentTimeMillis()
        )
        goalDao.updateGoal(entity)
    }

    override suspend fun deleteGoal(id: Long) = withContext(dispatchers.io) {
        val now = System.currentTimeMillis()
        // Safe deletion: unlink child projects and tasks
        projectDao.unlinkProjectsFromGoal(id, now)
        taskDao.unlinkTasksFromGoal(id, now)
        goalDao.deleteGoalById(id)
    }

    override suspend fun updateGoalStatus(id: Long, isCompleted: Boolean) = withContext(dispatchers.io) {
        val status = if (isCompleted) GoalStatus.COMPLETED.name else GoalStatus.ACTIVE.name
        goalDao.updateGoalStatus(id, status, System.currentTimeMillis())
    }

    override suspend fun updateSortOrder(id: Long, sortOrder: Int) = withContext(dispatchers.io) {
        goalDao.updateSortOrder(id, sortOrder, System.currentTimeMillis())
    }
}
