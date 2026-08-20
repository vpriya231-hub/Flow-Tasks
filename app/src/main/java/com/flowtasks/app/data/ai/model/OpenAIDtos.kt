package com.flowtasks.app.data.ai.model

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class OpenAIRequestDto(
    @Json(name = "model") val model: String,
    @Json(name = "messages") val messages: List<OpenAIMessageDto>,
    @Json(name = "max_tokens") val maxTokens: Int? = null,
    @Json(name = "temperature") val temperature: Float? = null
)

@JsonClass(generateAdapter = true)
data class OpenAIMessageDto(
    @Json(name = "role") val role: String,
    @Json(name = "content") val content: String
)

@JsonClass(generateAdapter = true)
data class OpenAIResponseDto(
    @Json(name = "id") val id: String? = null,
    @Json(name = "choices") val choices: List<OpenAIChoiceDto>? = null,
    @Json(name = "usage") val usage: OpenAIUsageDto? = null,
    @Json(name = "error") val error: OpenAIErrorDto? = null
)

@JsonClass(generateAdapter = true)
data class OpenAIChoiceDto(
    @Json(name = "index") val index: Int? = null,
    @Json(name = "message") val message: OpenAIMessageDto? = null,
    @Json(name = "finish_reason") val finishReason: String? = null
)

@JsonClass(generateAdapter = true)
data class OpenAIUsageDto(
    @Json(name = "prompt_tokens") val promptTokens: Int? = null,
    @Json(name = "completion_tokens") val completionTokens: Int? = null,
    @Json(name = "total_tokens") val totalTokens: Int? = null
)

@JsonClass(generateAdapter = true)
data class OpenAIErrorDto(
    @Json(name = "message") val message: String? = null,
    @Json(name = "type") val type: String? = null,
    @Json(name = "code") val code: String? = null
)
