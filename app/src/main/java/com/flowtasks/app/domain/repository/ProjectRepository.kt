package com.flowtasks.app.domain.repository

import com.flowtasks.app.domain.model.Project
import kotlinx.coroutines.flow.Flow

interface ProjectRepository {
    fun getProjects(includeArchived: Boolean = false): Flow<List<Project>>
    fun getProjectsByGoal(goalId: Long): Flow<List<Project>>
    fun getProjectById(id: Long): Flow<Project?>
    suspend fun getProjectByIdDirect(id: Long): Project?
    suspend fun createProject(project: Project): Long
    suspend fun updateProject(project: Project)
    suspend fun deleteProject(id: Long)
    suspend fun updateProjectStatus(id: Long, isCompleted: Boolean)
    suspend fun updateSortOrder(id: Long, sortOrder: Int)
}
