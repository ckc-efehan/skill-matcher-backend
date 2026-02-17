package org.efehan.skillmatcherbackend.service

import io.mockk.every
import io.mockk.impl.annotations.MockK
import io.mockk.junit5.MockKExtension
import io.mockk.slot
import io.mockk.verify
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.efehan.skillmatcherbackend.core.chat.ChatService
import org.efehan.skillmatcherbackend.exception.GlobalErrorCode
import org.efehan.skillmatcherbackend.persistence.ChatMessageModel
import org.efehan.skillmatcherbackend.persistence.ChatMessageRepository
import org.efehan.skillmatcherbackend.persistence.ConversationModel
import org.efehan.skillmatcherbackend.persistence.ConversationRepository
import org.efehan.skillmatcherbackend.persistence.RoleModel
import org.efehan.skillmatcherbackend.persistence.UserModel
import org.efehan.skillmatcherbackend.persistence.UserRepository
import org.efehan.skillmatcherbackend.shared.exceptions.AccessDeniedException
import org.efehan.skillmatcherbackend.shared.exceptions.EntryNotFoundException
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.springframework.data.domain.PageRequest
import org.springframework.messaging.simp.SimpMessagingTemplate
import java.time.Instant
import java.util.Optional

@ExtendWith(MockKExtension::class)
@DisplayName("ChatService Unit Tests")
class ChatServiceTest {
    @MockK
    private lateinit var conversationRepo: ConversationRepository

    @MockK
    private lateinit var messageRepo: ChatMessageRepository

    @MockK
    private lateinit var userRepo: UserRepository

    @MockK
    private lateinit var messagingTemplate: SimpMessagingTemplate

    private lateinit var chatService: ChatService

    private val role = RoleModel("EMPLOYER", null)

    private val userA =
        UserModel(
            email = "alice@firma.de",
            passwordHash = "hashed",
            firstName = "Alice",
            lastName = "Schmidt",
            role = role,
        )

    private val userB =
        UserModel(
            email = "bob@firma.de",
            passwordHash = "hashed",
            firstName = "Bob",
            lastName = "Mueller",
            role = role,
        )

    @BeforeEach
    fun setUp() {
        chatService = ChatService(conversationRepo, messageRepo, userRepo, messagingTemplate)
    }

    @Test
    fun `getConversations returns conversations for user`() {
        // given
        val conversation = ConversationModel(userOne = userA, userTwo = userB)
        every { conversationRepo.findByUser(userA) } returns listOf(conversation)

        // when
        val result = chatService.getConversations(userA)

        // then
        assertThat(result).hasSize(1)
        assertThat(result[0]).isEqualTo(conversation)
    }

    @Test
    fun `getConversations returns empty list when no conversations exist`() {
        // given
        every { conversationRepo.findByUser(userA) } returns emptyList()

        // when
        val result = chatService.getConversations(userA)

        // then
        assertThat(result).isEmpty()
    }

    @Test
    fun `getLastMessages returns map keyed by conversation id`() {
        // given
        val conversation = ConversationModel(userOne = userA, userTwo = userB)
        val message = ChatMessageModel(conversation = conversation, sender = userA, content = "Hello", sentAt = Instant.now())
        every { messageRepo.findLastMessagesByConversations(listOf(conversation)) } returns listOf(message)

        // when
        val result = chatService.getLastMessages(listOf(conversation))

        // then
        assertThat(result).hasSize(1)
        assertThat(result[conversation.id]).isEqualTo(message)
    }

    @Test
    fun `getLastMessages returns empty map for empty input`() {
        // given
        every { messageRepo.findLastMessagesByConversations(emptyList()) } returns emptyList()

        // when
        val result = chatService.getLastMessages(emptyList())

        // then
        assertThat(result).isEmpty()
    }

    @Test
    fun `getLastMessage returns message when exists`() {
        // given
        val conversation = ConversationModel(userOne = userA, userTwo = userB)
        val message = ChatMessageModel(conversation = conversation, sender = userA, content = "Hello", sentAt = Instant.now())
        every { messageRepo.findTopByConversationOrderBySentAtDesc(conversation) } returns message

        // when
        val result = chatService.getLastMessage(conversation)

        // then
        assertThat(result).isEqualTo(message)
    }

    @Test
    fun `getLastMessage returns null when no messages exist`() {
        // given
        val conversation = ConversationModel(userOne = userA, userTwo = userB)
        every { messageRepo.findTopByConversationOrderBySentAtDesc(conversation) } returns null

        // when
        val result = chatService.getLastMessage(conversation)

        // then
        assertThat(result).isNull()
    }

    @Test
    fun `getMessages returns messages for conversation member`() {
        // given
        val conversation = ConversationModel(userOne = userA, userTwo = userB)
        val before = Instant.now()
        val message = ChatMessageModel(conversation = conversation, sender = userB, content = "Hi", sentAt = Instant.now())
        every { conversationRepo.findById(conversation.id) } returns Optional.of(conversation)
        every { messageRepo.findByConversationBefore(conversation, before, PageRequest.of(0, 20)) } returns listOf(message)

        // when
        val result = chatService.getMessages(userA, conversation.id, before, 20)

        // then
        assertThat(result).hasSize(1)
        assertThat(result[0].content).isEqualTo("Hi")
    }

    @Test
    fun `getMessages throws EntryNotFoundException when conversation not found`() {
        // given
        every { conversationRepo.findById("nonexistent") } returns Optional.empty()

        // then
        assertThatThrownBy {
            chatService.getMessages(userA, "nonexistent", Instant.now(), 20)
        }.isInstanceOf(EntryNotFoundException::class.java)
            .satisfies({ ex ->
                val e = ex as EntryNotFoundException
                assertThat(e.errorCode).isEqualTo(GlobalErrorCode.CONVERSATION_NOT_FOUND)
            })
    }

    @Test
    fun `getMessages throws AccessDeniedException when user is not a member`() {
        // given
        val otherUser =
            UserModel(
                email = "other@firma.de",
                passwordHash = "hashed",
                firstName = "Other",
                lastName = "User",
                role = role,
            )
        val conversation = ConversationModel(userOne = userA, userTwo = userB)
        every { conversationRepo.findById(conversation.id) } returns Optional.of(conversation)

        // then
        assertThatThrownBy {
            chatService.getMessages(otherUser, conversation.id, Instant.now(), 20)
        }.isInstanceOf(AccessDeniedException::class.java)
            .satisfies({ ex ->
                val e = ex as AccessDeniedException
                assertThat(e.errorCode).isEqualTo(GlobalErrorCode.CONVERSATION_ACCESS_DENIED)
            })
    }

    @Test
    fun `createConversation creates new conversation when none exists`() {
        // given
        every { userRepo.findById(userB.id) } returns Optional.of(userB)
        every { conversationRepo.findByUsers(userA, userB) } returns null
        every { conversationRepo.save(any()) } returnsArgument 0

        // when
        val (conversation, created) = chatService.createConversation(userA, userB.id)

        // then
        assertThat(created).isTrue()
        verify(exactly = 1) { conversationRepo.save(any()) }
    }

    @Test
    fun `createConversation returns existing conversation when already exists`() {
        // given
        val existing = ConversationModel(userOne = userA, userTwo = userB)
        every { userRepo.findById(userB.id) } returns Optional.of(userB)
        every { conversationRepo.findByUsers(userA, userB) } returns existing

        // when
        val (conversation, created) = chatService.createConversation(userA, userB.id)

        // then
        assertThat(created).isFalse()
        assertThat(conversation).isEqualTo(existing)
        verify(exactly = 0) { conversationRepo.save(any()) }
    }

    @Test
    fun `createConversation orders users by id`() {
        // given
        every { userRepo.findById(userB.id) } returns Optional.of(userB)
        every { conversationRepo.findByUsers(userA, userB) } returns null
        val slot = slot<ConversationModel>()
        every { conversationRepo.save(capture(slot)) } returnsArgument 0

        // when
        chatService.createConversation(userA, userB.id)

        // then
        val saved = slot.captured
        val expectedFirst = if (userA.id < userB.id) userA else userB
        val expectedSecond = if (userA.id < userB.id) userB else userA
        assertThat(saved.userOne).isEqualTo(expectedFirst)
        assertThat(saved.userTwo).isEqualTo(expectedSecond)
    }

    @Test
    fun `createConversation throws IllegalArgumentException when chatting with yourself`() {
        // then
        assertThatThrownBy {
            chatService.createConversation(userA, userA.id)
        }.isInstanceOf(IllegalArgumentException::class.java)
            .hasMessage("Cannot create a conversation with yourself.")

        verify(exactly = 0) { conversationRepo.save(any()) }
    }

    @Test
    fun `createConversation throws EntryNotFoundException when partner not found`() {
        // given
        every { userRepo.findById("nonexistent") } returns Optional.empty()

        // then
        assertThatThrownBy {
            chatService.createConversation(userA, "nonexistent")
        }.isInstanceOf(EntryNotFoundException::class.java)
            .satisfies({ ex ->
                val e = ex as EntryNotFoundException
                assertThat(e.errorCode).isEqualTo(GlobalErrorCode.USER_NOT_FOUND)
            })

        verify(exactly = 0) { conversationRepo.save(any()) }
    }

    @Test
    fun `sendMessage saves message and sends to both users via websocket`() {
        // given
        val conversation = ConversationModel(userOne = userA, userTwo = userB)
        every { conversationRepo.findById(conversation.id) } returns Optional.of(conversation)
        every { messageRepo.save(any()) } returnsArgument 0
        every { messagingTemplate.convertAndSendToUser(any<String>(), any(), any()) } returns Unit

        // when
        val result = chatService.sendMessage(userA, conversation.id, "Hello Bob!")

        // then
        assertThat(result.content).isEqualTo("Hello Bob!")
        assertThat(result.sender).isEqualTo(userA)
        assertThat(result.conversation).isEqualTo(conversation)
        assertThat(result.sentAt).isNotNull()
        verify(exactly = 1) { messageRepo.save(any()) }
        verify(exactly = 1) { messagingTemplate.convertAndSendToUser(userB.email, "/queue/messages", any()) }
        verify(exactly = 1) { messagingTemplate.convertAndSendToUser(userA.email, "/queue/messages", any()) }
    }

    @Test
    fun `sendMessage throws EntryNotFoundException when conversation not found`() {
        // given
        every { conversationRepo.findById("nonexistent") } returns Optional.empty()

        // then
        assertThatThrownBy {
            chatService.sendMessage(userA, "nonexistent", "Hello")
        }.isInstanceOf(EntryNotFoundException::class.java)
            .satisfies({ ex ->
                val e = ex as EntryNotFoundException
                assertThat(e.errorCode).isEqualTo(GlobalErrorCode.CONVERSATION_NOT_FOUND)
            })

        verify(exactly = 0) { messageRepo.save(any()) }
    }

    @Test
    fun `sendMessage throws AccessDeniedException when user is not a member`() {
        // given
        val otherUser =
            UserModel(
                email = "other@firma.de",
                passwordHash = "hashed",
                firstName = "Other",
                lastName = "User",
                role = role,
            )
        val conversation = ConversationModel(userOne = userA, userTwo = userB)
        every { conversationRepo.findById(conversation.id) } returns Optional.of(conversation)

        // then
        assertThatThrownBy {
            chatService.sendMessage(otherUser, conversation.id, "Hello")
        }.isInstanceOf(AccessDeniedException::class.java)
            .satisfies({ ex ->
                val e = ex as AccessDeniedException
                assertThat(e.errorCode).isEqualTo(GlobalErrorCode.CONVERSATION_ACCESS_DENIED)
            })

        verify(exactly = 0) { messageRepo.save(any()) }
    }

    @Test
    fun `sendMessage sends to correct recipient when userTwo sends`() {
        // given
        val conversation = ConversationModel(userOne = userA, userTwo = userB)
        every { conversationRepo.findById(conversation.id) } returns Optional.of(conversation)
        every { messageRepo.save(any()) } returnsArgument 0
        every { messagingTemplate.convertAndSendToUser(any<String>(), any(), any()) } returns Unit

        // when
        chatService.sendMessage(userB, conversation.id, "Hello Alice!")

        // then
        verify(exactly = 1) { messagingTemplate.convertAndSendToUser(userA.email, "/queue/messages", any()) }
        verify(exactly = 1) { messagingTemplate.convertAndSendToUser(userB.email, "/queue/messages", any()) }
    }
}
