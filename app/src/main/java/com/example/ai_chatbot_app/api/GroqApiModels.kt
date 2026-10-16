package com.example.ai_chatbot_app.api

import com.google.gson.annotations.SerializedName

data class GroqChatRequest(
    @SerializedName("model") val model: String = "llama-3.1-8b-instant",
    @SerializedName("messages") val messages: List<GroqMessage>,
    @SerializedName("temperature") val temperature: Double = 0.7
)

data class GroqMessage(
    @SerializedName("role") val role: String,
    @SerializedName("content") val content: String
)

data class GroqChatResponse(
    @SerializedName("id") val id: String?,
    @SerializedName("choices") val choices: List<GroqChoice>?,
    @SerializedName("error") val error: GroqError?
)

data class GroqChoice(
    @SerializedName("message") val message: GroqMessage?
)

data class GroqError(
    @SerializedName("message") val message: String?,
    @SerializedName("type") val type: String?,
    @SerializedName("code") val code: String?
)

data class GroqModelListResponse(
    @SerializedName("data") val data: List<GroqModelItem>?
)

data class GroqModelItem(
    @SerializedName("id") val id: String,
    @SerializedName("active") val active: Boolean? = true
)