package com.flowtasks.app.domain.usecase.ai

import com.flowtasks.app.domain.ai.AIRequest
import com.flowtasks.app.domain.ai.AIResponse
import com.flowtasks.app.domain.ai.AIResult
import com.flowtasks.app.domain.ai.AIService
import kotlinx.coroutines.flow.Flow

class GenerateAITextUseCase(
    private val aiService: AIService
) {
    operator fun invoke(request: AIRequest): Flow<AIResult<AIResponse>> {
        return aiService.generateText(request)
    }

    suspend fun direct(request: AIRequest): AIResult<AIResponse> {
        return aiService.generateTextDirect(request)
    }
}
