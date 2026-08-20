package com.flowtasks.app.domain.ai

/**
 * Clean, generic result state wrapper for AI operations.
 */
sealed interface AIResult<out T> {
    data object Idle : AIResult<Nothing>
    data object Loading : AIResult<Nothing>
    data class Success<T>(val data: T) : AIResult<T>
    data class Error(val error: AIError) : AIResult<Nothing>

    val isSuccess: Boolean
        get() = this is Success

    val isError: Boolean
        get() = this is Error

    val isLoading: Boolean
        get() = this is Loading

    fun getOrNull(): T? = when (this) {
        is Success -> data
        else -> null
    }

    fun errorOrNull(): AIError? = when (this) {
        is Error -> error
        else -> null
    }
}
