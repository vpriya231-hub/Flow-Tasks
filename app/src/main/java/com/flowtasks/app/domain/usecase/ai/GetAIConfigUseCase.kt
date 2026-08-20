package com.flowtasks.app.domain.usecase.ai

import com.flowtasks.app.domain.ai.AIConfig
import com.flowtasks.app.domain.ai.AIConfigRepository
import kotlinx.coroutines.flow.Flow

class GetAIConfigUseCase(
    private val aiConfigRepository: AIConfigRepository
) {
    operator fun invoke(): Flow<AIConfig> {
        return aiConfigRepository.aiConfigFlow
    }

    suspend fun getActive(): AIConfig {
        return aiConfigRepository.getActiveConfig()
    }
}
