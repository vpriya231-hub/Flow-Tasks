package com.flowtasks.app.domain.ai

/**
 * Controlled context extraction layer for future AI features.
 * Enforces data minimization by fetching ONLY the specific entity or summary
 * explicitly requested by the user-initiated feature.
 */
interface AIContextProvider {

    /**
     * Extracts minimal structured context for a single task.
     */
    suspend fun getTaskContext(taskId: Long): AIStructuredContext.TaskContext?

    /**
     * Extracts minimal structured context for a single goal.
     */
    suspend fun getGoalContext(goalId: Long): AIStructuredContext.GoalContext?

    /**
     * Extracts minimal structured context for a single project.
     */
    suspend fun getProjectContext(projectId: Long): AIStructuredContext.ProjectContext?

    /**
     * Extracts minimal structured context for a single habit.
     */
    suspend fun getHabitContext(habitId: Long): AIStructuredContext.HabitContext?

    /**
     * Extracts a high-level productivity summary without raw task lists.
     */
    suspend fun getProductivityContext(): AIStructuredContext.ProductivityContext
}
