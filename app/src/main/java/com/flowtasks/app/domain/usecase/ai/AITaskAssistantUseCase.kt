package com.flowtasks.app.domain.usecase.ai

import com.flowtasks.app.domain.ai.AIError
import com.flowtasks.app.domain.ai.AIRequest
import com.flowtasks.app.domain.ai.AIResult
import com.flowtasks.app.domain.ai.AIService
import com.flowtasks.app.domain.model.TaskPriority
import org.json.JSONArray
import org.json.JSONObject

data class GeneratedTaskSuggestion(
    val title: String,
    val description: String = "",
    val priority: TaskPriority = TaskPriority.NONE,
    val estimatedMinutes: Int? = null
)

data class ImprovedTaskSuggestion(
    val title: String,
    val description: String = "",
    val suggestedNotes: String = ""
)

data class PrioritySuggestion(
    val priority: TaskPriority = TaskPriority.NONE,
    val estimatedMinutes: Int? = null,
    val reasoning: String = ""
)

class AITaskAssistantUseCase(
    private val aiService: AIService
) {

    suspend fun generateTaskFromPrompt(prompt: String): AIResult<GeneratedTaskSuggestion> {
        val systemInstruction = """
            You are an expert productivity assistant for Flow Tasks.
            Given a user's task prompt, generate a structured task proposal in valid JSON format.
            Return ONLY a valid JSON object with the following schema:
            {
              "title": "Clear, actionable, concise task title",
              "description": "Helpful details, checklist items or context",
              "priority": "HIGH" | "MEDIUM" | "LOW" | "NONE",
              "estimatedMinutes": integer or null
            }
        """.trimIndent()

        val request = AIRequest(
            prompt = "Generate task for: $prompt",
            systemInstruction = systemInstruction
        )

        return when (val result = aiService.generateTextDirect(request)) {
            is AIResult.Success -> {
                val parsed = parseGeneratedTask(result.data.text, prompt)
                AIResult.Success(parsed)
            }
            is AIResult.Error -> AIResult.Error(result.error)
            is AIResult.Loading, is AIResult.Idle -> AIResult.Error(AIError.Unknown())
        }
    }

    suspend fun improveTask(title: String, description: String, notes: String): AIResult<ImprovedTaskSuggestion> {
        val systemInstruction = """
            You are an expert productivity assistant for Flow Tasks.
            Analyze the existing task title and description, and refine them into a clearer, more actionable format.
            Return ONLY a valid JSON object with the following schema:
            {
              "title": "Refined, professional and actionable title",
              "description": "Clear step-by-step description or breakdown",
              "suggestedNotes": "Helpful tips, resources, or follow-ups"
            }
        """.trimIndent()

        val content = buildString {
            appendLine("Task Title: $title")
            if (description.isNotBlank()) appendLine("Current Description: $description")
            if (notes.isNotBlank()) appendLine("Current Notes: $notes")
        }

        val request = AIRequest(
            prompt = "Improve and refine this task:\n$content",
            systemInstruction = systemInstruction
        )

        return when (val result = aiService.generateTextDirect(request)) {
            is AIResult.Success -> {
                val parsed = parseImprovedTask(result.data.text, title, description)
                AIResult.Success(parsed)
            }
            is AIResult.Error -> AIResult.Error(result.error)
            is AIResult.Loading, is AIResult.Idle -> AIResult.Error(AIError.Unknown())
        }
    }

    suspend fun generateSubtasks(title: String, description: String): AIResult<List<String>> {
        val systemInstruction = """
            You are an expert productivity assistant for Flow Tasks.
            Break down the provided task into 3 to 6 concise, actionable subtask action items.
            Return ONLY a valid JSON array of strings, e.g. ["Step 1", "Step 2", "Step 3"].
        """.trimIndent()

        val content = buildString {
            appendLine("Task Title: $title")
            if (description.isNotBlank()) appendLine("Task Description: $description")
        }

        val request = AIRequest(
            prompt = "Break down into subtasks:\n$content",
            systemInstruction = systemInstruction
        )

        return when (val result = aiService.generateTextDirect(request)) {
            is AIResult.Success -> {
                val parsed = parseSubtasks(result.data.text)
                AIResult.Success(parsed)
            }
            is AIResult.Error -> AIResult.Error(result.error)
            is AIResult.Loading, is AIResult.Idle -> AIResult.Error(AIError.Unknown())
        }
    }

    suspend fun suggestPriorityAndDuration(title: String, description: String, dueDate: Long?): AIResult<PrioritySuggestion> {
        val systemInstruction = """
            You are an expert productivity assistant for Flow Tasks.
            Evaluate the urgency, importance, and typical effort for this task.
            Return ONLY a valid JSON object with the following schema:
            {
              "priority": "HIGH" | "MEDIUM" | "LOW" | "NONE",
              "estimatedMinutes": integer,
              "reasoning": "One short sentence explaining why this priority and duration make sense."
            }
        """.trimIndent()

        val content = buildString {
            appendLine("Task Title: $title")
            if (description.isNotBlank()) appendLine("Task Description: $description")
            if (dueDate != null) appendLine("Due Date timestamp: $dueDate")
        }

        val request = AIRequest(
            prompt = "Suggest priority and duration:\n$content",
            systemInstruction = systemInstruction
        )

        return when (val result = aiService.generateTextDirect(request)) {
            is AIResult.Success -> {
                val parsed = parsePrioritySuggestion(result.data.text)
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

    private fun parseGeneratedTask(raw: String, fallbackTitle: String): GeneratedTaskSuggestion {
        val jsonStr = extractJsonString(raw)
        return try {
            val json = JSONObject(jsonStr)
            val title = json.optString("title", fallbackTitle).ifBlank { fallbackTitle }
            val description = json.optString("description", "")
            val priorityStr = json.optString("priority", "NONE")
            val priority = runCatching { TaskPriority.valueOf(priorityStr.uppercase()) }.getOrDefault(TaskPriority.NONE)
            val estimated = if (json.has("estimatedMinutes") && !json.isNull("estimatedMinutes")) {
                json.optInt("estimatedMinutes", 0).takeIf { it > 0 }
            } else null

            GeneratedTaskSuggestion(
                title = title,
                description = description,
                priority = priority,
                estimatedMinutes = estimated
            )
        } catch (e: Exception) {
            GeneratedTaskSuggestion(
                title = fallbackTitle,
                description = raw.take(200)
            )
        }
    }

    private fun parseImprovedTask(raw: String, fallbackTitle: String, fallbackDesc: String): ImprovedTaskSuggestion {
        val jsonStr = extractJsonString(raw)
        return try {
            val json = JSONObject(jsonStr)
            val title = json.optString("title", fallbackTitle).ifBlank { fallbackTitle }
            val description = json.optString("description", fallbackDesc)
            val suggestedNotes = json.optString("suggestedNotes", "")

            ImprovedTaskSuggestion(
                title = title,
                description = description,
                suggestedNotes = suggestedNotes
            )
        } catch (e: Exception) {
            ImprovedTaskSuggestion(
                title = fallbackTitle,
                description = raw
            )
        }
    }

    private fun parseSubtasks(raw: String): List<String> {
        val jsonStr = extractJsonString(raw)
        val resultList = mutableListOf<String>()
        try {
            val array = JSONArray(jsonStr)
            for (i in 0 until array.length()) {
                val item = array.optString(i)
                if (item.isNotBlank()) {
                    resultList.add(item.trim())
                }
            }
        } catch (e: Exception) {
            // Fallback line parsing
            raw.lines().forEach { line ->
                val cleaned = line.trim().removePrefix("- ").removePrefix("* ").replace("^\\d+\\.\\s*".toRegex(), "").trim()
                if (cleaned.isNotBlank() && !cleaned.startsWith("[") && !cleaned.startsWith("]")) {
                    resultList.add(cleaned)
                }
            }
        }
        return resultList.ifEmpty { listOf("Review requirements", "Execute plan", "Verify results") }
    }

    private fun parsePrioritySuggestion(raw: String): PrioritySuggestion {
        val jsonStr = extractJsonString(raw)
        return try {
            val json = JSONObject(jsonStr)
            val priorityStr = json.optString("priority", "NONE")
            val priority = runCatching { TaskPriority.valueOf(priorityStr.uppercase()) }.getOrDefault(TaskPriority.MEDIUM)
            val estimated = if (json.has("estimatedMinutes") && !json.isNull("estimatedMinutes")) {
                json.optInt("estimatedMinutes", 30).takeIf { it > 0 }
            } else 30
            val reasoning = json.optString("reasoning", "Suggested based on task scope.")

            PrioritySuggestion(
                priority = priority,
                estimatedMinutes = estimated,
                reasoning = reasoning
            )
        } catch (e: Exception) {
            PrioritySuggestion(
                priority = TaskPriority.MEDIUM,
                estimatedMinutes = 30,
                reasoning = "Standard task estimate."
            )
        }
    }
}
