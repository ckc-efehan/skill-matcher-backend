package org.efehan.skillmatcherbackend.core.chat

import jakarta.validation.Validator
import org.efehan.skillmatcherbackend.core.auth.SecurityUser
import org.springframework.messaging.handler.annotation.MessageMapping
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.stereotype.Controller

@Controller
class ChatWebSocketController(
    private val chatService: ChatService,
    private val validator: Validator,
) {
    @MessageMapping("/chat.send")
    fun sendMessage(
        @AuthenticationPrincipal securityUser: SecurityUser,
        request: SendMessageRequest,
    ) {
        val violations = validator.validate(request)
        if (violations.isNotEmpty()) {
            return
        }

        chatService.sendMessage(securityUser.user, request.conversationId, request.content)
    }
}
