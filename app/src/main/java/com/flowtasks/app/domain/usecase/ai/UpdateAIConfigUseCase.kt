package com.flowtasks.app.domain.usecase.ai

import com.flowtasks.app.domain.ai.AIConfigRepository
import com.flowtasks.app.domain.ai.AIProviderType

class UpdateAIConfigUseCase(
    private val aiConfigRepository: AIConfigRepository
) {
    suspend fun setProvider(provider: AIProviderType) {
        aiConfigRepository.updateProvider(provider)
    }

    suspend fun setModel(modelName: String) {
        aiConfigRepository.updateModel(modelName)
    }

    suspend fun setTemperature(temperature: Float?) {
        aiConfigRepository.updateTemperature(temperature)
    }

    suspend fun setMaxTokens(maxTokens: Int?) {
        aiConfigRepository.updateMaxTokens(maxTokens)
    }

    suspend fun setTimeoutSeconds(timeoutSeconds: Long) {
        aiConfigRepository.updateTimeoutSeconds(timeoutSeconds)
    }

    suspend fun setEnabled(enabled: Boolean) {
        aiConfigRepository.setEnabled(enabled)
    }
}
