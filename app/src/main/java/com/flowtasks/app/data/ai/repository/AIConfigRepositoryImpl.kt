package com.flowtasks.app.data.ai.repository

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import com.flowtasks.app.core.datastore.dataStore
import com.flowtasks.app.domain.ai.AIConfig
import com.flowtasks.app.domain.ai.AIConfigRepository
import com.flowtasks.app.domain.ai.AIModelRegistry
import com.flowtasks.app.domain.ai.AIProviderType
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

class AIConfigRepositoryImpl(
    private val context: Context
) : AIConfigRepository {

    private object PreferencesKeys {
        val PROVIDER = stringPreferencesKey("ai_provider")
        val MODEL_NAME = stringPreferencesKey("ai_model_name")
        val MODEL_GEMINI = stringPreferencesKey("ai_model_gemini")
        val MODEL_OPENAI = stringPreferencesKey("ai_model_openai")
        val MODEL_ANTHROPIC = stringPreferencesKey("ai_model_anthropic")
        val MODEL_CUSTOM = stringPreferencesKey("ai_model_custom")
        val TEMPERATURE = floatPreferencesKey("ai_temperature")
        val MAX_TOKENS = intPreferencesKey("ai_max_tokens")
        val TIMEOUT_SECONDS = longPreferencesKey("ai_timeout_seconds")
        val IS_ENABLED = booleanPreferencesKey("ai_is_enabled")
    }

    override val aiConfigFlow: Flow<AIConfig> = context.dataStore.data.map { preferences ->
        val providerStr = preferences[PreferencesKeys.PROVIDER] ?: AIProviderType.GEMINI.name
        val provider = AIProviderType.fromString(providerStr)

        val storedModel = when (provider) {
            AIProviderType.GEMINI -> preferences[PreferencesKeys.MODEL_GEMINI]
            AIProviderType.OPENAI -> preferences[PreferencesKeys.MODEL_OPENAI]
            AIProviderType.ANTHROPIC -> preferences[PreferencesKeys.MODEL_ANTHROPIC]
            AIProviderType.CUSTOM -> preferences[PreferencesKeys.MODEL_CUSTOM]
        } ?: preferences[PreferencesKeys.MODEL_NAME]

        val model = AIModelRegistry.normalizeModelForProvider(provider, storedModel)
        val temp = preferences[PreferencesKeys.TEMPERATURE]
        val maxTokens = preferences[PreferencesKeys.MAX_TOKENS]
        val timeout = preferences[PreferencesKeys.TIMEOUT_SECONDS] ?: AIConfig.DEFAULT_TIMEOUT_SECONDS
        val isEnabled = preferences[PreferencesKeys.IS_ENABLED] ?: true

        AIConfig(
            provider = provider,
            modelName = model,
            temperature = temp,
            maxOutputTokens = maxTokens,
            timeoutSeconds = timeout,
            isEnabled = isEnabled
        )
    }

    override suspend fun getActiveConfig(): AIConfig {
        return aiConfigFlow.first()
    }

    override suspend fun updateProvider(provider: AIProviderType) {
        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.PROVIDER] = provider.name

            val storedModel = when (provider) {
                AIProviderType.GEMINI -> preferences[PreferencesKeys.MODEL_GEMINI]
                AIProviderType.OPENAI -> preferences[PreferencesKeys.MODEL_OPENAI]
                AIProviderType.ANTHROPIC -> preferences[PreferencesKeys.MODEL_ANTHROPIC]
                AIProviderType.CUSTOM -> preferences[PreferencesKeys.MODEL_CUSTOM]
            }

            val targetModel = AIModelRegistry.normalizeModelForProvider(provider, storedModel)
            preferences[PreferencesKeys.MODEL_NAME] = targetModel
        }
    }

    override suspend fun updateModel(modelName: String) {
        val currentProvider = getActiveConfig().provider
        val normalized = AIModelRegistry.normalizeModelForProvider(currentProvider, modelName)
        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.MODEL_NAME] = normalized
            when (currentProvider) {
                AIProviderType.GEMINI -> preferences[PreferencesKeys.MODEL_GEMINI] = normalized
                AIProviderType.OPENAI -> preferences[PreferencesKeys.MODEL_OPENAI] = normalized
                AIProviderType.ANTHROPIC -> preferences[PreferencesKeys.MODEL_ANTHROPIC] = normalized
                AIProviderType.CUSTOM -> preferences[PreferencesKeys.MODEL_CUSTOM] = normalized
            }
        }
    }

    override suspend fun updateTemperature(temperature: Float?) {
        context.dataStore.edit { preferences ->
            if (temperature != null) {
                preferences[PreferencesKeys.TEMPERATURE] = temperature
            } else {
                preferences.remove(PreferencesKeys.TEMPERATURE)
            }
        }
    }

    override suspend fun updateMaxTokens(maxTokens: Int?) {
        context.dataStore.edit { preferences ->
            if (maxTokens != null) {
                preferences[PreferencesKeys.MAX_TOKENS] = maxTokens
            } else {
                preferences.remove(PreferencesKeys.MAX_TOKENS)
            }
        }
    }

    override suspend fun updateTimeoutSeconds(timeoutSeconds: Long) {
        val clamped = timeoutSeconds.coerceIn(AIConfig.MIN_TIMEOUT_SECONDS, AIConfig.MAX_TIMEOUT_SECONDS)
        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.TIMEOUT_SECONDS] = clamped
        }
    }

    override suspend fun setEnabled(enabled: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.IS_ENABLED] = enabled
        }
    }
}

