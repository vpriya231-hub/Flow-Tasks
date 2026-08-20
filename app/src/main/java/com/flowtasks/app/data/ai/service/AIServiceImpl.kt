package com.flowtasks.app.data.ai.service

import com.flowtasks.app.domain.ai.AIConfig
import com.flowtasks.app.domain.ai.AIConfigRepository
import com.flowtasks.app.domain.ai.AIError
import com.flowtasks.app.domain.ai.AIKeyManager
import com.flowtasks.app.domain.ai.AIProvider
import com.flowtasks.app.domain.ai.AIProviderType
import com.flowtasks.app.domain.ai.AIRequest
import com.flowtasks.app.domain.ai.AIResponse
import com.flowtasks.app.domain.ai.AIResult
import com.flowtasks.app.domain.ai.AIService
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.withTimeoutOrNull

/**
 * Centralized AI Coordinator Service implementation.
 * Manages provider resolution, secure key injection, request timeouts,
 * cooperative cancellation, and smart transient retry policies.
 */
class AIServiceImpl(
    private val aiConfigRepository: AIConfigRepository,
    private val aiKeyManager: AIKeyManager,
    private val providers: Map<AIProviderType, AIProvider>
) : AIService {

    companion object {
        private const val MAX_RETRY_ATTEMPTS = 2
        private const val INITIAL_RETRY_BACKOFF_MS = 500L
    }

    override val activeConfig: Flow<AIConfig> = aiConfigRepository.aiConfigFlow

    override suspend fun isAvailable(): Boolean {
        val config = aiConfigRepository.getActiveConfig()
        if (!config.isEnabled) return false
        return aiKeyManager.hasApiKey(config.provider)
    }

    override fun generateText(request: AIRequest): Flow<AIResult<AIResponse>> = flow {
        emit(AIResult.Loading)
        val result = generateTextDirect(request)
        emit(result)
    }

    override suspend fun generateTextDirect(request: AIRequest): AIResult<AIResponse> {
        val config = aiConfigRepository.getActiveConfig()

        if (!config.isEnabled) {
            return AIResult.Error(AIError.ProviderDisabled())
        }

        val effectiveConfig = request.config ?: config
        val providerType = effectiveConfig.provider
        val provider = providers[providerType]
            ?: return AIResult.Error(AIError.Unknown("Provider $providerType is not supported in this build."))

        val apiKey = aiKeyManager.getApiKey(providerType)
        if (apiKey.isNullOrBlank()) {
            return AIResult.Error(AIError.MissingApiKey())
        }

        val timeoutMillis = effectiveConfig.timeoutSeconds * 1000L

        return executeWithRetryAndTimeout(
            request = request.copy(config = effectiveConfig),
            apiKey = apiKey,
            provider = provider,
            timeoutMillis = timeoutMillis
        )
    }

    override suspend fun validateActiveKey(): AIResult<Boolean> {
        val config = aiConfigRepository.getActiveConfig()
        val provider = providers[config.provider]
            ?: return AIResult.Error(AIError.Unknown("Provider ${config.provider} is not registered."))

        val apiKey = aiKeyManager.getApiKey(config.provider)
        if (apiKey.isNullOrBlank()) {
            return AIResult.Error(AIError.MissingApiKey())
        }

        return try {
            provider.validateKey(apiKey, config.modelName)
        } catch (e: CancellationException) {
            AIResult.Error(AIError.RequestCancelled())
        } catch (e: Exception) {
            AIResult.Error(AIError.Unknown(cause = e))
        }
    }

    private suspend fun executeWithRetryAndTimeout(
        request: AIRequest,
        apiKey: String,
        provider: AIProvider,
        timeoutMillis: Long
    ): AIResult<AIResponse> {
        var currentAttempt = 0
        var backoffMs = INITIAL_RETRY_BACKOFF_MS
        var lastError: AIError? = null

        while (currentAttempt <= MAX_RETRY_ATTEMPTS) {
            try {
                val executionResult = withTimeoutOrNull(timeoutMillis) {
                    provider.generateText(request, apiKey)
                }

                if (executionResult == null) {
                    // Timed out
                    val timeoutError = AIError.Timeout()
                    if (currentAttempt < MAX_RETRY_ATTEMPTS) {
                        currentAttempt++
                        delay(backoffMs)
                        backoffMs *= 2
                        continue
                    }
                    return AIResult.Error(timeoutError)
                }

                when (executionResult) {
                    is AIResult.Success -> return executionResult
                    is AIResult.Error -> {
                        val error = executionResult.error
                        lastError = error

                        // Only retry transient errors; do NOT retry authentication or client validation errors
                        if (error.isTransient && currentAttempt < MAX_RETRY_ATTEMPTS) {
                            currentAttempt++
                            delay(backoffMs)
                            backoffMs *= 2
                            continue
                        } else {
                            return executionResult
                        }
                    }
                    else -> return executionResult
                }
            } catch (e: CancellationException) {
                return AIResult.Error(AIError.RequestCancelled())
            } catch (e: Exception) {
                val unknownError = AIError.Unknown(cause = e)
                lastError = unknownError
                if (currentAttempt < MAX_RETRY_ATTEMPTS) {
                    currentAttempt++
                    delay(backoffMs)
                    backoffMs *= 2
                    continue
                }
                return AIResult.Error(unknownError)
            }
        }

        return AIResult.Error(lastError ?: AIError.Unknown())
    }
}
