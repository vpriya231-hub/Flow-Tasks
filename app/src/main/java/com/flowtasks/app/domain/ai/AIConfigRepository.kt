package com.flowtasks.app.domain.ai

import kotlinx.coroutines.flow.Flow

/**
 * Repository interface for managing local AI user preferences (active provider, model, enabled state).
 */
interface AIConfigRepository {
    val aiConfigFlow: Flow<AIConfig>

    suspend fun getActiveConfig(): AIConfig
    suspend fun updateProvider(provider: AIProviderType)
    suspend fun updateModel(modelName: String)
    suspend fun updateTemperature(temperature: Float?)
    suspend fun updateMaxTokens(maxTokens: Int?)
    suspend fun updateTimeoutSeconds(timeoutSeconds: Long)
    suspend fun setEnabled(enabled: Boolean)
}
