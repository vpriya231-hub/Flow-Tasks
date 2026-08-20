package com.flowtasks.app.domain.usecase.ai

import com.flowtasks.app.domain.ai.AIContextProvider
import com.flowtasks.app.domain.ai.AIStructuredContext

class GetAIContextUseCase(
    private val aiContextProvider: AIContextProvider
) {
    suspend fun forTask(taskId: Long): AIStructuredContext.TaskContext? {
        return aiContextProvider.getTaskContext(taskId)
    }

    suspend fun forGoal(goalId: Long): AIStructuredContext.GoalContext? {
        return aiContextProvider.getGoalContext(goalId)
    }

    suspend fun forProject(projectId: Long): AIStructuredContext.ProjectContext? {
        return aiContextProvider.getProjectContext(projectId)
    }

    suspend fun forHabit(habitId: Long): AIStructuredContext.HabitContext? {
        return aiContextProvider.getHabitContext(habitId)
    }

    suspend fun forProductivitySummary(): AIStructuredContext.ProductivityContext {
        return aiContextProvider.getProductivityContext()
    }
}
