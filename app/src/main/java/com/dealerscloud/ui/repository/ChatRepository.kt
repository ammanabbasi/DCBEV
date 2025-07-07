package com.dealerscloud.ui.repository

import com.dealerscloud.integration.DCBEVApiClient
import com.dealerscloud.models.*
import kotlinx.coroutines.flow.*
import javax.inject.Inject
import javax.inject.Singleton
import java.util.UUID

@Singleton
class ChatRepository @Inject constructor(
    private val apiClient: DCBEVApiClient
) {
    private val _messages = MutableStateFlow<List<Message>>(emptyList())
    val messages: StateFlow<List<Message>> = _messages.asStateFlow()
    
    private val _isStreaming = MutableStateFlow(false)
    val isStreaming: StateFlow<Boolean> = _isStreaming.asStateFlow()
    
    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()
    
    /**
     * Add a user message to the conversation
     */
    fun addUserMessage(content: String) {
        val userMessage = Message(
            content = content,
            sender = MessageSender.USER,
            conversationId = UUID.randomUUID().toString()
        )
        _messages.value = _messages.value + userMessage
        _error.value = null
    }
    
    /**
     * Stream assistant reply and update messages in real-time
     */
    suspend fun streamAssistantReply(userMessage: String): Flow<String> {
        _isStreaming.value = true
        _error.value = null
        
        val request = ChatRequest(
            message = userMessage,
            conversationId = UUID.randomUUID().toString()
        )
        
        // Create initial assistant message with empty content
        val assistantMessage = Message(
            content = "",
            sender = MessageSender.AI,
            conversationId = request.conversationId ?: UUID.randomUUID().toString(),
            isStreaming = true
        )
        _messages.value = _messages.value + assistantMessage
        
        return apiClient.streamChatCompletion(request)
            .onEach { delta ->
                // Update the last message (assistant) with streamed content
                val currentMessages = _messages.value.toMutableList()
                val lastMessageIndex = currentMessages.lastIndex
                
                if (lastMessageIndex >= 0 && currentMessages[lastMessageIndex].sender == MessageSender.AI) {
                    val updatedMessage = currentMessages[lastMessageIndex].copy(
                        content = currentMessages[lastMessageIndex].content + delta,
                        isStreaming = true
                    )
                    currentMessages[lastMessageIndex] = updatedMessage
                    _messages.value = currentMessages
                }
            }
            .onCompletion { throwable ->
                _isStreaming.value = false
                
                // Mark the assistant message as complete
                val currentMessages = _messages.value.toMutableList()
                val lastMessageIndex = currentMessages.lastIndex
                
                if (lastMessageIndex >= 0 && currentMessages[lastMessageIndex].sender == MessageSender.AI) {
                    val completedMessage = currentMessages[lastMessageIndex].copy(
                        isStreaming = false
                    )
                    currentMessages[lastMessageIndex] = completedMessage
                    _messages.value = currentMessages
                }
                
                if (throwable != null) {
                    _error.value = "Failed to get response: ${throwable.message}"
                }
            }
            .catch { throwable ->
                _error.value = "Streaming error: ${throwable.message}"
            }
    }
    
    /**
     * Clear all messages
     */
    fun clearMessages() {
        _messages.value = emptyList()
        _error.value = null
    }
    
    /**
     * Stream chat completion from the backend
     */
    fun streamChatCompletion(request: ChatRequest): Flow<String> {
        return apiClient.streamChatCompletion(request)
    }
    
    /**
     * Test backend connection
     */
    suspend fun testConnection(): Boolean {
        return apiClient.testConnection()
    }
}