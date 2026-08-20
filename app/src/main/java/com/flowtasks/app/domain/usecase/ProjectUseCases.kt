package com.flowtasks.app.domain.usecase

import com.flowtasks.app.domain.model.Project
import com.flowtasks.app.domain.repository.ProjectRepository
import kotlinx.coroutines.flow.Flow

class GetProjectsUseCase(
    private val projectRepository: ProjectRepository
) {
    operator fun invoke(includeArchived: Boolean = false): Flow<List<Project>> {
        return projectRepository.getProjects(includeArchived)
    }
}

class GetProjectsByGoalUseCase(
    private val projectRepository: ProjectRepository
) {
    operator fun invoke(goalId: Long): Flow<List<Project>> {
        return projectRepository.getProjectsByGoal(goalId)
    }
}

class CreateProjectUseCase(
    private val projectRepository: ProjectRepository
) {
    suspend operator fun invoke(
        title: String,
        goalId: Long? = null,
        description: String = "",
        targetDate: Long? = null,
        colorHex: String = "#0EA5E9"
    ): Long {
        if (title.isBlank()) return -1
        val project = Project(
            title = title.trim(),
            goalId = goalId,
            description = description.trim(),
            targetDate = targetDate,
            colorHex = colorHex
        )
        return projectRepository.createProject(project)
    }
}

class UpdateProjectUseCase(
    private val projectRepository: ProjectRepository
) {
    suspend operator fun invoke(project: Project) {
        if (project.title.isBlank()) return
        projectRepository.updateProject(project)
    }
}

class DeleteProjectUseCase(
    private val projectRepository: ProjectRepository
) {
    suspend operator fun invoke(projectId: Long) {
        projectRepository.deleteProject(projectId)
    }
}

class ToggleProjectStatusUseCase(
    private val projectRepository: ProjectRepository
) {
    suspend operator fun invoke(projectId: Long, isCompleted: Boolean) {
        projectRepository.updateProjectStatus(projectId, isCompleted)
    }
}
