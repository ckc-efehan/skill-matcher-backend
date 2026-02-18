package org.efehan.skillmatcherbackend.integration.api

import org.assertj.core.api.Assertions.assertThat
import org.efehan.skillmatcherbackend.core.auth.JwtService
import org.efehan.skillmatcherbackend.core.chat.ChatMessageResponse
import org.efehan.skillmatcherbackend.core.chat.SendMessageRequest
import org.efehan.skillmatcherbackend.persistence.ConversationModel
import org.efehan.skillmatcherbackend.persistence.RoleModel
import org.efehan.skillmatcherbackend.persistence.UserModel
import org.efehan.skillmatcherbackend.testcontainers.AbstractWebSocketIntegrationTest
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.messaging.simp.stomp.StompFrameHandler
import org.springframework.messaging.simp.stomp.StompHeaders
import org.springframework.security.crypto.password.PasswordEncoder
import java.lang.reflect.Type
import java.time.temporal.ChronoUnit
import java.util.concurrent.LinkedBlockingDeque
import java.util.concurrent.TimeUnit

@DisplayName("ChatWebSocketController Integration Tests")
class ChatWebSocketControllerIT : AbstractWebSocketIntegrationTest() {
    @Autowired
    private lateinit var passwordEncoder: PasswordEncoder

    @Autowired
    private lateinit var jwtService: JwtService

    private fun createUserAndGetToken(
        email: String = "alice@firma.de",
        firstName: String = "Alice",
        lastName: String = "Schmidt",
    ): Pair<UserModel, String> {
        val role = roleRepository.findAll().firstOrNull() ?: roleRepository.save(RoleModel("EMPLOYER", null))
        val user =
            UserModel(
                email = email,
                passwordHash = passwordEncoder.encode("Test-Password1!"),
                firstName = firstName,
                lastName = lastName,
                role = role,
            )
        user.isEnabled = true
        userRepository.save(user)
        return user to jwtService.generateAccessToken(user)
    }

    private fun createConversation(
        userOne: UserModel,
        userTwo: UserModel,
    ): ConversationModel {
        val first = if (userOne.id < userTwo.id) userOne else userTwo
        val second = if (userOne.id < userTwo.id) userTwo else userOne
        return conversationRepository.save(ConversationModel(userOne = first, userTwo = second))
    }

    private fun frameHandler(messages: LinkedBlockingDeque<ChatMessageResponse>): StompFrameHandler =
        object : StompFrameHandler {
            override fun getPayloadType(headers: StompHeaders): Type = ChatMessageResponse::class.java

            override fun handleFrame(
                headers: StompHeaders,
                payload: Any?,
            ) {
                if (payload is ChatMessageResponse) {
                    messages.add(payload)
                }
            }
        }

    @Test
    fun `should deliver message to both sender and recipient`() {
        // given
        val (alice, tokenAlice) = createUserAndGetToken()
        val (bob, tokenBob) = createUserAndGetToken(email = "bob@firma.de", firstName = "Bob", lastName = "Mueller")
        val conversation = createConversation(alice, bob)

        val aliceMessages = LinkedBlockingDeque<ChatMessageResponse>()
        val bobMessages = LinkedBlockingDeque<ChatMessageResponse>()

        val aliceSession = connectWithAuth(tokenAlice)
        val bobSession = connectWithAuth(tokenBob)

        aliceSession.subscribe("/user/queue/messages", frameHandler(aliceMessages))
        bobSession.subscribe("/user/queue/messages", frameHandler(bobMessages))
        Thread.sleep(500)

        // when
        val request = SendMessageRequest(conversationId = conversation.id, content = "Hello Bob!")
        aliceSession.send("/app/chat.send", request)

        // then
        val aliceMsg = aliceMessages.poll(5, TimeUnit.SECONDS)
        val bobMsg = bobMessages.poll(5, TimeUnit.SECONDS)

        assertThat(aliceMsg).isNotNull()
        assertThat(aliceMsg!!.content).isEqualTo("Hello Bob!")
        assertThat(aliceMsg.senderId).isEqualTo(alice.id)
        assertThat(aliceMsg.conversationId).isEqualTo(conversation.id)

        assertThat(bobMsg).isNotNull()
        assertThat(bobMsg!!.content).isEqualTo("Hello Bob!")
        assertThat(bobMsg.senderId).isEqualTo(alice.id)
        assertThat(bobMsg.conversationId).isEqualTo(conversation.id)

        // verify DB persistence
        val dbMessages = chatMessageRepository.findAll()
        assertThat(dbMessages).hasSize(1)
        assertThat(dbMessages[0].content).isEqualTo("Hello Bob!")
        assertThat(dbMessages[0].sender.id).isEqualTo(alice.id)
    }

    @Test
    fun `should persist message with correct sentAt`() {
        // given
        val (alice, tokenAlice) = createUserAndGetToken()
        val (bob, _) = createUserAndGetToken(email = "bob@firma.de", firstName = "Bob", lastName = "Mueller")
        val conversation = createConversation(alice, bob)

        val aliceMessages = LinkedBlockingDeque<ChatMessageResponse>()
        val aliceSession = connectWithAuth(tokenAlice)
        aliceSession.subscribe("/user/queue/messages", frameHandler(aliceMessages))
        Thread.sleep(500)

        // when
        val request = SendMessageRequest(conversationId = conversation.id, content = "Timestamp test")
        aliceSession.send("/app/chat.send", request)

        // then
        val msg = aliceMessages.poll(5, TimeUnit.SECONDS)
        assertThat(msg).isNotNull()

        val dbMessage = chatMessageRepository.findAll().first()
        assertThat(dbMessage.content).isEqualTo("Timestamp test")
        assertThat(dbMessage.sentAt).isNotNull()
        assertThat(msg!!.sentAt.truncatedTo(ChronoUnit.MICROS))
            .isEqualTo(dbMessage.sentAt.truncatedTo(ChronoUnit.MICROS))
    }

    @Test
    fun `should not broadcast when validation fails`() {
        // given
        val (alice, tokenAlice) = createUserAndGetToken()
        val (bob, tokenBob) = createUserAndGetToken(email = "bob@firma.de", firstName = "Bob", lastName = "Mueller")
        createConversation(alice, bob)

        val bobMessages = LinkedBlockingDeque<ChatMessageResponse>()
        val aliceSession = connectWithAuth(tokenAlice)
        val bobSession = connectWithAuth(tokenBob)

        bobSession.subscribe("/user/queue/messages", frameHandler(bobMessages))
        Thread.sleep(500)

        // when — send with blank conversationId and content
        val invalidRequest = SendMessageRequest(conversationId = "", content = "")
        aliceSession.send("/app/chat.send", invalidRequest)

        // then
        val msg = bobMessages.poll(2, TimeUnit.SECONDS)
        assertThat(msg).isNull()
        assertThat(chatMessageRepository.findAll()).isEmpty()
    }

    @Test
    fun `should not deliver message to non-member`() {
        // given
        val (alice, tokenAlice) = createUserAndGetToken()
        val (bob, _) = createUserAndGetToken(email = "bob@firma.de", firstName = "Bob", lastName = "Mueller")
        val (_, tokenCharlie) =
            createUserAndGetToken(email = "charlie@firma.de", firstName = "Charlie", lastName = "Weber")
        val conversation = createConversation(alice, bob)

        val charlieMessages = LinkedBlockingDeque<ChatMessageResponse>()
        val aliceSession = connectWithAuth(tokenAlice)
        val charlieSession = connectWithAuth(tokenCharlie)

        charlieSession.subscribe("/user/queue/messages", frameHandler(charlieMessages))
        Thread.sleep(500)

        // when
        val request = SendMessageRequest(conversationId = conversation.id, content = "Secret message")
        aliceSession.send("/app/chat.send", request)

        // then
        val msg = charlieMessages.poll(2, TimeUnit.SECONDS)
        assertThat(msg).isNull()
    }
}
