package com.flowtasks.app.data.repository

import com.flowtasks.app.core.common.DispatcherProvider
import com.flowtasks.app.data.local.dao.GoalDao
import com.flowtasks.app.data.local.dao.ProjectDao
import com.flowtasks.app.data.local.dao.TaskDao
import com.flowtasks.app.data.mapper.ProjectMapper
import com.flowtasks.app.domain.model.Project
import com.flowtasks.app.domain.model.ProjectStatus
import com.flowtasks.app.domain.repository.ProjectRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext

class ProjectRepositoryImpl(
    private val projectDao: ProjectDao,
    private val goalDao: GoalDao,
    private val taskDao: TaskDao,
    private val dispatchers: DispatcherProvider
) : ProjectRepository {

    override fun getProjects(includeArchived: Boolean): Flow<List<Project>> {
        val baseFlow = if (includeArchived) projectDao.getAllProjects() else projectDao.getAllActiveProjects()
        return baseFlow.map { entities ->
            entities.map { entity ->
                val goalTitle = entity.goalId?.let { goalDao.getGoalByIdDirect(it)?.title }
                ProjectMapper.toDomain(
                    entity = entity,
                    goalTitle = goalTitle,
                    totalTasksCount = 0,
                    completedTasksCount = 0
                )
            }
        }.flowOn(dispatchers.io)
    }

    override fun getProjectsByGoal(goalId: Long): Flow<List<Project>> {
        return projectDao.getProjectsByGoal(goalId).map { entities ->
            val goalTitle = goalDao.getGoalByIdDirect(goalId)?.title
            entities.map { entity ->
                ProjectMapper.toDomain(
                    entity = entity,
                    goalTitle = goalTitle,
                    totalTasksCount = 0,
                    completedTasksCount = 0
                )
            }
        }.flowOn(dispatchers.io)
    }

    override fun getProjectById(id: Long): Flow<Project?> {
        return projectDao.getProjectById(id).map { entity ->
            if (entity == null) return@map null
            val goalTitle = entity.goalId?.let { goalDao.getGoalByIdDirect(it)?.title }
            ProjectMapper.toDomain(
                entity = entity,
                goalTitle = goalTitle,
                totalTasksCount = 0,
                completedTasksCount = 0
            )
        }.flowOn(dispatchers.io)
    }

    override suspend fun getProjectByIdDirect(id: Long): Project? = withContext(dispatchers.io) {
        val entity = projectDao.getProjectByIdDirect(id) ?: return@withContext null
        val goalTitle = entity.goalId?.let { goalDao.getGoalByIdDirect(it)?.title }
        ProjectMapper.toDomain(
            entity = entity,
            goalTitle = goalTitle,
            totalTasksCount = 0,
            completedTasksCount = 0
        )
    }

    override suspend fun createProject(project: Project): Long = withContext(dispatchers.io) {
        val entity = ProjectMapper.toEntity(project).copy(
            createdAt = System.currentTimeMillis(),
            updatedAt = System.currentTimeMillis()
        )
        projectDao.insertProject(entity)
    }

    override suspend fun updateProject(project: Project) = withContext(dispatchers.io) {
        val entity = ProjectMapper.toEntity(project).copy(
            updatedAt = System.currentTimeMillis()
        )
        projectDao.updateProject(entity)
    }

    override suspend fun deleteProject(id: Long) = withContext(dispatchers.io) {
        val now = System.currentTimeMillis()
        // Safe deletion: unlink tasks from project so tasks are NOT lost
        taskDao.unlinkTasksFromProject(id, now)
        projectDao.deleteProjectById(id)
    }

    override suspend fun updateProjectStatus(id: Long, isCompleted: Boolean) = withContext(dispatchers.io) {
        val status = if (isCompleted) ProjectStatus.COMPLETED.name else ProjectStatus.ACTIVE.name
        projectDao.updateProjectStatus(id, status, System.currentTimeMillis())
    }

    override suspend fun updateSortOrder(id: Long, sortOrder: Int) = withContext(dispatchers.io) {
        projectDao.updateSortOrder(id, sortOrder, System.currentTimeMillis())
    }
}
