package com.flowtasks.app.data.mapper

import com.flowtasks.app.core.utils.HabitCalculatedMetrics
import com.flowtasks.app.core.utils.HabitCalculator
import com.flowtasks.app.data.local.entity.HabitCompletionEntity
import com.flowtasks.app.data.local.entity.HabitEntity
import com.flowtasks.app.domain.model.Habit
import com.flowtasks.app.domain.model.HabitCompletion
import com.flowtasks.app.domain.model.HabitFrequencyType
import com.flowtasks.app.domain.model.HabitStatus

object HabitMapper {
    fun toDomain(
        entity: HabitEntity,
        metrics: HabitCalculatedMetrics? = null
    ): Habit {
        return Habit(
            id = entity.id,
            title = entity.title,
            description = entity.description,
            frequencyType = try {
                HabitFrequencyType.valueOf(entity.frequencyType)
            } catch (e: Exception) {
                HabitFrequencyType.DAILY
            },
            frequencyDays = HabitCalculator.parseDaysOfWeek(entity.frequencyDays),
            targetCountPerPeriod = entity.targetCountPerPeriod,
            status = try {
                HabitStatus.valueOf(entity.status)
            } catch (e: Exception) {
                HabitStatus.ACTIVE
            },
            colorHex = entity.colorHex,
            sortOrder = entity.sortOrder,
            createdAt = entity.createdAt,
            updatedAt = entity.updatedAt,
            isCompletedToday = metrics?.isCompletedToday ?: false,
            currentStreak = metrics?.currentStreak ?: 0,
            bestStreak = metrics?.bestStreak ?: 0,
            totalCompletions = metrics?.totalCompletions ?: 0,
            weeklyHistory = metrics?.weeklyHistory ?: emptyList()
        )
    }

    fun toEntity(domain: Habit): HabitEntity {
        return HabitEntity(
            id = domain.id,
            title = domain.title,
            description = domain.description,
            frequencyType = domain.frequencyType.name,
            frequencyDays = HabitCalculator.serializeDaysOfWeek(domain.frequencyDays),
            targetCountPerPeriod = domain.targetCountPerPeriod,
            status = domain.status.name,
            colorHex = domain.colorHex,
            sortOrder = domain.sortOrder,
            createdAt = domain.createdAt,
            updatedAt = domain.updatedAt
        )
    }

    fun toDomain(entity: HabitCompletionEntity): HabitCompletion {
        return HabitCompletion(
            id = entity.id,
            habitId = entity.habitId,
            completedDate = entity.completedDate,
            completedAt = entity.completedAt,
            notes = entity.notes
        )
    }

    fun toEntity(domain: HabitCompletion): HabitCompletionEntity {
        return HabitCompletionEntity(
            id = domain.id,
            habitId = domain.habitId,
            completedDate = domain.completedDate,
            completedAt = domain.completedAt,
            notes = domain.notes
        )
    }
}
