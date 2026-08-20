package com.flowtasks.app.data.ai.provider

import com.flowtasks.app.data.ai.model.OpenAIMessageDto
import com.flowtasks.app.data.ai.model.OpenAIRequestDto
import com.flowtasks.app.data.ai.network.OpenAIApiService
import com.flowtasks.app.data.ai.network.OpenAINetworkClient
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
 * Concrete AIProvider implementation for user-configured custom endpoints / models.
 * Compatible with OpenAI-compatible custom servers (e.g., Ollama, vLLM, LM Studio, LiteLLM).
 */
class CustomAIProvider(
    private val apiService: OpenAIApiService = OpenAINetworkClient.createApiService()
) : AIProvider {

    override val providerType: AIProviderType = AIProviderType.CUSTOM

    override suspend fun generateText(request: AIRequest, apiKey: String): AIResult<AIResponse> = withContext(Dispatchers.IO) {
        if (apiKey.isBlank()) {
            return@withContext AIResult.Error(AIError.MissingApiKey())
        }

        val rawModel = request.config?.modelName?.ifBlank { null } ?: AIModelRegistry.MODEL_CUSTOM_DEFAULT
        val model = AIModelRegistry.normalizeModelForProvider(AIProviderType.CUSTOM, rawModel)

        val messages = mutableListOf<OpenAIMessageDto>()
        request.systemInstruction?.let {
            messages.add(OpenAIMessageDto(role = "system", content = it))
        }
        messages.add(OpenAIMessageDto(role = "user", content = request.prompt))

        val requestDto = OpenAIRequestDto(
            model = model,
            messages = messages,
            maxTokens = request.config?.maxOutputTokens,
            temperature = request.config?.temperature
        )

        try {
            val response = apiService.createChatCompletion(
                authorization = if (apiKey.startsWith("Bearer ", ignoreCase = true)) apiKey else "Bearer $apiKey",
                request = requestDto
            )

            if (response.isSuccessful) {
                val body = response.body()
                val choice = body?.choices?.firstOrNull()
                val text = choice?.message?.content

                if (text.isNullOrBlank()) {
                    AIResult.Error(AIError.MalformedResponse("Received empty response candidate from Custom endpoint."))
                } else {
                    val usage = body.usage?.let {
                        AIUsageMetadata(
                            promptTokens = it.promptTokens ?: 0,
                            candidateTokens = it.completionTokens ?: 0,
                            totalTokens = it.totalTokens ?: 0
                        )
                    }

                    AIResult.Success(
                        AIResponse(
                            text = text,
                            isSuccess = true,
                            finishReason = choice.finishReason,
                            usageMetadata = usage,
                            provider = AIProviderType.CUSTOM,
                            model = model
                        )
                    )
                }
            } else {
                val errorBody = response.errorBody()?.string() ?: ""
                val code = response.code()
                AIResult.Error(parseHttpError(code, errorBody))
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
            AIResult.Error(AIError.NetworkUnavailable(message = "Network connection to custom endpoint failed."))
        } catch (e: Exception) {
            AIResult.Error(AIError.Unknown(cause = e))
        }
    }

    override suspend fun validateKey(
        apiKey: String,
        modelName: String?
    ): AIResult<Boolean> = withContext(Dispatchers.IO) {
        val targetModel = AIModelRegistry.normalizeModelForProvider(AIProviderType.CUSTOM, modelName)
        val testRequest = AIRequest(
            prompt = "ping",
            config = AIConfig(
                provider = AIProviderType.CUSTOM,
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
            400 -> AIError.MalformedResponse("Custom endpoint validation failed: $errorBody")
            401 -> AIError.InvalidApiKey("Invalid custom API key / token. Please verify in Settings.")
            403 -> AIError.Unauthorized("Access forbidden to custom endpoint.")
            404 -> AIError.MalformedResponse("Custom model not found on endpoint.")
            429 -> AIError.RateLimitExceeded("Custom endpoint rate limit reached.")
            in 500..599 -> AIError.ServerError(httpCode, "Custom endpoint server error (HTTP $httpCode).")
            else -> AIError.ServerError(httpCode, "Custom endpoint HTTP Error $httpCode: $errorBody")
        }
    }
}
