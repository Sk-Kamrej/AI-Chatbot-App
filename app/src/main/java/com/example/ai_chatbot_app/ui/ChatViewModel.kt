package com.example.ai_chatbot_app.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.ai_chatbot_app.data.ChatMessage
import com.example.ai_chatbot_app.data.GroqRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class ChatViewModel(
    private val repository: GroqRepository = GroqRepository()
) : ViewModel() {

    private val _messages = MutableStateFlow<List<ChatMessage>>(emptyList())
    val messages: StateFlow<List<ChatMessage>> = _messages.asStateFlow()

    fun sendMessage(userText: String) {
        val trimmed = userText.trim()
        if (trimmed.isEmpty()) return

        val userMsg = ChatMessage(
            message = trimmed,
            isFromUser = true
        )

        val loadingAiMsg = ChatMessage(
            message = "",
            isFromUser = false,
            isLoading = true
        )

        // Capture previous conversation history before adding new user message
        val previousHistory = _messages.value.filter { !it.isLoading && !it.isError }

        val currentList = _messages.value.toMutableList()
        currentList.add(userMsg)
        currentList.add(loadingAiMsg)
        _messages.value = currentList

        viewModelScope.launch {
            val result = repository.generateResponse(trimmed, previousHistory)

            val updatedList = _messages.value.toMutableList()
            if (updatedList.isNotEmpty() && updatedList.last().isLoading) {
                updatedList.removeAt(updatedList.lastIndex)

                result.onSuccess { textResponse ->
                    val aiResponse = ChatMessage(
                        message = textResponse,
                        isFromUser = false
                    )
                    updatedList.add(aiResponse)
                }.onFailure { exception ->
                    val errorMessage = exception.localizedMessage ?: "An unexpected error occurred."
                    val aiErrorResponse = ChatMessage(
                        message = "⚠️ **Error:** $errorMessage",
                        isFromUser = false,
                        isError = true
                    )
                    updatedList.add(aiErrorResponse)
                }

                _messages.value = updatedList
            }
        }
    }

    fun clearChat() {
        _messages.value = emptyList()
    }
}