package com.flowtasks.app.domain.ai

/**
 * Token usage metadata for an AI generation request.
 */
data class AIUsageMetadata(
    val promptTokens: Int = 0,
    val candidateTokens: Int = 0,
    val totalTokens: Int = 0
)

/**
 * Provider-independent structured AI response model.
 */
data class AIResponse(
    val text: String,
    val isSuccess: Boolean = true,
    val finishReason: String? = null,
    val usageMetadata: AIUsageMetadata? = null,
    val provider: AIProviderType = AIProviderType.GEMINI,
    val model: String = "",
    val timestamp: Long = System.currentTimeMillis()
)
