package com.flowtasks.app.data.ai.provider

import com.flowtasks.app.data.ai.model.AnthropicMessageDto
import com.flowtasks.app.data.ai.model.AnthropicRequestDto
import com.flowtasks.app.data.ai.network.AnthropicApiService
import com.flowtasks.app.data.ai.network.AnthropicNetworkClient
import com.flowtasks.app.domain.ai.AIConfig
import com.flowtasks.app.domain.ai.AIError
import com.flowtasks.app.domain.ai.AIModelRegistry
import com.flowtasks.app.domain.ai.AIProvider
import com.flowtasks.app.domain.ai.AIProviderType
import com.flowtasks.app.domain.ai.AIRequest
import com.flowtasks.app.domain.ai.AIResponse
import com.flowtasks.app.domain.ai.AIResult
import com.flowtasks.app.domain.ai.AIUsageMetadata
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.IOException
import java.net.ConnectException
import java.net.SocketTimeoutException
import java.net.UnknownHostException

/**
 * Concrete AIProvider implementation for Anthropic Claude models.
 */
class AnthropicAIProvider(
    private val apiService: AnthropicApiService = AnthropicNetworkClient.createApiService()
) : AIProvider {

    override val providerType: AIProviderType = AIProviderType.ANTHROPIC

    override suspend fun generateText(request: AIRequest, apiKey: String): AIResult<AIResponse> = withContext(Dispatchers.IO) {
        if (apiKey.isBlank()) {
            return@withContext AIResult.Error(AIError.MissingApiKey())
        }

        val rawModel = request.config?.modelName?.ifBlank { null } ?: AIModelRegistry.MODEL_CLAUDE_3_7_SONNET
        val model = AIModelRegistry.normalizeModelForProvider(AIProviderType.ANTHROPIC, rawModel)

        val messages = listOf(
            AnthropicMessageDto(role = "user", content = request.prompt)
        )

        val requestDto = AnthropicRequestDto(
            model = model,
            maxTokens = request.config?.maxOutputTokens ?: 1024,
            messages = messages,
            system = request.systemInstruction,
            temperature = request.config?.temperature
        )

        try {
            val response = apiService.createMessage(
                apiKey = apiKey,
                request = requestDto
            )

            if (response.isSuccessful) {
                val body = response.body()
                val text = body?.content?.firstOrNull { it.type == "text" }?.text

                if (text.isNullOrBlank()) {
                    AIResult.Error(AIError.MalformedResponse("Received empty response text from Anthropic."))
                } else {
                    val usage = body.usage?.let {
                        AIUsageMetadata(
                            promptTokens = it.inputTokens ?: 0,
                            candidateTokens = it.outputTokens ?: 0,
                            totalTokens = (it.inputTokens ?: 0) + (it.outputTokens ?: 0)
                        )
                    }

                    AIResult.Success(
                        AIResponse(
                            text = text,
                            isSuccess = true,
                            finishReason = body.stopReason,
                            usageMetadata = usage,
                            provider = AIProviderType.ANTHROPIC,
                            model = model
                        )
                    )
                }
            } else {
                val errorBody = response.errorBody()?.string() ?: ""
                val code = response.code()
                val error = parseHttpError(code, errorBody)
                AIResult.Error(error)
            }
        } catch (e: CancellationException) {
            AIResult.Error(AIError.RequestCancelled())
        } catch (e: SocketTimeoutException) {
            AIResult.Error(AIError.Timeout())
        } catch (e: UnknownHostException) {
            AIResult.Error(AIError.NetworkUnavailable())
        } catch (e: ConnectException) {
            AIResult.Error(AIError.NetworkUnavailable())
        } catch (e: IOException) {
            AIResult.Error(AIError.NetworkUnavailable(message = "Network connection failed. Please try again."))
        } catch (e: Exception) {
            AIResult.Error(AIError.Unknown(cause = e))
        }
    }

    override suspend fun validateKey(
        apiKey: String,
        modelName: String?
    ): AIResult<Boolean> = withContext(Dispatchers.IO) {
        val targetModel = AIModelRegistry.normalizeModelForProvider(AIProviderType.ANTHROPIC, modelName)
        val testRequest = AIRequest(
            prompt = "ping",
            config = AIConfig(
                provider = AIProviderType.ANTHROPIC,
                modelName = targetModel,
                maxOutputTokens = 1
            )
        )
        when (val result = generateText(testRequest, apiKey)) {
            is AIResult.Success -> AIResult.Success(true)
            is AIResult.Error -> AIResult.Error(result.error)
            else -> AIResult.Error(AIError.Unknown())
        }
    }

    private fun parseHttpError(httpCode: Int, errorBody: String): AIError {
        return when (httpCode) {
            400 -> AIError.MalformedResponse("Anthropic request validation failed: $errorBody")
            401 -> AIError.InvalidApiKey("Invalid Anthropic API key. Please check your key in Settings.")
            403 -> AIError.Unauthorized("Anthropic key unauthorized or permission denied.")
            404 -> AIError.MalformedResponse("The selected Anthropic model was not found.")
            429 -> AIError.RateLimitExceeded("Anthropic rate limit or credit quota exceeded.")
            in 500..599 -> AIError.ServerError(httpCode, "Anthropic service is temporarily unavailable (HTTP $httpCode).")
            else -> AIError.ServerError(httpCode, "Anthropic HTTP Error $httpCode: $errorBody")
        }
    }
}
