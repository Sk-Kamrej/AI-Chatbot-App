package com.example.ai_chatbot_app.data

import com.example.ai_chatbot_app.BuildConfig
import com.google.ai.client.generativeai.GenerativeModel
import com.google.ai.client.generativeai.type.Content
import com.google.ai.client.generativeai.type.content
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class GeminiRepository {

    private var generativeModel: GenerativeModel? = null

    init {
        val apiKey = BuildConfig.GEMINI_API_KEY
        if (apiKey.isNotBlank() && apiKey != "YOUR_GEMINI_API_KEY_HERE") {
            generativeModel = GenerativeModel(
                modelName = "gemini-2.5-flash",
                apiKey = apiKey
            )
        }
    }

    suspend fun generateResponse(
        userMessage: String,
        historyMessages: List<ChatMessage>
    ): Result<String> = withContext(Dispatchers.IO) {
        val model = generativeModel
            ?: return@withContext Result.failure(
                IllegalStateException("Gemini API Key is missing or invalid. Please configure GEMINI_API_KEY in local.properties.")
            )

        try {
            val historyList = mutableListOf<Content>()
            for (msg in historyMessages) {
                if (msg.isLoading || msg.isError || msg.message.isBlank()) continue
                val role = if (msg.isFromUser) "user" else "model"
                historyList.add(content(role = role) { text(msg.message) })
            }

            val chat = model.startChat(history = historyList)
            val response = chat.sendMessage(userMessage)

            val text = response.text
            if (text.isNullOrEmpty()) {
                Result.failure(Exception("Received empty response from Gemini."))
            } else {
                Result.success(text)
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}