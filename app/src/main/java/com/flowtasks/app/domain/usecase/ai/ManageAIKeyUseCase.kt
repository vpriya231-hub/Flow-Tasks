package com.flowtasks.app.domain.usecase.ai

import com.flowtasks.app.domain.ai.AIKeyManager
import com.flowtasks.app.domain.ai.AIProviderType

class ManageAIKeyUseCase(
    private val aiKeyManager: AIKeyManager
) {
    suspend fun saveKey(provider: AIProviderType, apiKey: String) {
        val trimmed = apiKey.trim()
        require(trimmed.isNotBlank()) { "API key cannot be empty." }
        aiKeyManager.setApiKey(provider, trimmed)
    }

    suspend fun getKey(provider: AIProviderType): String? {
        return aiKeyManager.getApiKey(provider)
    }

    suspend fun hasKey(provider: AIProviderType): Boolean {
        return aiKeyManager.hasApiKey(provider)
    }

    suspend fun removeKey(provider: AIProviderType) {
        aiKeyManager.deleteApiKey(provider)
    }

    suspend fun getMaskedKey(provider: AIProviderType): String? {
        return aiKeyManager.getMaskedApiKey(provider)
    }
}
