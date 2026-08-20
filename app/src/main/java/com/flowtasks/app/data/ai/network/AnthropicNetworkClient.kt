package com.flowtasks.app.data.ai.network

import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import java.util.concurrent.TimeUnit

/**
 * Factory for creating secure HTTPS network clients for Anthropic API calls.
 */
object AnthropicNetworkClient {

    private const val BASE_URL = "https://api.anthropic.com/"
    private const val DEFAULT_TIMEOUT_SECONDS = 30L

    fun createApiService(
        timeoutSeconds: Long = DEFAULT_TIMEOUT_SECONDS
    ): AnthropicApiService {
        val okHttpClient = OkHttpClient.Builder()
            .connectTimeout(timeoutSeconds, TimeUnit.SECONDS)
            .readTimeout(timeoutSeconds, TimeUnit.SECONDS)
            .writeTimeout(timeoutSeconds, TimeUnit.SECONDS)
            .retryOnConnectionFailure(false)
            .build()

        val moshi = Moshi.Builder()
            .add(KotlinJsonAdapterFactory())
            .build()

        val retrofit = Retrofit.Builder()
            .baseUrl(BASE_URL)
            .client(okHttpClient)
            .addConverterFactory(MoshiConverterFactory.create(moshi))
            .build()

        return retrofit.create(AnthropicApiService::class.java)
    }
}
