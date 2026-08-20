package com.flowtasks.app.data.ai.network

import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import java.util.concurrent.TimeUnit

/**
 * Factory for creating secure HTTPS network clients for OpenAI API calls.
 */
object OpenAINetworkClient {

    private const val BASE_URL = "https://api.openai.com/"
    private const val DEFAULT_TIMEOUT_SECONDS = 30L

    fun createApiService(
        timeoutSeconds: Long = DEFAULT_TIMEOUT_SECONDS,
        baseUrl: String = BASE_URL
    ): OpenAIApiService {
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
            .baseUrl(baseUrl)
            .client(okHttpClient)
            .addConverterFactory(MoshiConverterFactory.create(moshi))
            .build()

        return retrofit.create(OpenAIApiService::class.java)
    }
}
