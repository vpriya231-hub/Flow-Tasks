package com.flowtasks.app.domain.usecase.ai

import com.flowtasks.app.domain.ai.AIError
import com.flowtasks.app.domain.ai.AIRequest
import com.flowtasks.app.domain.ai.AIResult
import com.flowtasks.app.domain.ai.AIService
import com.flowtasks.app.domain.model.Goal
import com.flowtasks.app.domain.model.Habit
import com.flowtasks.app.domain.model.ProductivityStats
import com.flowtasks.app.domain.model.Task
import org.json.JSONArray
import org.json.JSONObject

data class DailyPlanResult(
    val summary: String,
    val suggestedSchedule: List<String> = emptyList()
)

data class GoalBreakdownSuggestion(
    val milestoneProjects: List<String>,
    val actionableTasks: List<String>,
    val tips: String = ""
)

data class SuggestedHabit(
    val title: String,
    val frequency: String = "DAILY",
    val rationale: String = ""
)

class AIProductivityUseCase(
    private val aiService: AIService
) {

    suspend fun generateDailyPlan(
        tasks: List<Task> = emptyList(),
        habits: List<Habit> = emptyList()
    ): AIResult<DailyPlanResult> {
        val pendingTasks = tasks.filter { !it.isCompleted }
        val content = buildString {
            if (pendingTasks.isNotEmpty()) {
                appendLine("Today's Pending Tasks (${pendingTasks.size}):")
                pendingTasks.take(15).forEach {
                    appendLine("- [${it.priority.name}] ${it.title} ${if (it.dueTime != null) "at ${it.dueTime}" else ""}")
                }
            } else {
                appendLine("No specific tasks scheduled yet.")
            }
            if (habits.isNotEmpty()) {
                appendLine("\nActive Habits (${habits.size}):")
                habits.take(10).forEach {
                    appendLine("- ${it.title} (${it.frequencyType.name}, streak: ${it.currentStreak})")
                }
            }
        }

        val systemInstruction = """
            You are an elite productivity coach for Flow Tasks.
            Generate a motivating, clear, chronological daily schedule and strategy for today.
            Organize into:
            1. 🎯 High-Impact Focus Priorities
            2. ⏰ Recommended Time Blocks
            3. 💡 Strategy Tip for Deep Flow
        """.trimIndent()

        val request = AIRequest(
            prompt = "Plan my day based on:\n$content",
            systemInstruction = systemInstruction
        )

        return when (val result = aiService.generateTextDirect(request)) {
            is AIResult.Success -> {
                val lines = result.data.text.trim().lines().filter { it.isNotBlank() }
                AIResult.Success(
                    DailyPlanResult(
                        summary = result.data.text.trim(),
                        suggestedSchedule = lines.filter { it.startsWith("-") || it.startsWith("•") || it.matches("^\\d+\\..*".toRegex()) }
                    )
                )
            }
            is AIResult.Error -> AIResult.Error(result.error)
            is AIResult.Loading, is AIResult.Idle -> AIResult.Error(AIError.Unknown())
        }
    }

    suspend fun generateProductivitySummary(stats: ProductivityStats = ProductivityStats()): AIResult<String> {
        val focusMins = stats.totalFocusSecondsThisWeek / 60
        val systemInstruction = """
            You are a supportive productivity analyst for Flow Tasks.
            Analyze the user's weekly metrics:
            - Tasks completed this week: ${stats.completedThisWeek}
            - Total tasks all time: ${stats.totalTasksCount}
            - Completion rate: ${stats.completionRatePercentage}%
            - Focus minutes this week: ${focusMins}m
            - Longest Habit Streak: ${stats.longestHabitStreak} days
            Provide an inspiring 2-paragraph evaluation with 1 actionable optimization for tomorrow.
        """.trimIndent()

        val request = AIRequest(
            prompt = "Analyze my productivity statistics and give actionable guidance.",
            systemInstruction = systemInstruction
        )

        return when (val result = aiService.generateTextDirect(request)) {
            is AIResult.Success -> AIResult.Success(result.data.text.trim())
            is AIResult.Error -> AIResult.Error(result.error)
            is AIResult.Loading, is AIResult.Idle -> AIResult.Error(AIError.Unknown())
        }
    }

    suspend fun generateGoalBreakdown(goal: Goal): AIResult<GoalBreakdownSuggestion> {
        val systemInstruction = """
            You are an expert strategic planner for Flow Tasks.
            Break down the provided long-term goal into practical milestone projects and actionable initial tasks.
            Return ONLY a valid JSON object with the following schema:
            {
              "milestoneProjects": ["Phase 1: ...", "Phase 2: ...", "Phase 3: ..."],
              "actionableTasks": ["Task 1", "Task 2", "Task 3", "Task 4", "Task 5"],
              "tips": "One strategic piece of advice for achieving this goal."
            }
        """.trimIndent()

        val content = "Goal: ${goal.title}\nDescription: ${goal.description}"
        val request = AIRequest(
            prompt = "Break down this goal:\n$content",
            systemInstruction = systemInstruction
        )

        return when (val result = aiService.generateTextDirect(request)) {
            is AIResult.Success -> {
                val parsed = parseGoalBreakdown(result.data.text, goal.title)
                AIResult.Success(parsed)
            }
            is AIResult.Error -> AIResult.Error(result.error)
            is AIResult.Loading, is AIResult.Idle -> AIResult.Error(AIError.Unknown())
        }
    }

    suspend fun suggestHabits(goals: List<Goal>): AIResult<List<SuggestedHabit>> {
        val goalsSummary = if (goals.isEmpty()) {
            "General personal productivity, health, and focus"
        } else {
            goals.joinToString("; ") { "${it.title}: ${it.description}" }
        }

        val systemInstruction = """
            You are an expert behavioral scientist and habit designer for Flow Tasks.
            Suggest 3 to 5 keystone habits that directly support the user's goals.
            Return ONLY a valid JSON array of objects with the schema:
            [
              {
                "title": "Clear atomic habit name",
                "frequency": "DAILY" | "WEEKLY",
                "rationale": "Brief 1-sentence reason why this habit builds momentum."
              }
            ]
        """.trimIndent()

        val request = AIRequest(
            prompt = "Suggest habits for goals: $goalsSummary",
            systemInstruction = systemInstruction
        )

        return when (val result = aiService.generateTextDirect(request)) {
            is AIResult.Success -> {
                val parsed = parseSuggestedHabits(result.data.text)
                AIResult.Success(parsed)
            }
            is AIResult.Error -> AIResult.Error(result.error)
            is AIResult.Loading, is AIResult.Idle -> AIResult.Error(AIError.Unknown())
        }
    }

    private fun extractJsonString(raw: String): String {
        val trimmed = raw.trim()
        val jsonBlockRegex = "```(?:json)?\\s*([\\s\\S]*?)\\s*```".toRegex()
        val match = jsonBlockRegex.find(trimmed)
        if (match != null) {
            return match.groupValues[1].trim()
        }
        val firstBrace = trimmed.indexOf('{')
        val lastBrace = trimmed.lastIndexOf('}')
        if (firstBrace != -1 && lastBrace != -1 && lastBrace > firstBrace) {
            return trimmed.substring(firstBrace, lastBrace + 1)
        }
        val firstBracket = trimmed.indexOf('[')
        val lastBracket = trimmed.lastIndexOf(']')
        if (firstBracket != -1 && lastBracket != -1 && lastBracket > firstBracket) {
            return trimmed.substring(firstBracket, lastBracket + 1)
        }
        return trimmed
    }

    private fun parseGoalBreakdown(raw: String, fallbackGoalTitle: String): GoalBreakdownSuggestion {
        val jsonStr = extractJsonString(raw)
        return try {
            val json = JSONObject(jsonStr)
            val projects = mutableListOf<String>()
            val tasks = mutableListOf<String>()

            val projectsArray = json.optJSONArray("milestoneProjects")
            if (projectsArray != null) {
                for (i in 0 until projectsArray.length()) {
                    projects.add(projectsArray.optString(i))
                }
            }

            val tasksArray = json.optJSONArray("actionableTasks")
            if (tasksArray != null) {
                for (i in 0 until tasksArray.length()) {
                    tasks.add(tasksArray.optString(i))
                }
            }

            val tips = json.optString("tips", "")

            GoalBreakdownSuggestion(
                milestoneProjects = projects.ifEmpty { listOf("Phase 1: Foundation", "Phase 2: Execution", "Phase 3: Review") },
                actionableTasks = tasks.ifEmpty { listOf("Define initial milestones", "Gather resources", "Execute first sprint") },
                tips = tips
            )
        } catch (e: Exception) {
            GoalBreakdownSuggestion(
                milestoneProjects = listOf("Phase 1: Research", "Phase 2: Implementation"),
                actionableTasks = listOf("Define requirements for $fallbackGoalTitle", "Create project schedule"),
                tips = raw.take(200)
            )
        }
    }

    private fun parseSuggestedHabits(raw: String): List<SuggestedHabit> {
        val jsonStr = extractJsonString(raw)
        val habits = mutableListOf<SuggestedHabit>()
        try {
            val array = JSONArray(jsonStr)
            for (i in 0 until array.length()) {
                val item = array.optJSONObject(i) ?: continue
                val title = item.optString("title", "")
                val freq = item.optString("frequency", "DAILY")
                val rationale = item.optString("rationale", "")
                if (title.isNotBlank()) {
                    habits.add(SuggestedHabit(title = title, frequency = freq, rationale = rationale))
                }
            }
        } catch (e: Exception) {
            // Fallback default habits
            habits.add(SuggestedHabit("Daily 10-minute planning", "DAILY", "Sets daily focus"))
            habits.add(SuggestedHabit("Deep work block (45m)", "DAILY", "Builds sustained momentum"))
        }
        return habits.ifEmpty {
            listOf(
                SuggestedHabit("Daily Morning Planning", "DAILY", "Clarifies priority tasks before starting work."),
                SuggestedHabit("Evening Review & Shutdown", "DAILY", "Ensures all tasks are updated and tracked.")
            )
        }
    }
}

