package com.flowtasks.app.domain.ai

/**
 * Provider-independent structured AI request model.
 */
data class AIRequest(
    val prompt: String,
    val systemInstruction: String? = null,
    val context: AIStructuredContext? = null,
    val config: AIConfig? = null,
    val metadata: Map<String, String> = emptyMap()
) {
    init {
        require(prompt.isNotBlank()) { "AI request prompt must not be blank." }
    }

    /**
     * Builds a sanitized text payload combining structured context and user prompt
     * in a standardized, provider-agnostic format.
     */
    fun buildCombinedPrompt(): String {
        if (context == null) return prompt

        val contextStr = when (context) {
            is AIStructuredContext.TaskContext -> {
                buildString {
                    appendLine("Task Context:")
                    appendLine("- Title: ${context.title}")
                    if (context.description.isNotBlank()) appendLine("- Description: ${context.description}")
                    appendLine("- Priority: ${context.priority}")
                    if (context.estimatedMinutes != null) appendLine("- Estimated Duration: ${context.estimatedMinutes} minutes")
                    if (context.subtaskTitles.isNotEmpty()) {
                        appendLine("- Existing Subtasks: ${context.subtaskTitles.joinToString(", ")}")
                    }
                }
            }
            is AIStructuredContext.GoalContext -> {
                buildString {
                    appendLine("Goal Context:")
                    appendLine("- Title: ${context.title}")
                    if (context.description.isNotBlank()) appendLine("- Description: ${context.description}")
                    appendLine("- Status: ${context.status}")
                }
            }
            is AIStructuredContext.ProjectContext -> {
                buildString {
                    appendLine("Project Context:")
                    appendLine("- Title: ${context.title}")
                    if (context.description.isNotBlank()) appendLine("- Description: ${context.description}")
                    appendLine("- Status: ${context.status}")
                }
            }
            is AIStructuredContext.HabitContext -> {
                buildString {
                    appendLine("Habit Context:")
                    appendLine("- Title: ${context.title}")
                    appendLine("- Frequency: ${context.frequencyType}")
                    appendLine("- Current Streak: ${context.currentStreak} days")
                }
            }
            is AIStructuredContext.ProductivityContext -> {
                buildString {
                    appendLine("Productivity Summary Context:")
                    appendLine("- Tasks Completed Today: ${context.completedToday}")
                    appendLine("- Tasks Completed This Week: ${context.completedThisWeek}")
                    appendLine("- Focus Minutes This Week: ${context.focusMinutesThisWeek}")
                    appendLine("- Active Habits: ${context.activeHabitsCount}")
                }
            }
            is AIStructuredContext.CustomContext -> {
                "${context.label}:\n${context.text}\n"
            }
        }

        return "$contextStr\n$prompt"
    }
}
