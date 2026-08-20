package com.flowtasks.app.data.ai.network

import com.flowtasks.app.data.ai.model.GeminiRequestDto
import com.flowtasks.app.data.ai.model.GeminiResponseDto
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.Header
import retrofit2.http.POST
import retrofit2.http.Path

/**
 * Direct HTTPS Retrofit service for Google Gemini Generative Language API.
 * Uses secure HTTPS transport and passes the decrypted BYOK API key in the request header.
 */
interface GeminiApiService {

    @POST("v1beta/models/{model}:generateContent")
    suspend fun generateContent(
        @Path("model") model: String,
        @Header("x-goog-api-key") apiKey: String,
        @Body request: GeminiRequestDto
    ): Response<GeminiResponseDto>
}
