package com.flowtasks.app.data.ai.model

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class AnthropicRequestDto(
    @Json(name = "model") val model: String,
    @Json(name = "max_tokens") val maxTokens: Int = 1024,
    @Json(name = "messages") val messages: List<AnthropicMessageDto>,
    @Json(name = "system") val system: String? = null,
    @Json(name = "temperature") val temperature: Float? = null
)

@JsonClass(generateAdapter = true)
data class AnthropicMessageDto(
    @Json(name = "role") val role: String,
    @Json(name = "content") val content: String
)

@JsonClass(generateAdapter = true)
data class AnthropicResponseDto(
    @Json(name = "id") val id: String? = null,
    @Json(name = "content") val content: List<AnthropicContentBlockDto>? = null,
    @Json(name = "stop_reason") val stopReason: String? = null,
    @Json(name = "usage") val usage: AnthropicUsageDto? = null,
    @Json(name = "error") val error: AnthropicErrorDto? = null
)

@JsonClass(generateAdapter = true)
data class AnthropicContentBlockDto(
    @Json(name = "type") val type: String? = null,
    @Json(name = "text") val text: String? = null
)

@JsonClass(generateAdapter = true)
data class AnthropicUsageDto(
    @Json(name = "input_tokens") val inputTokens: Int? = null,
    @Json(name = "output_tokens") val outputTokens: Int? = null
)

@JsonClass(generateAdapter = true)
data class AnthropicErrorDto(
    @Json(name = "type") val type: String? = null,
    @Json(name = "message") val message: String? = null
)
