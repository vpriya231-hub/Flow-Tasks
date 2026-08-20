package com.flowtasks.app.domain.ai

/**
 * Supported Gemini Model definitions (compatibility alias).
 */
typealias GeminiModelOption = AIModelOption

/**
 * Centralized, provider-agnostic AI configuration model.
 * Does not hardcode specific model names across consumers.
 */
data class AIConfig(
    val provider: AIProviderType = AIProviderType.GEMINI,
    val modelName: String = AIModelRegistry.getDefaultModelForProvider(AIProviderType.GEMINI),
    val temperature: Float? = null,
    val maxOutputTokens: Int? = null,
    val timeoutSeconds: Long = DEFAULT_TIMEOUT_SECONDS,
    val isEnabled: Boolean = true
) {
    companion object {
        const val MODEL_GEMINI_3_6_FLASH = AIModelRegistry.MODEL_GEMINI_3_6_FLASH
        const val MODEL_GEMINI_3_7_FLASH = AIModelRegistry.MODEL_GEMINI_3_7_FLASH
        const val MODEL_GEMINI_3_1_PRO = AIModelRegistry.MODEL_GEMINI_3_1_PRO

        const val DEFAULT_GEMINI_MODEL = AIModelRegistry.MODEL_GEMINI_3_6_FLASH
        const val DEFAULT_TIMEOUT_SECONDS = 30L
        const val MAX_TIMEOUT_SECONDS = 120L
        const val MIN_TIMEOUT_SECONDS = 5L

        val SUPPORTED_GEMINI_MODELS = AIModelRegistry.GEMINI_MODELS

        /**
         * Normalizes any obsolete or deprecated model IDs into a modern supported model ID for Gemini.
         */
        fun normalizeModelName(rawModelName: String?, provider: AIProviderType = AIProviderType.GEMINI): String {
            return AIModelRegistry.normalizeModelForProvider(provider, rawModelName)
        }
    }
}

