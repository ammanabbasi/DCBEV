package com.dealerscloud.ui.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.dealerscloud.models.ChatState
import com.dealerscloud.models.ChatUiEvent
import com.dealerscloud.ui.repository.ChatRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ChatViewModel @Inject constructor(
    private val chatRepository: ChatRepository
) : ViewModel() {
    
    // UI Events Channel
    private val _uiEvents = Channel<ChatUiEvent>()
    val uiEvents = _uiEvents.receiveAsFlow()
    
    // Combined Chat State
    val chatState: StateFlow<ChatState> = combine(
        chatRepository.messages,
        chatRepository.isStreaming,
        chatRepository.error
    ) { messages, isStreaming, error ->
        ChatState(
            messages = messages,
            isStreaming = isStreaming,
            error = error,
            currentStreamingMessage = if (isStreaming) messages.lastOrNull() else null
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = ChatState()
    )
    
    /**
     * Send a message to the AI assistant
     */
    fun sendMessage(content: String) {
        if (content.isBlank()) return
        
        viewModelScope.launch {
            try {
                // Add user message
                chatRepository.addUserMessage(content.trim())
                
                // Stream assistant response
                chatRepository.streamAssistantReply(content.trim())
                    .collect { /* Response is handled in repository */ }
                    
            } catch (e: Exception) {
                // Error handling is done in repository
            }
        }
    }
    
    /**
     * Clear all messages in the conversation
     */
    fun clearMessages() {
        chatRepository.clearMessages()
    }
    
    /**
     * Retry the last message
     */
    fun retryLastMessage() {
        val messages = chatState.value.messages
        val lastUserMessage = messages.findLast { it.sender.name == "USER" }
        
        if (lastUserMessage != null) {
            // Remove the last assistant message if it exists and failed
            val lastAssistantMessage = messages.lastOrNull()
            if (lastAssistantMessage?.sender?.name == "ASSISTANT") {
                // Remove the last assistant message and retry
                sendMessage(lastUserMessage.content)
            }
        }
    }
    
    /**
     * Test backend connectivity
     */
    fun testConnection() {
        viewModelScope.launch {
            chatRepository.testConnection()
        }
    }
    
    /**
     * Copy message content to clipboard
     */
    fun copyMessage(messageId: String) {
        viewModelScope.launch {
            _uiEvents.send(ChatUiEvent.CopyMessage(messageId))
        }
    }
    
    /**
     * Regenerate assistant response
     */
    fun regenerateResponse(messageId: String) {
        val messages = chatState.value.messages
        val messageIndex = messages.indexOfFirst { it.id == messageId }
        
        if (messageIndex > 0) {
            val previousUserMessage = messages[messageIndex - 1]
            if (previousUserMessage.sender.name == "USER") {
                sendMessage(previousUserMessage.content)
            }
        }
    }
}