package com.flowtasks.app.core.security

import android.content.Context
import android.content.SharedPreferences
import com.flowtasks.app.domain.ai.AIKeyManager
import com.flowtasks.app.domain.ai.AIProviderType
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Secure on-device credential storage for BYOK (Bring Your Own Key) AI provider API keys.
 * Encrypts all keys using Android Keystore AES-256 GCM before persisting.
 */
class SecureAIKeyStorage(
    private val context: Context,
    private val encryptor: AndroidKeystoreEncryptor = AndroidKeystoreEncryptor()
) : AIKeyManager {

    companion object {
        private const val PREFS_FILE_NAME = "flow_tasks_secure_ai_keys"
        private const val KEY_PREFIX = "encrypted_key_"
    }

    private val sharedPreferences: SharedPreferences by lazy {
        context.getSharedPreferences(PREFS_FILE_NAME, Context.MODE_PRIVATE)
    }

    override suspend fun setApiKey(provider: AIProviderType, apiKey: String): Unit = withContext(Dispatchers.IO) {
        val trimmed = apiKey.trim()
        if (trimmed.isBlank()) {
            deleteApiKey(provider)
            return@withContext
        }

        val encrypted = encryptor.encrypt(trimmed)
        sharedPreferences.edit()
            .putString("$KEY_PREFIX${provider.name}", encrypted)
            .apply()
    }

    override suspend fun getApiKey(provider: AIProviderType): String? = withContext(Dispatchers.IO) {
        val encrypted = sharedPreferences.getString("$KEY_PREFIX${provider.name}", null) ?: return@withContext null
        encryptor.decrypt(encrypted)
    }

    override suspend fun hasApiKey(provider: AIProviderType): Boolean = withContext(Dispatchers.IO) {
        val encrypted = sharedPreferences.getString("$KEY_PREFIX${provider.name}", null)
        !encrypted.isNullOrBlank()
    }

    override suspend fun deleteApiKey(provider: AIProviderType): Unit = withContext(Dispatchers.IO) {
        sharedPreferences.edit()
            .remove("$KEY_PREFIX${provider.name}")
            .apply()
    }

    override suspend fun getMaskedApiKey(provider: AIProviderType): String? = withContext(Dispatchers.IO) {
        val rawKey = getApiKey(provider) ?: return@withContext null
        maskKey(rawKey)
    }

    /**
     * Safely masks an API key for UI display (e.g. "AIza...••••••••3f8B").
     * Never exposes the complete key.
     */
    private fun maskKey(key: String): String {
        return when {
            key.length <= 8 -> "••••••••"
            key.length <= 16 -> {
                val prefix = key.take(3)
                val suffix = key.takeLast(2)
                "$prefix...••••••••$suffix"
            }
            else -> {
                val prefix = key.take(4)
                val suffix = key.takeLast(4)
                "$prefix...••••••••$suffix"
            }
        }
    }
}
