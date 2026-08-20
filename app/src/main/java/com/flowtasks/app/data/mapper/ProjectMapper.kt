package com.flowtasks.app.data.mapper

import com.flowtasks.app.data.local.entity.ProjectEntity
import com.flowtasks.app.domain.model.Project
import com.flowtasks.app.domain.model.ProjectStatus

object ProjectMapper {
    fun toDomain(
        entity: ProjectEntity,
        goalTitle: String? = null,
        totalTasksCount: Int = 0,
        completedTasksCount: Int = 0
    ): Project {
        return Project(
            id = entity.id,
            goalId = entity.goalId,
            goalTitle = goalTitle,
            title = entity.title,
            description = entity.description,
            targetDate = entity.targetDate,
            status = try {
                ProjectStatus.valueOf(entity.status)
            } catch (e: Exception) {
                ProjectStatus.ACTIVE
            },
            colorHex = entity.colorHex,
            sortOrder = entity.sortOrder,
            createdAt = entity.createdAt,
            updatedAt = entity.updatedAt,
            totalTasksCount = totalTasksCount,
            completedTasksCount = completedTasksCount
        )
    }

    fun toEntity(domain: Project): ProjectEntity {
        return ProjectEntity(
            id = domain.id,
            goalId = domain.goalId,
            title = domain.title,
            description = domain.description,
            targetDate = domain.targetDate,
            status = domain.status.name,
            colorHex = domain.colorHex,
            sortOrder = domain.sortOrder,
            createdAt = domain.createdAt,
            updatedAt = domain.updatedAt
        )
    }
}
