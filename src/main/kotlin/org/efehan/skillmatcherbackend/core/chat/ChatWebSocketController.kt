package org.efehan.skillmatcherbackend.core.chat

import jakarta.validation.Validator
import org.efehan.skillmatcherbackend.core.auth.SecurityUser
import org.springframework.messaging.handler.annotation.MessageMapping
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.stereotype.Controller
import java.security.Principal

@Controller
class ChatWebSocketController(
    private val chatService: ChatService,
    private val validator: Validator,
) {
    @MessageMapping("/chat.send")
    fun sendMessage(
        principal: Principal,
        request: SendMessageRequest,
    ) {
        val violations = validator.validate(request)
        if (violations.isNotEmpty()) {
            return
        }

        val authToken = principal as? UsernamePasswordAuthenticationToken ?: return
        val securityUser = authToken.principal as? SecurityUser ?: return
        chatService.sendMessage(securityUser.user, request.conversationId, request.content)
    }
}
