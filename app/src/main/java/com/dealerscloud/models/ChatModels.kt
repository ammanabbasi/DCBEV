package com.dealerscloud.models

import androidx.compose.runtime.Immutable
import java.time.LocalDateTime
import java.util.UUID

@Immutable
data class Message(
    val id: String = UUID.randomUUID().toString(),
    val content: String,
    val sender: MessageSender,
    val timestamp: LocalDateTime = LocalDateTime.now(),
    val conversationId: String,
    val isStreaming: Boolean = false,
    val userFeedback: String? = null, // "positive" or "negative"
    val tokens: Int? = null,
    val model: String? = "DCBEV-Assistant"
)

enum class MessageSender {
    USER, AI, SYSTEM
}

@Immutable
data class ChatState(
    val messages: List<Message> = emptyList(),
    val isLoading: Boolean = false,
    val isStreaming: Boolean = false,
    val currentStreamingMessage: Message? = null,
    val error: String? = null,
    val conversationId: String? = null
)

sealed class ChatUiEvent {
    data class SendMessage(val content: String) : ChatUiEvent()
    object ClearMessages : ChatUiEvent()
    object RetryLastMessage : ChatUiEvent()
    data class CopyMessage(val messageId: String) : ChatUiEvent()
    data class RegenerateResponse(val messageId: String) : ChatUiEvent()
}

data class ChatRequest(
    val message: String,
    val conversationId: String? = null,
    val dealershipContext: Map<String, Any>? = null
)

data class StreamChunk(
    val content: String,
    val done: Boolean = false
)