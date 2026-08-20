package com.flowtasks.app.domain.usecase

import com.flowtasks.app.domain.model.Goal
import com.flowtasks.app.domain.repository.GoalRepository
import kotlinx.coroutines.flow.Flow

class GetGoalsUseCase(
    private val goalRepository: GoalRepository
) {
    operator fun invoke(includeArchived: Boolean = false): Flow<List<Goal>> {
        return goalRepository.getGoals(includeArchived)
    }
}

class CreateGoalUseCase(
    private val goalRepository: GoalRepository
) {
    suspend operator fun invoke(
        title: String,
        description: String = "",
        targetDate: Long? = null,
        colorHex: String = "#4F46E5"
    ): Long {
        if (title.isBlank()) return -1
        val goal = Goal(
            title = title.trim(),
            description = description.trim(),
            targetDate = targetDate,
            colorHex = colorHex
        )
        return goalRepository.createGoal(goal)
    }
}

class UpdateGoalUseCase(
    private val goalRepository: GoalRepository
) {
    suspend operator fun invoke(goal: Goal) {
        if (goal.title.isBlank()) return
        goalRepository.updateGoal(goal)
    }
}

class DeleteGoalUseCase(
    private val goalRepository: GoalRepository
) {
    suspend operator fun invoke(goalId: Long) {
        goalRepository.deleteGoal(goalId)
    }
}

class ToggleGoalStatusUseCase(
    private val goalRepository: GoalRepository
) {
    suspend operator fun invoke(goalId: Long, isCompleted: Boolean) {
        goalRepository.updateGoalStatus(goalId, isCompleted)
    }
}
