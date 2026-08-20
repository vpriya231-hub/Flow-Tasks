package com.flowtasks.app

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.flowtasks.app.core.security.SecureAIKeyStorage
import com.flowtasks.app.data.ai.model.GeminiCandidateDto
import com.flowtasks.app.data.ai.model.GeminiContentDto
import com.flowtasks.app.data.ai.model.GeminiErrorDto
import com.flowtasks.app.data.ai.model.GeminiPartDto
import com.flowtasks.app.data.ai.model.GeminiRequestDto
import com.flowtasks.app.data.ai.model.GeminiResponseDto
import com.flowtasks.app.data.ai.model.GeminiUsageMetadataDto
import com.flowtasks.app.data.ai.network.GeminiApiService
import com.flowtasks.app.data.ai.provider.GeminiAIProvider
import com.flowtasks.app.data.ai.service.AIServiceImpl
import com.flowtasks.app.domain.ai.AIConfig
import com.flowtasks.app.domain.ai.AIConfigRepository
import com.flowtasks.app.domain.ai.AIError
import com.flowtasks.app.domain.ai.AIKeyManager
import com.flowtasks.app.domain.ai.AIModelRegistry
import com.flowtasks.app.domain.ai.AIProvider
import com.flowtasks.app.domain.ai.AIProviderType
import com.flowtasks.app.domain.ai.AIRequest
import com.flowtasks.app.domain.ai.AIResponse
import com.flowtasks.app.domain.ai.AIResult
import com.flowtasks.app.domain.ai.AIStructuredContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.ResponseBody.Companion.toResponseBody
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import retrofit2.Response
import java.net.SocketTimeoutException
import java.net.UnknownHostException

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class AIFoundationArchitectureTest {

    private lateinit var context: Context

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
    }

    // ==========================================
    // 1. SECURE KEY STORAGE & MASKING TESTS
    // ==========================================

    @Test
    fun testSecureAIKeyStorage_lifecycleAndMasking() = runTest {
        val keyStorage = SecureAIKeyStorage(context)
        val provider = AIProviderType.GEMINI

        // Initially empty
        assertFalse(keyStorage.hasApiKey(provider))
        assertNull(keyStorage.getApiKey(provider))
        assertNull(keyStorage.getMaskedApiKey(provider))

        // Set key
        val testKey = "AIzaSyTestApiKey123456789"
        keyStorage.setApiKey(provider, testKey)

        assertTrue(keyStorage.hasApiKey(provider))
        val retrievedKey = keyStorage.getApiKey(provider)
        assertEquals(testKey, retrievedKey)

        // Masked key verification (never reveals full key)
        val masked = keyStorage.getMaskedApiKey(provider)
        assertNotNull(masked)
        assertTrue(masked!!.startsWith("AIza..."))
        assertTrue(masked.endsWith("6789"))
        assertFalse(masked.contains("TestApiKey"))

        // Deletion
        keyStorage.deleteApiKey(provider)
        assertFalse(keyStorage.hasApiKey(provider))
        assertNull(keyStorage.getApiKey(provider))
    }

    // ==========================================
    // 2. STRUCTURED CONTEXT & REQUEST MODEL TESTS
    // ==========================================

    @Test
    fun testAIRequest_buildCombinedPrompt_dataMinimization() {
        val taskContext = AIStructuredContext.TaskContext(
            id = 42L,
            title = "Prepare Project Proposal",
            description = "Draft budget and timeline",
            priority = "HIGH",
            estimatedMinutes = 60,
            subtaskTitles = listOf("Section A", "Section B")
        )

        val request = AIRequest(
            prompt = "Break this task into 3 actionable steps",
            context = taskContext
        )

        val prompt = request.buildCombinedPrompt()
        assertTrue(prompt.contains("Task Context:"))
        assertTrue(prompt.contains("Prepare Project Proposal"))
        assertTrue(prompt.contains("Draft budget and timeline"))
        assertTrue(prompt.contains("Estimated Duration: 60 minutes"))
        assertTrue(prompt.contains("Section A, Section B"))
        assertTrue(prompt.contains("Break this task into 3 actionable steps"))
    }

    // ==========================================
    // 3. GEMINI PROVIDER & ERROR TRANSLATION TESTS
    // ==========================================

    @Test
    fun testGeminiAIProvider_successResponse() = runTest {
        val mockService = object : GeminiApiService {
            override suspend fun generateContent(
                model: String,
                apiKey: String,
                request: GeminiRequestDto
            ): Response<GeminiResponseDto> {
                return Response.success(
                    GeminiResponseDto(
                        candidates = listOf(
                            GeminiCandidateDto(
                                content = GeminiContentDto(
                                    role = "model",
                                    parts = listOf(GeminiPartDto(text = "1. Define scope\n2. Outline budget\n3. Review"))
                                ),
                                finishReason = "STOP"
                            )
                        ),
                        usageMetadata = GeminiUsageMetadataDto(
                            promptTokenCount = 15,
                            candidatesTokenCount = 20,
                            totalTokenCount = 35
                        )
                    )
                )
            }
        }

        val provider = GeminiAIProvider(mockService)
        val result = provider.generateText(
            request = AIRequest(prompt = "Suggest subtasks"),
            apiKey = "valid-key"
        )

        assertTrue(result is AIResult.Success)
        val response = (result as AIResult.Success).data
        assertTrue(response.text.contains("Define scope"))
        assertEquals(AIProviderType.GEMINI, response.provider)
        assertEquals(35, response.usageMetadata?.totalTokens)
    }

    @Test
    fun testGeminiAIProvider_errorTranslation_invalidKey() = runTest {
        val mockService = object : GeminiApiService {
            override suspend fun generateContent(
                model: String,
                apiKey: String,
                request: GeminiRequestDto
            ): Response<GeminiResponseDto> {
                val errorBody = """{"error": {"code": 400, "message": "API_KEY_INVALID", "status": "INVALID_ARGUMENT"}}"""
                    .toResponseBody("application/json".toMediaTypeOrNull())
                return Response.error(400, errorBody)
            }
        }

        val provider = GeminiAIProvider(mockService)
        val result = provider.generateText(AIRequest(prompt = "Hello"), apiKey = "bad-key")

        assertTrue(result is AIResult.Error)
        val error = (result as AIResult.Error).error
        assertTrue(error is AIError.InvalidApiKey)
        assertFalse(error.isTransient)
    }

    @Test
    fun testGeminiAIProvider_errorTranslation_rateLimit() = runTest {
        val mockService = object : GeminiApiService {
            override suspend fun generateContent(
                model: String,
                apiKey: String,
                request: GeminiRequestDto
            ): Response<GeminiResponseDto> {
                val errorBody = """{"error": {"code": 429, "message": "RESOURCE_EXHAUSTED"}}"""
                    .toResponseBody("application/json".toMediaTypeOrNull())
                return Response.error(429, errorBody)
            }
        }

        val provider = GeminiAIProvider(mockService)
        val result = provider.generateText(AIRequest(prompt = "Hello"), apiKey = "valid-key")

        assertTrue(result is AIResult.Error)
        val error = (result as AIResult.Error).error
        assertTrue(error is AIError.RateLimitExceeded)
        assertTrue(error.isTransient)
    }

    @Test
    fun testGeminiAIProvider_errorTranslation_networkTimeout() = runTest {
        val mockService = object : GeminiApiService {
            override suspend fun generateContent(
                model: String,
                apiKey: String,
                request: GeminiRequestDto
            ): Response<GeminiResponseDto> {
                throw SocketTimeoutException("Read timed out")
            }
        }

        val provider = GeminiAIProvider(mockService)
        val result = provider.generateText(AIRequest(prompt = "Hello"), apiKey = "valid-key")

        assertTrue(result is AIResult.Error)
        val error = (result as AIResult.Error).error
        assertTrue(error is AIError.Timeout)
        assertTrue(error.isTransient)
    }

    // ==========================================
    // 4. AI SERVICE COORDINATOR & RETRY LOGIC TESTS
    // ==========================================

    @Test
    fun testAIService_missingKeyHandling() = runTest {
        val fakeKeyManager = object : AIKeyManager {
            override suspend fun setApiKey(provider: AIProviderType, apiKey: String) {}
            override suspend fun getApiKey(provider: AIProviderType): String? = null
            override suspend fun hasApiKey(provider: AIProviderType): Boolean = false
            override suspend fun deleteApiKey(provider: AIProviderType) {}
            override suspend fun getMaskedApiKey(provider: AIProviderType): String? = null
        }

        val fakeConfigRepo = FakeAIConfigRepository()
        val mockProvider = object : AIProvider {
            override val providerType = AIProviderType.GEMINI
            override suspend fun generateText(request: AIRequest, apiKey: String): AIResult<AIResponse> =
                AIResult.Success(AIResponse("OK", provider = AIProviderType.GEMINI, model = "test"))
            override suspend fun validateKey(apiKey: String, modelName: String?): AIResult<Boolean> = AIResult.Success(true)
        }

        val service = AIServiceImpl(
            aiConfigRepository = fakeConfigRepo,
            aiKeyManager = fakeKeyManager,
            providers = mapOf(AIProviderType.GEMINI to mockProvider)
        )

        val result = service.generateTextDirect(AIRequest(prompt = "Hello"))
        assertTrue(result is AIResult.Error)
        assertTrue((result as AIResult.Error).error is AIError.MissingApiKey)
    }

    @Test
    fun testAIService_disabledStateHandling() = runTest {
        val fakeKeyManager = object : AIKeyManager {
            override suspend fun setApiKey(provider: AIProviderType, apiKey: String) {}
            override suspend fun getApiKey(provider: AIProviderType): String = "secret-key"
            override suspend fun hasApiKey(provider: AIProviderType): Boolean = true
            override suspend fun deleteApiKey(provider: AIProviderType) {}
            override suspend fun getMaskedApiKey(provider: AIProviderType): String = "sec...••••"
        }

        val fakeConfigRepo = FakeAIConfigRepository(initialConfig = AIConfig(isEnabled = false))
        val mockProvider = object : AIProvider {
            override val providerType = AIProviderType.GEMINI
            override suspend fun generateText(request: AIRequest, apiKey: String): AIResult<AIResponse> =
                AIResult.Success(AIResponse("OK", provider = AIProviderType.GEMINI, model = "test"))
            override suspend fun validateKey(apiKey: String, modelName: String?): AIResult<Boolean> = AIResult.Success(true)
        }

        val service = AIServiceImpl(
            aiConfigRepository = fakeConfigRepo,
            aiKeyManager = fakeKeyManager,
            providers = mapOf(AIProviderType.GEMINI to mockProvider)
        )

        val result = service.generateTextDirect(AIRequest(prompt = "Hello"))
        assertTrue(result is AIResult.Error)
        assertTrue((result as AIResult.Error).error is AIError.ProviderDisabled)
    }

    @Test
    fun testAIService_transientErrorRetrySucceeds() = runTest {
        var callCount = 0
        val fakeKeyManager = object : AIKeyManager {
            override suspend fun setApiKey(provider: AIProviderType, apiKey: String) {}
            override suspend fun getApiKey(provider: AIProviderType): String = "secret-key"
            override suspend fun hasApiKey(provider: AIProviderType): Boolean = true
            override suspend fun deleteApiKey(provider: AIProviderType) {}
            override suspend fun getMaskedApiKey(provider: AIProviderType): String = "sec...••••"
        }

        val fakeConfigRepo = FakeAIConfigRepository()
        val mockProvider = object : AIProvider {
            override val providerType = AIProviderType.GEMINI
            override suspend fun generateText(request: AIRequest, apiKey: String): AIResult<AIResponse> {
                callCount++
                return if (callCount == 1) {
                    // First call fails with transient server error
                    AIResult.Error(AIError.ServerError(503))
                } else {
                    // Second call succeeds
                    AIResult.Success(AIResponse("Success after retry", provider = AIProviderType.GEMINI, model = "test"))
                }
            }
            override suspend fun validateKey(apiKey: String, modelName: String?): AIResult<Boolean> = AIResult.Success(true)
        }

        val service = AIServiceImpl(
            aiConfigRepository = fakeConfigRepo,
            aiKeyManager = fakeKeyManager,
            providers = mapOf(AIProviderType.GEMINI to mockProvider)
        )

        val result = service.generateTextDirect(AIRequest(prompt = "Hello"))
        assertTrue(result is AIResult.Success)
        assertEquals("Success after retry", (result as AIResult.Success).data.text)
        assertEquals(2, callCount)
    }

    // ==========================================
    // 5. GEMINI 3.X MIGRATION & COMPATIBILITY TESTS
    // ==========================================

    @Test
    fun testGemini3xModelMigration_normalizesObsoleteModels() {
        // Obsolete models migrate to default Gemini 3.6 Flash
        assertEquals(AIConfig.MODEL_GEMINI_3_6_FLASH, AIConfig.normalizeModelName("gemini-2.5-flash"))
        assertEquals(AIConfig.MODEL_GEMINI_3_6_FLASH, AIConfig.normalizeModelName("gemini-1.5-flash"))
        assertEquals(AIConfig.MODEL_GEMINI_3_6_FLASH, AIConfig.normalizeModelName("gemini-1.5-pro"))
        assertEquals(AIConfig.MODEL_GEMINI_3_6_FLASH, AIConfig.normalizeModelName("gemini-2.0-flash"))
        assertEquals(AIConfig.MODEL_GEMINI_3_6_FLASH, AIConfig.normalizeModelName(null))
        assertEquals(AIConfig.MODEL_GEMINI_3_6_FLASH, AIConfig.normalizeModelName(""))

        // Valid models are preserved
        assertEquals(AIConfig.MODEL_GEMINI_3_6_FLASH, AIConfig.normalizeModelName("gemini-3.6-flash"))
        assertEquals(AIConfig.MODEL_GEMINI_3_7_FLASH, AIConfig.normalizeModelName("gemini-3.7-flash"))
        assertEquals(AIConfig.MODEL_GEMINI_3_1_PRO, AIConfig.normalizeModelName("gemini-3.1-pro-preview"))
    }

    @Test
    fun testGeminiAIProvider_gemini37Flash_includesThinkingConfig() = runTest {
        var capturedRequest: GeminiRequestDto? = null
        val mockService = object : GeminiApiService {
            override suspend fun generateContent(
                model: String,
                apiKey: String,
                request: GeminiRequestDto
            ): Response<GeminiResponseDto> {
                capturedRequest = request
                return Response.success(
                    GeminiResponseDto(
                        candidates = listOf(
                            GeminiCandidateDto(
                                content = GeminiContentDto(
                                    role = "model",
                                    parts = listOf(GeminiPartDto(text = "Response text"))
                                ),
                                finishReason = "STOP"
                            )
                        )
                    )
                )
            }
        }

        val provider = GeminiAIProvider(mockService)
        val result = provider.generateText(
            request = AIRequest(
                prompt = "Explain strategy",
                config = AIConfig(modelName = AIConfig.MODEL_GEMINI_3_7_FLASH)
            ),
            apiKey = "valid-key"
        )

        assertTrue(result is AIResult.Success)
        assertNotNull(capturedRequest)
        assertNotNull(capturedRequest?.generationConfig?.thinkingConfig)
        assertEquals("medium", capturedRequest?.generationConfig?.thinkingConfig?.thinkingLevel)
    }

    @Test
    fun testAIModelRegistry_providerIsolationAndNormalization() {
        // 1. Gemini
        val geminiModels = AIModelRegistry.getModelsForProvider(AIProviderType.GEMINI)
        assertEquals(3, geminiModels.size)
        assertTrue(geminiModels.all { it.provider == AIProviderType.GEMINI })
        assertEquals(AIModelRegistry.MODEL_GEMINI_3_6_FLASH, AIModelRegistry.getDefaultModelForProvider(AIProviderType.GEMINI))
        assertEquals(AIModelRegistry.MODEL_GEMINI_3_6_FLASH, AIModelRegistry.normalizeModelForProvider(AIProviderType.GEMINI, "gpt-5"))
        assertEquals(AIModelRegistry.MODEL_GEMINI_3_7_FLASH, AIModelRegistry.normalizeModelForProvider(AIProviderType.GEMINI, "gemini-3.7-flash"))

        // 2. OpenAI
        val openAIModels = AIModelRegistry.getModelsForProvider(AIProviderType.OPENAI)
        assertEquals(2, openAIModels.size)
        assertTrue(openAIModels.all { it.provider == AIProviderType.OPENAI })
        assertEquals(AIModelRegistry.MODEL_OPENAI_GPT_5, AIModelRegistry.getDefaultModelForProvider(AIProviderType.OPENAI))
        assertEquals(AIModelRegistry.MODEL_OPENAI_GPT_5, AIModelRegistry.normalizeModelForProvider(AIProviderType.OPENAI, "gemini-3.6-flash"))
        assertEquals(AIModelRegistry.MODEL_OPENAI_GPT_5_MINI, AIModelRegistry.normalizeModelForProvider(AIProviderType.OPENAI, "gpt-5-mini"))

        // 3. Anthropic
        val anthropicModels = AIModelRegistry.getModelsForProvider(AIProviderType.ANTHROPIC)
        assertEquals(3, anthropicModels.size)
        assertTrue(anthropicModels.all { it.provider == AIProviderType.ANTHROPIC })
        assertEquals(AIModelRegistry.MODEL_CLAUDE_3_7_SONNET, AIModelRegistry.getDefaultModelForProvider(AIProviderType.ANTHROPIC))
        assertEquals(AIModelRegistry.MODEL_CLAUDE_3_7_SONNET, AIModelRegistry.normalizeModelForProvider(AIProviderType.ANTHROPIC, "gpt-5"))
        assertEquals(AIModelRegistry.MODEL_CLAUDE_3_5_SONNET, AIModelRegistry.normalizeModelForProvider(AIProviderType.ANTHROPIC, "claude-3-5-sonnet-20241022"))

        // 4. Custom
        val customModels = AIModelRegistry.getModelsForProvider(AIProviderType.CUSTOM)
        assertTrue(customModels.isEmpty())
        assertEquals(AIModelRegistry.MODEL_CUSTOM_DEFAULT, AIModelRegistry.getDefaultModelForProvider(AIProviderType.CUSTOM))
        assertEquals("llama-3.3-70b-instruct", AIModelRegistry.normalizeModelForProvider(AIProviderType.CUSTOM, "llama-3.3-70b-instruct"))
    }

    @Test
    fun testAIConfigRepositoryImpl_perProviderModelPersistence() = runTest {
        val repo = com.flowtasks.app.data.ai.repository.AIConfigRepositoryImpl(context)

        // Initial default: Gemini with 3.6 Flash
        repo.updateProvider(AIProviderType.GEMINI)
        repo.updateModel(AIModelRegistry.MODEL_GEMINI_3_7_FLASH)
        assertEquals(AIProviderType.GEMINI, repo.getActiveConfig().provider)
        assertEquals(AIModelRegistry.MODEL_GEMINI_3_7_FLASH, repo.getActiveConfig().modelName)

        // Switch to OpenAI: default model GPT-5
        repo.updateProvider(AIProviderType.OPENAI)
        assertEquals(AIProviderType.OPENAI, repo.getActiveConfig().provider)
        assertEquals(AIModelRegistry.MODEL_OPENAI_GPT_5, repo.getActiveConfig().modelName)

        // Change OpenAI model to GPT-5 mini
        repo.updateModel(AIModelRegistry.MODEL_OPENAI_GPT_5_MINI)
        assertEquals(AIModelRegistry.MODEL_OPENAI_GPT_5_MINI, repo.getActiveConfig().modelName)

        // Switch to Anthropic: default model Claude 3.7 Sonnet
        repo.updateProvider(AIProviderType.ANTHROPIC)
        assertEquals(AIProviderType.ANTHROPIC, repo.getActiveConfig().provider)
        assertEquals(AIModelRegistry.MODEL_CLAUDE_3_7_SONNET, repo.getActiveConfig().modelName)

        // Change Anthropic model to Claude 3.5 Sonnet
        repo.updateModel(AIModelRegistry.MODEL_CLAUDE_3_5_SONNET)
        assertEquals(AIModelRegistry.MODEL_CLAUDE_3_5_SONNET, repo.getActiveConfig().modelName)

        // Switch to Custom: default model custom-model
        repo.updateProvider(AIProviderType.CUSTOM)
        assertEquals(AIProviderType.CUSTOM, repo.getActiveConfig().provider)
        assertEquals(AIModelRegistry.MODEL_CUSTOM_DEFAULT, repo.getActiveConfig().modelName)

        // Change Custom model to user specified endpoint model
        repo.updateModel("llama-3.3-70b-instruct")
        assertEquals("llama-3.3-70b-instruct", repo.getActiveConfig().modelName)

        // Switch back to Gemini: should restore Gemini 3.7 Flash
        repo.updateProvider(AIProviderType.GEMINI)
        assertEquals(AIProviderType.GEMINI, repo.getActiveConfig().provider)
        assertEquals(AIModelRegistry.MODEL_GEMINI_3_7_FLASH, repo.getActiveConfig().modelName)

        // Switch back to OpenAI: should restore GPT-5 mini
        repo.updateProvider(AIProviderType.OPENAI)
        assertEquals(AIProviderType.OPENAI, repo.getActiveConfig().provider)
        assertEquals(AIModelRegistry.MODEL_OPENAI_GPT_5_MINI, repo.getActiveConfig().modelName)

        // Switch back to Anthropic: should restore Claude 3.5 Sonnet
        repo.updateProvider(AIProviderType.ANTHROPIC)
        assertEquals(AIProviderType.ANTHROPIC, repo.getActiveConfig().provider)
        assertEquals(AIModelRegistry.MODEL_CLAUDE_3_5_SONNET, repo.getActiveConfig().modelName)

        // Switch back to Custom: should restore custom entered model
        repo.updateProvider(AIProviderType.CUSTOM)
        assertEquals(AIProviderType.CUSTOM, repo.getActiveConfig().provider)
        assertEquals("llama-3.3-70b-instruct", repo.getActiveConfig().modelName)
    }

    @Test
    fun testGeminiAIProvider_validateKey_usesSelectedModel() = runTest {
        var requestedModel: String? = null
        val mockService = object : GeminiApiService {
            override suspend fun generateContent(
                model: String,
                apiKey: String,
                request: GeminiRequestDto
            ): Response<GeminiResponseDto> {
                requestedModel = model
                return Response.success(
                    GeminiResponseDto(
                        candidates = listOf(
                            GeminiCandidateDto(
                                content = GeminiContentDto(
                                    role = "model",
                                    parts = listOf(GeminiPartDto(text = "pong"))
                                ),
                                finishReason = "STOP"
                            )
                        )
                    )
                )
            }
        }

        val provider = GeminiAIProvider(mockService)
        val result = provider.validateKey("valid-key", AIConfig.MODEL_GEMINI_3_1_PRO)

        assertTrue(result is AIResult.Success)
        assertEquals(AIConfig.MODEL_GEMINI_3_1_PRO, requestedModel)
    }

    @Test
    fun testAIServiceImpl_routesToConfiguredProvider() = runTest {
        val fakeKeyManager = object : AIKeyManager {
            private val keys = mutableMapOf(
                AIProviderType.GEMINI to "gemini-key",
                AIProviderType.OPENAI to "openai-key",
                AIProviderType.ANTHROPIC to "anthropic-key",
                AIProviderType.CUSTOM to "custom-key"
            )
            override suspend fun getApiKey(provider: AIProviderType): String? = keys[provider]
            override suspend fun setApiKey(provider: AIProviderType, apiKey: String) { keys[provider] = apiKey }
            override suspend fun deleteApiKey(provider: AIProviderType) { keys.remove(provider) }
            override suspend fun hasApiKey(provider: AIProviderType): Boolean = keys.containsKey(provider)
            override suspend fun getMaskedApiKey(provider: AIProviderType): String? = keys[provider]?.let { "masked" }
        }

        var calledProvider: AIProviderType? = null
        val makeMockProvider: (AIProviderType) -> AIProvider = { type ->
            object : AIProvider {
                override val providerType: AIProviderType = type
                override suspend fun generateText(request: AIRequest, apiKey: String): AIResult<AIResponse> {
                    calledProvider = type
                    return AIResult.Success(AIResponse(text = "Hello from $type", provider = type, model = request.config?.modelName ?: "default"))
                }
                override suspend fun validateKey(apiKey: String, modelName: String?): AIResult<Boolean> {
                    calledProvider = type
                    return AIResult.Success(true)
                }
            }
        }

        val fakeRepo = FakeAIConfigRepository(AIConfig(provider = AIProviderType.OPENAI, modelName = AIModelRegistry.MODEL_OPENAI_GPT_5))
        val aiService = AIServiceImpl(
            aiConfigRepository = fakeRepo,
            aiKeyManager = fakeKeyManager,
            providers = mapOf(
                AIProviderType.GEMINI to makeMockProvider(AIProviderType.GEMINI),
                AIProviderType.OPENAI to makeMockProvider(AIProviderType.OPENAI),
                AIProviderType.ANTHROPIC to makeMockProvider(AIProviderType.ANTHROPIC),
                AIProviderType.CUSTOM to makeMockProvider(AIProviderType.CUSTOM)
            )
        )

        // Route to OpenAI
        val openAIResult = aiService.generateTextDirect(AIRequest(prompt = "Test"))
        assertTrue(openAIResult is AIResult.Success)
        assertEquals(AIProviderType.OPENAI, calledProvider)

        // Switch to Anthropic
        fakeRepo.updateProvider(AIProviderType.ANTHROPIC)
        fakeRepo.updateModel(AIModelRegistry.MODEL_CLAUDE_3_7_SONNET)
        val anthropicResult = aiService.generateTextDirect(AIRequest(prompt = "Test"))
        assertTrue(anthropicResult is AIResult.Success)
        assertEquals(AIProviderType.ANTHROPIC, calledProvider)

        // Test connection routes to Anthropic
        val connectionResult = aiService.validateActiveKey()
        assertTrue(connectionResult is AIResult.Success)
        assertEquals(AIProviderType.ANTHROPIC, calledProvider)
    }
}

private class FakeAIConfigRepository(
    initialConfig: AIConfig = AIConfig()
) : AIConfigRepository {
    private val state = MutableStateFlow(initialConfig)
    override val aiConfigFlow: Flow<AIConfig> = state

    override suspend fun getActiveConfig(): AIConfig = state.value
    override suspend fun updateProvider(provider: AIProviderType) {
        state.value = state.value.copy(provider = provider)
    }
    override suspend fun updateModel(modelName: String) {
        state.value = state.value.copy(modelName = modelName)
    }
    override suspend fun updateTemperature(temperature: Float?) {
        state.value = state.value.copy(temperature = temperature)
    }
    override suspend fun updateMaxTokens(maxTokens: Int?) {
        state.value = state.value.copy(maxOutputTokens = maxTokens)
    }
    override suspend fun updateTimeoutSeconds(timeoutSeconds: Long) {
        state.value = state.value.copy(timeoutSeconds = timeoutSeconds)
    }
    override suspend fun setEnabled(enabled: Boolean) {
        state.value = state.value.copy(isEnabled = enabled)
    }
}
