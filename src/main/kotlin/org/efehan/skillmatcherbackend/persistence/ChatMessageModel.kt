package org.efehan.skillmatcherbackend.persistence

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.JoinColumn
import jakarta.persistence.ManyToOne
import jakarta.persistence.Table
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
) : AuditingBaseEntity()

@Repository
interface ChatMessageRepository : JpaRepository<ChatMessageModel, String> {
    @Query(
        """
          SELECT m FROM ChatMessageModel m
          WHERE m.conversation = :conversation
            AND m.createdDate < :before
          ORDER BY m.createdDate DESC
          """,
    )
    fun findByConversationBefore(
        conversation: ConversationModel,
        before: Instant,
        pageable: Pageable,
    ): List<ChatMessageModel>

    fun findTopByConversationOrderByCreatedDateDesc(conversation: ConversationModel): ChatMessageModel?
}
