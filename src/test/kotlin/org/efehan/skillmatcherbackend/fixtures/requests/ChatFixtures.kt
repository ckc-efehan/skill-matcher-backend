package org.efehan.skillmatcherbackend.fixtures.requests

import org.efehan.skillmatcherbackend.core.chat.CreateConversationRequest
import org.efehan.skillmatcherbackend.core.chat.SendMessageRequest

object ChatFixtures {
    fun buildCreateConversationRequest(userId: String = "some-user-id"): CreateConversationRequest =
        CreateConversationRequest(
            userId = userId,
        )

    fun buildSendMessageRequest(
        conversationId: String = "some-conversation-id",
        content: String = "Hello!",
    ): SendMessageRequest =
        SendMessageRequest(
            conversationId = conversationId,
            content = content,
        )
}
