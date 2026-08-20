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
 * Concrete AIProvider implementation for OpenAI (GPT-5, GPT-5 mini).
 */
class OpenAIAIProvider(
    private val apiService: OpenAIApiService = OpenAINetworkClient.createApiService()
) : AIProvider {

    override val providerType: AIProviderType = AIProviderType.OPENAI

    override suspend fun generateText(request: AIRequest, apiKey: String): AIResult<AIResponse> = withContext(Dispatchers.IO) {
        if (apiKey.isBlank()) {
            return@withContext AIResult.Error(AIError.MissingApiKey())
        }

        val rawModel = request.config?.modelName?.ifBlank { null } ?: AIModelRegistry.MODEL_OPENAI_GPT_5
        val model = AIModelRegistry.normalizeModelForProvider(AIProviderType.OPENAI, rawModel)

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
                authorization = "Bearer $apiKey",
                request = requestDto
            )

            if (response.isSuccessful) {
                val body = response.body()
                val choice = body?.choices?.firstOrNull()
                val text = choice?.message?.content

                if (text.isNullOrBlank()) {
                    AIResult.Error(AIError.MalformedResponse("Received empty response candidate from OpenAI."))
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
                            provider = AIProviderType.OPENAI,
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
        val targetModel = AIModelRegistry.normalizeModelForProvider(AIProviderType.OPENAI, modelName)
        val testRequest = AIRequest(
            prompt = "ping",
            config = AIConfig(
                provider = AIProviderType.OPENAI,
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
            400 -> AIError.MalformedResponse("OpenAI request validation failed: $errorBody")
            401 -> AIError.InvalidApiKey("Invalid OpenAI API key. Please check your key in Settings.")
            403 -> AIError.Unauthorized("OpenAI key unauthorized or quota depleted.")
            404 -> AIError.MalformedResponse("The selected OpenAI model was not found.")
            429 -> AIError.RateLimitExceeded("OpenAI rate limit or quota exceeded.")
            in 500..599 -> AIError.ServerError(httpCode, "OpenAI service is temporarily unavailable (HTTP $httpCode).")
            else -> AIError.ServerError(httpCode, "OpenAI HTTP Error $httpCode: $errorBody")
        }
    }
}
