package com.example.ai_chatbot_app.data

import com.example.ai_chatbot_app.BuildConfig
import com.example.ai_chatbot_app.api.GroqApiService
import com.example.ai_chatbot_app.api.GroqChatRequest
import com.example.ai_chatbot_app.api.GroqMessage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit

class GroqRepository {

    private val apiService: GroqApiService

    private val defaultChatModels = listOf(
        "llama-3.1-8b-instant",
        "llama-3.3-70b-versatile",
        "llama3-8b-8192",
        "llama3-70b-8192",
        "mixtral-8x7b-32768"
    )

    init {
        val loggingInterceptor = HttpLoggingInterceptor().apply {
            level = HttpLoggingInterceptor.Level.BODY
        }

        val okHttpClient = OkHttpClient.Builder()
            .addInterceptor(loggingInterceptor)
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .build()

        val retrofit = Retrofit.Builder()
            .baseUrl("https://api.groq.com/")
            .client(okHttpClient)
            .addConverterFactory(GsonConverterFactory.create())
            .build()

        apiService = retrofit.create(GroqApiService::class.java)
    }

    suspend fun generateResponse(
        userMessage: String,
        historyMessages: List<ChatMessage>
    ): Result<String> = withContext(Dispatchers.IO) {
        val apiKey = BuildConfig.GROQ_API_KEY
        if (apiKey.isBlank() || apiKey == "YOUR_GROQ_API_KEY_HERE" || apiKey == "YOUR_GEMINI_API_KEY_HERE") {
            return@withContext Result.failure(
                IllegalStateException("Groq API Key is missing or invalid. Please configure GROQ_API_KEY in local.properties.")
            )
        }

        val authHeader = "Bearer $apiKey"

        // Fetch live models and filter ONLY chat-compatible LLM models (excluding whisper audio models)
        val availableModels = mutableListOf<String>()
        try {
            val modelsResponse = apiService.getModels(authHeader)
            if (modelsResponse.isSuccessful) {
                val modelItems = modelsResponse.body()?.data
                if (!modelItems.isNullOrEmpty()) {
                    val keywords = listOf("llama", "mixtral", "gemma", "qwen", "deepseek")
                    modelItems.filter { item ->
                        val id = item.id.lowercase()
                        val isChatModel = keywords.any { id.contains(it) } && !id.contains("whisper") && !id.contains("vision")
                        (item.active != false) && isChatModel
                    }.forEach {
                        availableModels.add(it.id)
                    }
                }
            } else if (modelsResponse.code() == 401) {
                return@withContext Result.failure(
                    Exception("Invalid Groq API Key (401 Unauthorized). Please check your key at https://console.groq.com/keys")
                )
            }
        } catch (_: Exception) {
            // Fallback if model listing fails
        }

        if (availableModels.isEmpty()) {
            availableModels.addAll(defaultChatModels)
        }

        // Construct clean chat message sequence with system prompt
        val messages = mutableListOf<GroqMessage>()
        messages.add(GroqMessage(role = "system", content = "You are Lumora, a helpful, friendly AI assistant."))

        var lastRole = "system"
        for (msg in historyMessages) {
            if (msg.isLoading || msg.isError || msg.message.isBlank()) continue
            val role = if (msg.isFromUser) "user" else "assistant"

            if (role != lastRole) {
                messages.add(GroqMessage(role = role, content = msg.message))
                lastRole = role
            }
        }

        if (lastRole == "user") {
            val lastIndex = messages.indexOfLast { it.role == "user" }
            if (lastIndex >= 0) {
                messages[lastIndex] = GroqMessage(role = "user", content = userMessage)
            } else {
                messages.add(GroqMessage(role = "user", content = userMessage))
            }
        } else {
            messages.add(GroqMessage(role = "user", content = userMessage))
        }

        var lastError: String? = null

        // Attempt completion with available chat models
        for (modelName in availableModels) {
            try {
                val request = GroqChatRequest(
                    model = modelName,
                    messages = messages
                )

                val response = apiService.createChatCompletion(
                    authorization = authHeader,
                    request = request
                )

                if (response.isSuccessful) {
                    val body = response.body()
                    val text = body?.choices?.firstOrNull()?.message?.content
                    if (!text.isNullOrEmpty()) {
                        return@withContext Result.success(text)
                    }
                } else if (response.code() == 401) {
                    return@withContext Result.failure(
                        Exception("Invalid Groq API Key (401 Unauthorized). Please check your key at https://console.groq.com/keys")
                    )
                } else {
                    val errorBody = response.errorBody()?.string() ?: ""
                    lastError = "Groq API Error (${response.code()}): $errorBody"
                    // Continue loop to try next model
                    continue
                }
            } catch (e: Exception) {
                lastError = e.localizedMessage
            }
        }

        Result.failure(Exception(lastError ?: "Failed to generate response from Groq API."))
    }
}