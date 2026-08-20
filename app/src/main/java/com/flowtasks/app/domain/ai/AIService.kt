package com.flowtasks.app.domain.ai

import kotlinx.coroutines.flow.Flow

/**
 * Centralized AI coordinator service for Flow Tasks.
 * Orchestrates provider resolution, key retrieval, timeouts, data minimization,
 * error handling, and smart retry policies.
 */
interface AIService {

    /**
     * Observable flow of the active configuration.
     */
    val activeConfig: Flow<AIConfig>

    /**
     * Checks if the active AI provider is configured and available with a valid key.
     */
    suspend fun isAvailable(): Boolean

    /**
     * Executes a text generation request and emits reactive state flow (Loading -> Success/Error).
     */
    fun generateText(request: AIRequest): Flow<AIResult<AIResponse>>

    /**
     * Direct suspend execution of a text generation request.
     */
    suspend fun generateTextDirect(request: AIRequest): AIResult<AIResponse>

    /**
     * Validates the currently configured API key against the active provider.
     */
    suspend fun validateActiveKey(): AIResult<Boolean>
}
