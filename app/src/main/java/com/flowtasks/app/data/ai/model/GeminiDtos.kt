package com.flowtasks.app.data.ai.model

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class GeminiRequestDto(
    @Json(name = "contents") val contents: List<GeminiContentDto>,
    @Json(name = "systemInstruction") val systemInstruction: GeminiContentDto? = null,
    @Json(name = "generationConfig") val generationConfig: GeminiGenerationConfigDto? = null
)

@JsonClass(generateAdapter = true)
data class GeminiContentDto(
    @Json(name = "role") val role: String = "user",
    @Json(name = "parts") val parts: List<GeminiPartDto>
)

@JsonClass(generateAdapter = true)
data class GeminiPartDto(
    @Json(name = "text") val text: String
)

@JsonClass(generateAdapter = true)
data class GeminiGenerationConfigDto(
    @Json(name = "maxOutputTokens") val maxOutputTokens: Int? = null,
    @Json(name = "thinkingConfig") val thinkingConfig: GeminiThinkingConfigDto? = null
)

@JsonClass(generateAdapter = true)
data class GeminiThinkingConfigDto(
    @Json(name = "thinkingLevel") val thinkingLevel: String? = null
)

@JsonClass(generateAdapter = true)
data class GeminiResponseDto(
    @Json(name = "candidates") val candidates: List<GeminiCandidateDto>? = null,
    @Json(name = "usageMetadata") val usageMetadata: GeminiUsageMetadataDto? = null,
    @Json(name = "error") val error: GeminiErrorDto? = null
)

@JsonClass(generateAdapter = true)
data class GeminiCandidateDto(
    @Json(name = "content") val content: GeminiContentDto? = null,
    @Json(name = "finishReason") val finishReason: String? = null
)

@JsonClass(generateAdapter = true)
data class GeminiUsageMetadataDto(
    @Json(name = "promptTokenCount") val promptTokenCount: Int? = null,
    @Json(name = "candidatesTokenCount") val candidatesTokenCount: Int? = null,
    @Json(name = "totalTokenCount") val totalTokenCount: Int? = null
)

@JsonClass(generateAdapter = true)
data class GeminiErrorDto(
    @Json(name = "code") val code: Int? = null,
    @Json(name = "message") val message: String? = null,
    @Json(name = "status") val status: String? = null
)
