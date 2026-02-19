package org.efehan.skillmatcherbackend.fixtures.builder

import org.efehan.skillmatcherbackend.persistence.ChatMessageModel
import org.efehan.skillmatcherbackend.persistence.ConversationModel
import org.efehan.skillmatcherbackend.persistence.UserModel
import java.time.Instant

class ChatMessageBuilder {
    fun build(
        conversation: ConversationModel = ConversationBuilder().build(),
        sender: UserModel = UserBuilder().build(),
        content: String = "Hello",
        sentAt: Instant = Instant.now(),
    ): ChatMessageModel =
        ChatMessageModel(
            conversation = conversation,
            sender = sender,
            content = content,
            sentAt = sentAt,
        )
}
