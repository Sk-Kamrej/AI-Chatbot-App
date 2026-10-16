package com.example.ai_chatbot_app.api

import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.POST

interface GroqApiService {

    @GET("openai/v1/models")
    suspend fun getModels(
        @Header("Authorization") authorization: String
    ): Response<GroqModelListResponse>

    @POST("openai/v1/chat/completions")
    suspend fun createChatCompletion(
        @Header("Authorization") authorization: String,
        @Body request: GroqChatRequest
    ): Response<GroqChatResponse>
}