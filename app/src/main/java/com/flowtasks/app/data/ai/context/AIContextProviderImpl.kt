package com.flowtasks.app.data.ai.context

import com.flowtasks.app.domain.ai.AIContextProvider
import com.flowtasks.app.domain.ai.AIStructuredContext
import com.flowtasks.app.domain.repository.GoalRepository
import com.flowtasks.app.domain.repository.HabitRepository
import com.flowtasks.app.domain.repository.ProjectRepository
import com.flowtasks.app.domain.repository.TaskRepository
import com.flowtasks.app.domain.usecase.GetProductivityStatsUseCase
import kotlinx.coroutines.flow.first

/**
 * Concrete context extraction implementation.
 * Enforces data minimization by retrieving ONLY the single entity or metric snapshot
 * specifically requested by the consumer feature.
 */
class AIContextProviderImpl(
    private val taskRepository: TaskRepository,
    private val goalRepository: GoalRepository,
    private val projectRepository: ProjectRepository,
    private val habitRepository: HabitRepository,
    private val getProductivityStatsUseCase: GetProductivityStatsUseCase
) : AIContextProvider {

    override suspend fun getTaskContext(taskId: Long): AIStructuredContext.TaskContext? {
        val task = taskRepository.getTaskByIdDirect(taskId) ?: return null
        val subtasks = taskRepository.getSubtasksDirect(taskId)
        val subtaskTitles = subtasks.map { it.title }

        return AIStructuredContext.TaskContext(
            id = task.id,
            title = task.title,
            description = task.description,
            priority = task.priority.name,
            isCompleted = task.isCompleted,
            dueDate = task.dueDate,
            estimatedMinutes = task.estimatedDurationMinutes,
            subtaskTitles = subtaskTitles
        )
    }

    override suspend fun getGoalContext(goalId: Long): AIStructuredContext.GoalContext? {
        val goal = goalRepository.getGoalByIdDirect(goalId) ?: return null

        return AIStructuredContext.GoalContext(
            id = goal.id,
            title = goal.title,
            description = goal.description,
            targetDate = goal.targetDate,
            status = goal.status.name
        )
    }

    override suspend fun getProjectContext(projectId: Long): AIStructuredContext.ProjectContext? {
        val project = projectRepository.getProjectByIdDirect(projectId) ?: return null

        return AIStructuredContext.ProjectContext(
            id = project.id,
            title = project.title,
            description = project.description,
            colorHex = project.colorHex,
            status = project.status.name
        )
    }

    override suspend fun getHabitContext(habitId: Long): AIStructuredContext.HabitContext? {
        val habit = habitRepository.getHabitByIdDirect(habitId) ?: return null

        return AIStructuredContext.HabitContext(
            id = habit.id,
            title = habit.title,
            frequencyType = habit.frequencyType.name,
            currentStreak = habit.currentStreak,
            bestStreak = habit.bestStreak
        )
    }

    override suspend fun getProductivityContext(): AIStructuredContext.ProductivityContext {
        val stats = getProductivityStatsUseCase().first()

        return AIStructuredContext.ProductivityContext(
            completedToday = stats.completedToday,
            completedThisWeek = stats.completedThisWeek,
            focusMinutesThisWeek = stats.totalFocusSecondsThisWeek / 60,
            activeHabitsCount = stats.totalHabitsCount,
            longestStreak = stats.longestHabitStreak
        )
    }
}
