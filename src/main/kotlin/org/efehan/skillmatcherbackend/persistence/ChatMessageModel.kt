package org.efehan.skillmatcherbackend.persistence

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.JoinColumn
import jakarta.persistence.ManyToOne
import jakarta.persistence.Table
import org.efehan.skillmatcherbackend.core.chat.ChatMessageResponse
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.stereotype.Repository
import java.time.Instant

@Entity
@Table(name = "chat_messages")
class ChatMessageModel(
    @ManyToOne(optional = false)
    @JoinColumn(name = "conversation_id", nullable = false)
    val conversation: ConversationModel,
    @ManyToOne(optional = false)
    @JoinColumn(name = "sender_id", nullable = false)
    val sender: UserModel,
    @Column(name = "content", nullable = false, columnDefinition = "TEXT")
    val content: String,
    @Column(name = "sent_at", nullable = false)
    val sentAt: Instant,
) : AuditingBaseEntity() {
    fun toDTO() =
        ChatMessageResponse(
            id = id,
            conversationId = conversation.id,
            senderId = sender.id,
            content = content,
            sentAt = sentAt,
        )
}

@Repository
interface ChatMessageRepository : JpaRepository<ChatMessageModel, String> {
    @Query(
        """
          SELECT m FROM ChatMessageModel m
          WHERE m.conversation = :conversation
            AND m.sentAt < :before
          ORDER BY m.sentAt DESC
          """,
    )
    fun findByConversationBefore(
        conversation: ConversationModel,
        before: Instant,
        pageable: Pageable,
    ): List<ChatMessageModel>

    fun findTopByConversationOrderBySentAtDesc(conversation: ConversationModel): ChatMessageModel?

    @Query(
        """
          SELECT m FROM ChatMessageModel m
          WHERE m.conversation IN :conversations
            AND m.sentAt = (
                SELECT MAX(m2.sentAt) FROM ChatMessageModel m2
                WHERE m2.conversation = m.conversation
            )
          """,
    )
    fun findLastMessagesByConversations(conversations: List<ConversationModel>): List<ChatMessageModel>
}
