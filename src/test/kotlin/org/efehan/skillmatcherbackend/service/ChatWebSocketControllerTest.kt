package org.efehan.skillmatcherbackend.service

import io.mockk.every
import io.mockk.impl.annotations.MockK
import io.mockk.junit5.MockKExtension
import io.mockk.mockk
import io.mockk.verify
import jakarta.validation.ConstraintViolation
import jakarta.validation.Validator
import org.efehan.skillmatcherbackend.core.auth.SecurityUser
import org.efehan.skillmatcherbackend.core.chat.ChatService
import org.efehan.skillmatcherbackend.core.chat.ChatWebSocketController
import org.efehan.skillmatcherbackend.core.chat.SendMessageRequest
import org.efehan.skillmatcherbackend.persistence.ChatMessageModel
import org.efehan.skillmatcherbackend.persistence.ConversationModel
import org.efehan.skillmatcherbackend.persistence.RoleModel
import org.efehan.skillmatcherbackend.persistence.UserModel
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import java.time.Instant

@ExtendWith(MockKExtension::class)
@DisplayName("ChatWebSocketController Unit Tests")
class ChatWebSocketControllerTest {
    @MockK
    private lateinit var chatService: ChatService

    @MockK
    private lateinit var validator: Validator

    private lateinit var controller: ChatWebSocketController

    private val role = RoleModel("EMPLOYER", null)

    private val user =
        UserModel(
            email = "alice@firma.de",
            passwordHash = "hashed",
            firstName = "Alice",
            lastName = "Schmidt",
            role = role,
        )

    private val securityUser = SecurityUser(user)
    private val principal = UsernamePasswordAuthenticationToken(securityUser, null, securityUser.authorities)

    @BeforeEach
    fun setUp() {
        controller = ChatWebSocketController(chatService, validator)
    }

    @Test
    fun `sendMessage delegates to chatService when request is valid`() {
        // given
        val request = SendMessageRequest(conversationId = "conv-1", content = "Hello!")
        val conversation = ConversationModel(userOne = user, userTwo = user)
        val message = ChatMessageModel(conversation = conversation, sender = user, content = "Hello!", sentAt = Instant.now())
        every { validator.validate(request) } returns emptySet()
        every { chatService.sendMessage(user, "conv-1", "Hello!") } returns message

        // when
        controller.sendMessage(principal, request)

        // then
        verify(exactly = 1) { chatService.sendMessage(user, "conv-1", "Hello!") }
    }

    @Test
    fun `sendMessage does not call chatService when validation fails`() {
        // given
        val request = SendMessageRequest(conversationId = "", content = "")
        val violation = mockk<ConstraintViolation<SendMessageRequest>>()
        every { validator.validate(request) } returns setOf(violation)

        // when
        controller.sendMessage(principal, request)

        // then
        verify(exactly = 0) { chatService.sendMessage(any(), any(), any()) }
    }
}
