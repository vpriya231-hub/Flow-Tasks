package com.flowtasks.app.domain.ai

/**
 * Sealed hierarchy of user-friendly, secure AI errors.
 * These errors never contain raw credentials or sensitive stack traces.
 */
sealed class AIError(
    val userFriendlyMessage: String,
    val technicalDetail: String? = null,
    val isTransient: Boolean = false,
    override val cause: Throwable? = null
) : Exception(userFriendlyMessage, cause) {

    class MissingApiKey(
        message: String = "No AI API key configured. Please set your API key in Settings."
    ) : AIError(userFriendlyMessage = message, isTransient = false)

    class InvalidApiKey(
        message: String = "The AI API key is invalid or unauthorized. Please verify your key."
    ) : AIError(userFriendlyMessage = message, isTransient = false)

    class Unauthorized(
        message: String = "Access unauthorized. Please verify your API key and permissions."
    ) : AIError(userFriendlyMessage = message, isTransient = false)

    class RateLimitExceeded(
        message: String = "AI rate limit reached. Please wait a moment and try again.",
        val retryAfterSeconds: Int? = null
    ) : AIError(userFriendlyMessage = message, isTransient = true)

    class Timeout(
        message: String = "AI request timed out. Please check your network connection and try again."
    ) : AIError(userFriendlyMessage = message, isTransient = true)

    class NetworkUnavailable(
        message: String = "AI service unavailable. Please check your internet connection and try again."
    ) : AIError(userFriendlyMessage = message, isTransient = true)

    class ServerError(
        val statusCode: Int,
        message: String = "The AI provider encountered a temporary error. Please try again later."
    ) : AIError(userFriendlyMessage = message, technicalDetail = "HTTP $statusCode", isTransient = statusCode in 500..599)

    class MalformedResponse(
        message: String = "Received an unexpected response from the AI service."
    ) : AIError(userFriendlyMessage = message, isTransient = false)

    class RequestCancelled(
        message: String = "The AI request was cancelled."
    ) : AIError(userFriendlyMessage = message, isTransient = false)

    class ProviderDisabled(
        message: String = "AI features are currently disabled in settings."
    ) : AIError(userFriendlyMessage = message, isTransient = false)

    class Unknown(
        message: String = "An unexpected error occurred while communicating with the AI service.",
        cause: Throwable? = null
    ) : AIError(userFriendlyMessage = message, cause = cause, isTransient = false)
}
