package com.flowtasks.app.data.ai.network

import com.flowtasks.app.data.ai.model.AnthropicRequestDto
import com.flowtasks.app.data.ai.model.AnthropicResponseDto
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.Header
import retrofit2.http.POST

/**
 * Direct HTTPS Retrofit service for Anthropic Messages API.
 */
interface AnthropicApiService {

    @POST("v1/messages")
    suspend fun createMessage(
        @Header("x-api-key") apiKey: String,
        @Header("anthropic-version") anthropicVersion: String = "2023-06-01",
        @Body request: AnthropicRequestDto
    ): Response<AnthropicResponseDto>
}
