package com.flowtasks.app.data.ai.provider

import com.flowtasks.app.data.ai.model.GeminiContentDto
import com.flowtasks.app.data.ai.model.GeminiGenerationConfigDto
import com.flowtasks.app.data.ai.model.GeminiPartDto
import com.flowtasks.app.data.ai.model.GeminiRequestDto
import com.flowtasks.app.data.ai.model.GeminiThinkingConfigDto
import com.flowtasks.app.data.ai.network.GeminiApiService
import com.flowtasks.app.data.ai.network.GeminiNetworkClient
import com.flowtasks.app.domain.ai.AIConfig
import com.flowtasks.app.domain.ai.AIError
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
 * Concrete provider implementation for Google Gemini.
 * Translates provider-agnostic domain requests to Gemini API calls,
 * and normalizes responses and errors without exposing credentials.
 */
class GeminiAIProvider(
    private val apiService: GeminiApiService = GeminiNetworkClient.createApiService()
) : AIProvider {

    override val providerType: AIProviderType = AIProviderType.GEMINI

    override suspend fun generateText(
        request: AIRequest,
        apiKey: String
    ): AIResult<AIResponse> = withContext(Dispatchers.IO) {
        if (apiKey.isBlank()) {
            return@withContext AIResult.Error(AIError.MissingApiKey())
        }

        val rawModel = request.config?.modelName?.ifBlank { null } ?: AIConfig.DEFAULT_GEMINI_MODEL
        val model = AIConfig.normalizeModelName(rawModel)
        val combinedPrompt = request.buildCombinedPrompt()

        val contents = listOf(
            GeminiContentDto(
                role = "user",
                parts = listOf(GeminiPartDto(text = combinedPrompt))
            )
        )

        val systemInstruction = request.systemInstruction?.takeIf { it.isNotBlank() }?.let { instruction ->
            GeminiContentDto(
                role = "system",
                parts = listOf(GeminiPartDto(text = instruction))
            )
        }

        // Gemini 3.7 Flash supports thinkingConfig (medium by default).
        // Gemini 3.x models do NOT send deprecated temperature, top_p, top_k, candidate_count.
        val thinkingConfig = when (model) {
            AIConfig.MODEL_GEMINI_3_7_FLASH -> GeminiThinkingConfigDto(thinkingLevel = "medium")
            else -> null
        }

        val maxOutputTokens = request.config?.maxOutputTokens

        val generationConfig = if (maxOutputTokens != null || thinkingConfig != null) {
            GeminiGenerationConfigDto(
                maxOutputTokens = maxOutputTokens,
                thinkingConfig = thinkingConfig
            )
        } else {
            null
        }

        val requestDto = GeminiRequestDto(
            contents = contents,
            systemInstruction = systemInstruction,
            generationConfig = generationConfig
        )

        try {
            val response = apiService.generateContent(
                model = model,
                apiKey = apiKey,
                request = requestDto
            )

            if (response.isSuccessful) {
                val body = response.body()
                val candidate = body?.candidates?.firstOrNull()
                val text = candidate?.content?.parts?.joinToString("\n") { it.text }

                if (text.isNullOrBlank()) {
                    AIResult.Error(AIError.MalformedResponse("Received empty response candidate from Gemini."))
                } else {
                    val usage = body.usageMetadata?.let {
                        AIUsageMetadata(
                            promptTokens = it.promptTokenCount ?: 0,
                            candidateTokens = it.candidatesTokenCount ?: 0,
                            totalTokens = it.totalTokenCount ?: 0
                        )
                    }

                    AIResult.Success(
                        AIResponse(
                            text = text,
                            isSuccess = true,
                            finishReason = candidate.finishReason,
                            usageMetadata = usage,
                            provider = AIProviderType.GEMINI,
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
        val targetModel = AIConfig.normalizeModelName(modelName)
        val testRequest = AIRequest(
            prompt = "ping",
            config = AIConfig(
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

    private fun parseHttpError(statusCode: Int, errorBody: String): AIError {
        val isKeyError = errorBody.contains("API_KEY_INVALID", ignoreCase = true) ||
                errorBody.contains("API key not valid", ignoreCase = true) ||
                errorBody.contains("PERMISSION_DENIED", ignoreCase = true)

        return when {
            statusCode == 400 && isKeyError -> AIError.InvalidApiKey()
            statusCode == 401 -> AIError.Unauthorized()
            statusCode == 403 -> if (isKeyError) AIError.InvalidApiKey() else AIError.Unauthorized()
            statusCode == 429 -> AIError.RateLimitExceeded()
            statusCode in 500..599 -> AIError.ServerError(statusCode)
            statusCode == 400 -> AIError.MalformedResponse("Invalid request payload structure.")
            else -> AIError.ServerError(statusCode, "Provider returned HTTP error $statusCode")
        }
    }
}
