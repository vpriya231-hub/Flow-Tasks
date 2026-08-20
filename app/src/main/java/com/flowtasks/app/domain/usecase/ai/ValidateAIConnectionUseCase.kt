package com.flowtasks.app.domain.usecase.ai

import com.flowtasks.app.domain.ai.AIResult
import com.flowtasks.app.domain.ai.AIService

class ValidateAIConnectionUseCase(
    private val aiService: AIService
) {
    suspend operator fun invoke(): AIResult<Boolean> {
        return aiService.validateActiveKey()
    }
}
