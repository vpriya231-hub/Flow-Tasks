package com.flowtasks.app.domain.ai

/**
 * Model option descriptor for UI chips, settings, and provider dispatch.
 */
data class AIModelOption(
    val id: String,
    val displayName: String,
    val provider: AIProviderType,
    val defaultThinkingLevel: String? = null
)

/**
 * Dedicated registry for model options per provider.
 * Enforces provider -> model isolation and prevents cross-provider model leakage.
 */
object AIModelRegistry {
    // -------------------------------------------------------------
    // Gemini Models
    // -------------------------------------------------------------
    const val MODEL_GEMINI_3_6_FLASH = "gemini-3.6-flash"
    const val MODEL_GEMINI_3_7_FLASH = "gemini-3.7-flash"
    const val MODEL_GEMINI_3_1_PRO = "gemini-3.1-pro-preview"

    val GEMINI_MODELS = listOf(
        AIModelOption(
            id = MODEL_GEMINI_3_6_FLASH,
            displayName = "Gemini 3.6 Flash",
            provider = AIProviderType.GEMINI
        ),
        AIModelOption(
            id = MODEL_GEMINI_3_7_FLASH,
            displayName = "Gemini 3.7 Flash",
            provider = AIProviderType.GEMINI,
            defaultThinkingLevel = "medium"
        ),
        AIModelOption(
            id = MODEL_GEMINI_3_1_PRO,
            displayName = "Gemini 3.1 Pro",
            provider = AIProviderType.GEMINI
        )
    )

    // -------------------------------------------------------------
    // OpenAI Models
    // -------------------------------------------------------------
    const val MODEL_OPENAI_GPT_5 = "gpt-5"
    const val MODEL_OPENAI_GPT_5_MINI = "gpt-5-mini"

    val OPENAI_MODELS = listOf(
        AIModelOption(
            id = MODEL_OPENAI_GPT_5,
            displayName = "GPT-5",
            provider = AIProviderType.OPENAI
        ),
        AIModelOption(
            id = MODEL_OPENAI_GPT_5_MINI,
            displayName = "GPT-5 mini",
            provider = AIProviderType.OPENAI
        )
    )

    // -------------------------------------------------------------
    // Anthropic Models
    // -------------------------------------------------------------
    const val MODEL_CLAUDE_3_7_SONNET = "claude-3-7-sonnet-20250219"
    const val MODEL_CLAUDE_3_5_SONNET = "claude-3-5-sonnet-20241022"
    const val MODEL_CLAUDE_3_5_HAIKU = "claude-3-5-haiku-20241022"

    val ANTHROPIC_MODELS = listOf(
        AIModelOption(
            id = MODEL_CLAUDE_3_7_SONNET,
            displayName = "Claude 3.7 Sonnet",
            provider = AIProviderType.ANTHROPIC
        ),
        AIModelOption(
            id = MODEL_CLAUDE_3_5_SONNET,
            displayName = "Claude 3.5 Sonnet",
            provider = AIProviderType.ANTHROPIC
        ),
        AIModelOption(
            id = MODEL_CLAUDE_3_5_HAIKU,
            displayName = "Claude 3.5 Haiku",
            provider = AIProviderType.ANTHROPIC
        )
    )

    // -------------------------------------------------------------
    // Custom Provider Default
    // -------------------------------------------------------------
    const val MODEL_CUSTOM_DEFAULT = "custom-model"

    /**
     * Returns the list of preset models for the given provider.
     * Returns an empty list for CUSTOM provider since it uses free-form text input.
     */
    fun getModelsForProvider(provider: AIProviderType): List<AIModelOption> {
        return when (provider) {
            AIProviderType.GEMINI -> GEMINI_MODELS
            AIProviderType.OPENAI -> OPENAI_MODELS
            AIProviderType.ANTHROPIC -> ANTHROPIC_MODELS
            AIProviderType.CUSTOM -> emptyList()
        }
    }

    /**
     * Returns the default model ID for a given provider.
     */
    fun getDefaultModelForProvider(provider: AIProviderType): String {
        return when (provider) {
            AIProviderType.GEMINI -> MODEL_GEMINI_3_6_FLASH
            AIProviderType.OPENAI -> MODEL_OPENAI_GPT_5
            AIProviderType.ANTHROPIC -> MODEL_CLAUDE_3_7_SONNET
            AIProviderType.CUSTOM -> MODEL_CUSTOM_DEFAULT
        }
    }

    /**
     * Normalizes a model name to ensure it is valid and compatible with the given provider.
     * Prevents cross-provider model leakage and migrates deprecated model names.
     */
    fun normalizeModelForProvider(provider: AIProviderType, rawModelName: String?): String {
        if (rawModelName.isNullOrBlank()) {
            return getDefaultModelForProvider(provider)
        }
        val trimmed = rawModelName.trim()

        return when (provider) {
            AIProviderType.GEMINI -> {
                val matched = GEMINI_MODELS.firstOrNull { it.id.equals(trimmed, ignoreCase = true) }
                if (matched != null) {
                    matched.id
                } else {
                    // Deprecated / unrecognized Gemini models migrate to default 3.6 flash
                    MODEL_GEMINI_3_6_FLASH
                }
            }
            AIProviderType.OPENAI -> {
                val matched = OPENAI_MODELS.firstOrNull { it.id.equals(trimmed, ignoreCase = true) }
                if (matched != null) {
                    matched.id
                } else {
                    MODEL_OPENAI_GPT_5
                }
            }
            AIProviderType.ANTHROPIC -> {
                val matched = ANTHROPIC_MODELS.firstOrNull { it.id.equals(trimmed, ignoreCase = true) }
                if (matched != null) {
                    matched.id
                } else {
                    MODEL_CLAUDE_3_7_SONNET
                }
            }
            AIProviderType.CUSTOM -> {
                trimmed.ifBlank { MODEL_CUSTOM_DEFAULT }
            }
        }
    }
}
