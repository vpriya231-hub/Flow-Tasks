package com.flowtasks.app.data.ai.network

import com.flowtasks.app.data.ai.model.OpenAIRequestDto
import com.flowtasks.app.data.ai.model.OpenAIResponseDto
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.Header
import retrofit2.http.POST

/**
 * Direct HTTPS Retrofit service for OpenAI Chat Completions API.
 */
interface OpenAIApiService {

    @POST("v1/chat/completions")
    suspend fun createChatCompletion(
        @Header("Authorization") authorization: String,
        @Body request: OpenAIRequestDto
    ): Response<OpenAIResponseDto>
}
