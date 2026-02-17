package org.efehan.skillmatcherbackend.integration.api

import org.efehan.skillmatcherbackend.core.auth.JwtService
import org.efehan.skillmatcherbackend.core.chat.CreateConversationRequest
import org.efehan.skillmatcherbackend.persistence.ChatMessageModel
import org.efehan.skillmatcherbackend.persistence.ConversationModel
import org.efehan.skillmatcherbackend.persistence.RoleModel
import org.efehan.skillmatcherbackend.persistence.UserModel
import org.efehan.skillmatcherbackend.testcontainers.AbstractIntegrationTest
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.test.web.servlet.get
import org.springframework.test.web.servlet.post
import java.time.Instant

@DisplayName("ChatController Integration Tests")
class ChatControllerIT : AbstractIntegrationTest() {
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

    private fun createMessage(
        conversation: ConversationModel,
        sender: UserModel,
        content: String,
        sentAt: Instant = Instant.now(),
    ): ChatMessageModel =
        chatMessageRepository.save(
            ChatMessageModel(
                conversation = conversation,
                sender = sender,
                content = content,
                sentAt = sentAt,
            ),
        )

    @Test
    fun `should return conversations for authenticated user`() {
        // given
        val (alice, tokenAlice) = createUserAndGetToken()
        val (bob, _) = createUserAndGetToken(email = "bob@firma.de", firstName = "Bob", lastName = "Mueller")
        val conversation = createConversation(alice, bob)
        createMessage(conversation, alice, "Hello Bob!")

        // when & then
        mockMvc
            .get("/api/chat/conversations") {
                header("Authorization", "Bearer $tokenAlice")
            }.andExpect {
                status { isOk() }
                jsonPath("$.length()") { value(1) }
                jsonPath("$[0].id") { value(conversation.id) }
                jsonPath("$[0].partner.firstName") { value("Bob") }
                jsonPath("$[0].lastMessage.content") { value("Hello Bob!") }
                jsonPath("$[0].lastMessage.sentAt") { isNotEmpty() }
            }
    }

    @Test
    fun `should return empty list when user has no conversations`() {
        // given
        val (_, token) = createUserAndGetToken()

        // when & then
        mockMvc
            .get("/api/chat/conversations") {
                header("Authorization", "Bearer $token")
            }.andExpect {
                status { isOk() }
                jsonPath("$.length()") { value(0) }
            }
    }

    @Test
    fun `should return 401 when getting conversations without auth`() {
        // when & then
        mockMvc
            .get("/api/chat/conversations")
            .andExpect {
                status { isUnauthorized() }
            }
    }

    @Test
    fun `should return messages for conversation member`() {
        // given
        val (alice, tokenAlice) = createUserAndGetToken()
        val (bob, _) = createUserAndGetToken(email = "bob@firma.de", firstName = "Bob", lastName = "Mueller")
        val conversation = createConversation(alice, bob)
        val sentAt = Instant.parse("2026-01-15T10:00:00Z")
        createMessage(conversation, alice, "Hello", sentAt)
        createMessage(conversation, bob, "Hi there", sentAt.plusSeconds(60))

        // when & then
        mockMvc
            .get("/api/chat/conversations/${conversation.id}/messages") {
                header("Authorization", "Bearer $tokenAlice")
            }.andExpect {
                status { isOk() }
                jsonPath("$.length()") { value(2) }
                jsonPath("$[0].content") { value("Hi there") }
                jsonPath("$[0].sentAt") { isNotEmpty() }
                jsonPath("$[1].content") { value("Hello") }
            }
    }

    @Test
    fun `should support cursor-based pagination with before param`() {
        // given
        val (alice, tokenAlice) = createUserAndGetToken()
        val (bob, _) = createUserAndGetToken(email = "bob@firma.de", firstName = "Bob", lastName = "Mueller")
        val conversation = createConversation(alice, bob)
        val baseTime = Instant.parse("2026-01-15T10:00:00Z")
        createMessage(conversation, alice, "Message 1", baseTime)
        createMessage(conversation, bob, "Message 2", baseTime.plusSeconds(60))
        createMessage(conversation, alice, "Message 3", baseTime.plusSeconds(120))

        // when & then — fetch only messages before Message 3
        mockMvc
            .get("/api/chat/conversations/${conversation.id}/messages") {
                header("Authorization", "Bearer $tokenAlice")
                param("before", baseTime.plusSeconds(120).toString())
                param("limit", "10")
            }.andExpect {
                status { isOk() }
                jsonPath("$.length()") { value(2) }
                jsonPath("$[0].content") { value("Message 2") }
                jsonPath("$[1].content") { value("Message 1") }
            }
    }

    @Test
    fun `should return 404 when conversation does not exist`() {
        // given
        val (_, token) = createUserAndGetToken()

        // when & then
        mockMvc
            .get("/api/chat/conversations/nonexistent-id/messages") {
                header("Authorization", "Bearer $token")
            }.andExpect {
                status { isNotFound() }
                jsonPath("$.errorCode") { value("CONVERSATION_NOT_FOUND") }
            }
    }

    @Test
    fun `should return 403 when user is not a conversation member`() {
        // given
        val (alice, _) = createUserAndGetToken()
        val (bob, _) = createUserAndGetToken(email = "bob@firma.de", firstName = "Bob", lastName = "Mueller")
        val (charlie, tokenCharlie) =
            createUserAndGetToken(email = "charlie@firma.de", firstName = "Charlie", lastName = "Weber")
        val conversation = createConversation(alice, bob)

        // when & then
        mockMvc
            .get("/api/chat/conversations/${conversation.id}/messages") {
                header("Authorization", "Bearer $tokenCharlie")
            }.andExpect {
                status { isForbidden() }
                jsonPath("$.errorCode") { value("CONVERSATION_ACCESS_DENIED") }
            }
    }

    @Test
    fun `should return 401 when getting messages without auth`() {
        // when & then
        mockMvc
            .get("/api/chat/conversations/some-id/messages")
            .andExpect {
                status { isUnauthorized() }
            }
    }

    @Test
    fun `should create new conversation and return 201`() {
        // given
        val (alice, tokenAlice) = createUserAndGetToken()
        val (bob, _) = createUserAndGetToken(email = "bob@firma.de", firstName = "Bob", lastName = "Mueller")
        val request = CreateConversationRequest(userId = bob.id)

        // when & then
        mockMvc
            .post("/api/chat/conversations") {
                header("Authorization", "Bearer $tokenAlice")
                withBodyRequest(request)
            }.andExpect {
                status { isCreated() }
                jsonPath("$.id") { isNotEmpty() }
                jsonPath("$.partner.id") { value(bob.id) }
                jsonPath("$.partner.firstName") { value("Bob") }
                jsonPath("$.lastMessage") { isEmpty() }
            }
    }

    @Test
    fun `should return existing conversation with 200`() {
        // given
        val (alice, tokenAlice) = createUserAndGetToken()
        val (bob, _) = createUserAndGetToken(email = "bob@firma.de", firstName = "Bob", lastName = "Mueller")
        val existing = createConversation(alice, bob)
        val request = CreateConversationRequest(userId = bob.id)

        // when & then
        mockMvc
            .post("/api/chat/conversations") {
                header("Authorization", "Bearer $tokenAlice")
                withBodyRequest(request)
            }.andExpect {
                status { isOk() }
                jsonPath("$.id") { value(existing.id) }
                jsonPath("$.partner.id") { value(bob.id) }
            }
    }

    @Test
    fun `should return 400 when creating conversation with yourself`() {
        // given
        val (alice, tokenAlice) = createUserAndGetToken()
        val request = CreateConversationRequest(userId = alice.id)

        // when & then
        mockMvc
            .post("/api/chat/conversations") {
                header("Authorization", "Bearer $tokenAlice")
                withBodyRequest(request)
            }.andExpect {
                status { isBadRequest() }
            }
    }

    @Test
    fun `should return 404 when partner user does not exist`() {
        // given
        val (_, token) = createUserAndGetToken()
        val request = CreateConversationRequest(userId = "nonexistent-id")

        // when & then
        mockMvc
            .post("/api/chat/conversations") {
                header("Authorization", "Bearer $token")
                withBodyRequest(request)
            }.andExpect {
                status { isNotFound() }
                jsonPath("$.errorCode") { value("USER_NOT_FOUND") }
            }
    }

    @Test
    fun `should return 400 when userId is blank`() {
        // given
        val (_, token) = createUserAndGetToken()

        // when & then
        mockMvc
            .post("/api/chat/conversations") {
                header("Authorization", "Bearer $token")
                withBodyRequest(mapOf("userId" to "  "))
            }.andExpect {
                status { isBadRequest() }
            }
    }

    @Test
    fun `should return 401 when creating conversation without auth`() {
        // when & then
        mockMvc
            .post("/api/chat/conversations") {
                withBodyRequest(CreateConversationRequest(userId = "some-id"))
            }.andExpect {
                status { isUnauthorized() }
            }
    }
}
