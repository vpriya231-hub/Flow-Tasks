package com.flowtasks.app.domain.ai

/**
 * Interface contract for secure user API key storage and lifecycle management.
 * Guarantees that raw keys are never stored as plaintext, never logged, and masked when referenced.
 */
interface AIKeyManager {

    /**
     * Encrypts and securely persists the user's API key for a specified provider.
     */
    suspend fun setApiKey(provider: AIProviderType, apiKey: String)

    /**
     * Securely retrieves and decrypts the user's API key for internal use only.
     */
    suspend fun getApiKey(provider: AIProviderType): String?

    /**
     * Checks if a valid key is stored for the specified provider.
     */
    suspend fun hasApiKey(provider: AIProviderType): Boolean

    /**
     * Deletes the stored API key for the specified provider.
     */
    suspend fun deleteApiKey(provider: AIProviderType)

    /**
     * Returns a safely masked representation of the key for display (e.g. "AIza...••••••••").
     * Never returns the complete key.
     */
    suspend fun getMaskedApiKey(provider: AIProviderType): String?
}
