package com.flowtasks.app.data.mapper

import com.flowtasks.app.data.local.entity.GoalEntity
import com.flowtasks.app.domain.model.Goal
import com.flowtasks.app.domain.model.GoalStatus

object GoalMapper {
    fun toDomain(
        entity: GoalEntity,
        projectCount: Int = 0,
        totalTasksCount: Int = 0,
        completedTasksCount: Int = 0
    ): Goal {
        return Goal(
            id = entity.id,
            title = entity.title,
            description = entity.description,
            targetDate = entity.targetDate,
            status = try {
                GoalStatus.valueOf(entity.status)
            } catch (e: Exception) {
                GoalStatus.ACTIVE
            },
            colorHex = entity.colorHex,
            sortOrder = entity.sortOrder,
            createdAt = entity.createdAt,
            updatedAt = entity.updatedAt,
            projectCount = projectCount,
            totalTasksCount = totalTasksCount,
            completedTasksCount = completedTasksCount
        )
    }

    fun toEntity(domain: Goal): GoalEntity {
        return GoalEntity(
            id = domain.id,
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
