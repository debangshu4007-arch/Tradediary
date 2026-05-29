package com.example.network

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import retrofit2.http.Body
import retrofit2.http.Header
import retrofit2.http.POST
import java.util.concurrent.TimeUnit

@JsonClass(generateAdapter = true)
data class GroqMessage(
    @Json(name = "role") val role: String,
    @Json(name = "content") val content: String
)

@JsonClass(generateAdapter = true)
data class GroqChatRequest(
    @Json(name = "model") val model: String,
    @Json(name = "messages") val messages: List<GroqMessage>,
    @Json(name = "temperature") val temperature: Float = 0.4f
)

@JsonClass(generateAdapter = true)
data class GroqChoice(
    @Json(name = "message") val message: GroqMessage?
)

@JsonClass(generateAdapter = true)
data class GroqChatResponse(
    @Json(name = "choices") val choices: List<GroqChoice>?
)

interface GroqApiService {
    @POST("openai/v1/chat/completions")
    suspend fun chatCompletions(
        @Header("Authorization") authorization: String,
        @Body request: GroqChatRequest
    ): GroqChatResponse
}

object GroqRetrofitClient {
    private const val BASE_URL = "https://api.groq.com/"

    private val okHttpClient = OkHttpClient.Builder()
        .connectTimeout(60, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .writeTimeout(60, TimeUnit.SECONDS)
        .build()

    val service: GroqApiService by lazy {
        val retrofit = Retrofit.Builder()
            .baseUrl(BASE_URL)
            .client(okHttpClient)
            .addConverterFactory(MoshiConverterFactory.create())
            .build()
        retrofit.create(GroqApiService::class.java)
    }
}

object GroqService {
    private const val MODEL = "llama-3.1-8b-instant"

    private const val SYSTEM_PROMPT =
        "You are a professional trading coach for F&O, stocks, and investing journals. " +
        "Read the structured journal data and find practical loopholes. " +
        "Return a concise terminal-style report in English with light Hinglish-friendly wording. " +
        "Include: 1) best trading day/session/setup, 2) risk-reward issues, " +
        "3) money management or position size warnings, 4) partial-exit quality and whether winners are cut early, " +
        "5) fear/FOMO/revenge behavior impact, 6) stock vs F&O and CE vs PE/Buy vs Sell behavior clues, " +
        "and 7) exactly 3 direct action points. Be specific and do not give generic motivation."

    suspend fun getTradeAnalysis(prompt: String, apiKey: String): String {
        if (apiKey.isBlank()) {
            return "Please add your Groq API key in the AI Coach settings to activate live AI performance analytics."
        }

        val request = GroqChatRequest(
            model = MODEL,
            messages = listOf(
                GroqMessage(role = "system", content = SYSTEM_PROMPT),
                GroqMessage(role = "user", content = prompt)
            )
        )

        return try {
            val response = GroqRetrofitClient.service.chatCompletions("Bearer $apiKey", request)
            response.choices?.firstOrNull()?.message?.content
                ?: "No response generated. Double check your trade logs or inputs."
        } catch (e: Exception) {
            "Analysis Error: ${e.localizedMessage ?: "Connection timed out"}. Please check your connection or API key status."
        }
    }
}
