package com.flowtasks.app.domain.ai

/**
 * Provider-independent structured context interface for AI requests.
 * Enforces data minimization by allowing features to supply only the exact minimal
 * domain entity context required for a specific AI operation.
 */
sealed interface AIStructuredContext {

    /**
     * Minimal task context. Sent only when a task breakdown or optimization is explicitly requested.
     */
    data class TaskContext(
        val id: Long,
        val title: String,
        val description: String = "",
        val priority: String = "NONE",
        val isCompleted: Boolean = false,
        val dueDate: Long? = null,
        val estimatedMinutes: Int? = null,
        val subtaskTitles: List<String> = emptyList()
    ) : AIStructuredContext

    /**
     * Minimal goal context.
     */
    data class GoalContext(
        val id: Long,
        val title: String,
        val description: String = "",
        val targetDate: Long? = null,
        val status: String = "ACTIVE"
    ) : AIStructuredContext

    /**
     * Minimal project context.
     */
    data class ProjectContext(
        val id: Long,
        val title: String,
        val description: String = "",
        val colorHex: String = "",
        val status: String = "NOT_STARTED"
    ) : AIStructuredContext

    /**
     * Minimal habit context.
     */
    data class HabitContext(
        val id: Long,
        val title: String,
        val frequencyType: String = "DAILY",
        val currentStreak: Int = 0,
        val bestStreak: Int = 0
    ) : AIStructuredContext

    /**
     * Minimal productivity summary context (e.g. for weekly review or reflection).
     */
    data class ProductivityContext(
        val completedToday: Int,
        val completedThisWeek: Int,
        val focusMinutesThisWeek: Int,
        val activeHabitsCount: Int,
        val longestStreak: Int
    ) : AIStructuredContext

    /**
     * Generic text context payload.
     */
    data class CustomContext(
        val label: String,
        val text: String
    ) : AIStructuredContext
}
