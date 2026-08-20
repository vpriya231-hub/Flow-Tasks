package com.flowtasks.app.domain.ai

/**
 * Provider interface contract for pluggable AI services (Gemini, OpenAI, Anthropic, etc.).
 * Implementations execute generation requests using the provided secure API key.
 */
interface AIProvider {
    val providerType: AIProviderType

    /**
     * Executes a text generation request with the given decrypted API key.
     */
    suspend fun generateText(
        request: AIRequest,
        apiKey: String
    ): AIResult<AIResponse>

    /**
     * Validates if the provided API key is authorized by making a minimal verification request.
     */
    suspend fun validateKey(
        apiKey: String,
        modelName: String? = null
    ): AIResult<Boolean>
}
