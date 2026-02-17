package org.efehan.skillmatcherbackend.core.chat

import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Size
import java.time.Instant

data class CreateConversationRequest(
    @field:NotBlank
    val userId: String,
)

data class SendMessageRequest(
    @field:NotBlank
    val conversationId: String,
    @field:NotBlank
    @field:Size(max = 5000)
    val content: String,
)

data class ConversationResponse(
    val id: String,
    val partner: ChatUserResponse,
    val lastMessage: ChatMessageResponse?,
    val createdDate: Instant,
)

data class ChatMessageResponse(
    val id: String,
    val conversationId: String,
    val senderId: String,
    val content: String,
    val sentAt: Instant,
)

data class ChatUserResponse(
    val id: String,
    val firstName: String,
    val lastName: String,
)
