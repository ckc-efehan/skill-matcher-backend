package org.efehan.skillmatcherbackend.service

import io.mockk.every
import io.mockk.impl.annotations.MockK
import io.mockk.junit5.MockKExtension
import io.mockk.just
import io.mockk.runs
import io.mockk.slot
import io.mockk.verify
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.efehan.skillmatcherbackend.core.admin.AdminUserService
import org.efehan.skillmatcherbackend.core.admin.CreateUserRequest
import org.efehan.skillmatcherbackend.core.invitation.InvitationService
import org.efehan.skillmatcherbackend.exception.GlobalErrorCode
import org.efehan.skillmatcherbackend.persistence.RoleModel
import org.efehan.skillmatcherbackend.persistence.RoleRepository
import org.efehan.skillmatcherbackend.persistence.UserModel
import org.efehan.skillmatcherbackend.persistence.UserRepository
import org.efehan.skillmatcherbackend.shared.exceptions.DuplicateEntryException
import org.efehan.skillmatcherbackend.shared.exceptions.EntryNotFoundException
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith

@ExtendWith(MockKExtension::class)
@DisplayName("Admin User Service Unit Tests")
class AdminUserServiceTest {
    @MockK
    private lateinit var userRepository: UserRepository

    @MockK
    private lateinit var roleRepository: RoleRepository

    @MockK
    private lateinit var invitationService: InvitationService

    private lateinit var adminUserService: AdminUserService

    @BeforeEach
    fun setUp() {
        adminUserService = AdminUserService(userRepository, roleRepository, invitationService)
    }

    @Test
    fun `createUser successfully creates user and sends invitation`() {
        // given
        val request =
            CreateUserRequest(
                firstName = "Max",
                lastName = "Mustermann",
                email = "max.mustermann@firma.de",
                role = "EMPLOYER",
            )
        val role = RoleModel("EMPLOYER", null)
        val userSlot = slot<UserModel>()

        every { userRepository.existsByEmail(request.email) } returns false
        every { roleRepository.findByName("EMPLOYER") } returns role
        every { userRepository.existsByUsername("max.mustermann") } returns false
        every { userRepository.save(capture(userSlot)) } returnsArgument 0
        every { invitationService.createAndSendInvitation(any()) } just runs

        // when
        val result = adminUserService.createUser(request)

        // then
        assertThat(result.username).isEqualTo("max.mustermann")
        assertThat(result.email).isEqualTo("max.mustermann@firma.de")
        assertThat(result.firstName).isEqualTo("Max")
        assertThat(result.lastName).isEqualTo("Mustermann")
        assertThat(result.role).isEqualTo("EMPLOYER")

        val savedUser = userSlot.captured
        assertThat(savedUser.isEnabled).isFalse()
        assertThat(savedUser.passwordHash).isNull()

        verify(exactly = 1) { invitationService.createAndSendInvitation(any()) }
    }

    @Test
    fun `createUser appends suffix when username already exists`() {
        // given
        val request =
            CreateUserRequest(
                firstName = "Max",
                lastName = "Mustermann",
                email = "max.mustermann2@firma.de",
                role = "EMPLOYER",
            )
        val role = RoleModel("EMPLOYER", null)

        every { userRepository.existsByEmail(request.email) } returns false
        every { roleRepository.findByName("EMPLOYER") } returns role
        every { userRepository.existsByUsername("max.mustermann") } returns true
        every { userRepository.existsByUsername("max.mustermann2") } returns false
        every { userRepository.save(any()) } returnsArgument 0
        every { invitationService.createAndSendInvitation(any()) } just runs

        // when
        val result = adminUserService.createUser(request)

        // then
        assertThat(result.username).isEqualTo("max.mustermann2")
    }

    @Test
    fun `createUser throws DuplicateEntryException when email already exists`() {
        // given
        val request =
            CreateUserRequest(
                firstName = "Max",
                lastName = "Mustermann",
                email = "max.mustermann@firma.de",
                role = "EMPLOYER",
            )

        every { userRepository.existsByEmail(request.email) } returns true

        // then
        assertThatThrownBy {
            adminUserService.createUser(request)
        }.isInstanceOf(DuplicateEntryException::class.java)
            .satisfies({ ex ->
                val e = ex as DuplicateEntryException
                assertThat(e.errorCode).isEqualTo(GlobalErrorCode.USER_ALREADY_EXISTS)
            })

        verify(exactly = 0) { userRepository.save(any()) }
    }

    @Test
    fun `createUser throws EntryNotFoundException when role does not exist`() {
        // given
        val request =
            CreateUserRequest(
                firstName = "Max",
                lastName = "Mustermann",
                email = "max.mustermann@firma.de",
                role = "INVALID_ROLE",
            )

        every { userRepository.existsByEmail(request.email) } returns false
        every { roleRepository.findByName("INVALID_ROLE") } returns null

        // then
        assertThatThrownBy {
            adminUserService.createUser(request)
        }.isInstanceOf(EntryNotFoundException::class.java)
            .satisfies({ ex ->
                val e = ex as EntryNotFoundException
                assertThat(e.errorCode).isEqualTo(GlobalErrorCode.ROLE_NOT_FOUND)
            })

        verify(exactly = 0) { userRepository.save(any()) }
    }

    @Test
    fun `normalizeForUsername handles umlauts correctly`() {
        assertThat(adminUserService.normalizeForUsername("Müller")).isEqualTo("mueller")
        assertThat(adminUserService.normalizeForUsername("Böhm")).isEqualTo("boehm")
        assertThat(adminUserService.normalizeForUsername("Lüders")).isEqualTo("lueders")
        assertThat(adminUserService.normalizeForUsername("Straße")).isEqualTo("strasse")
    }

    @Test
    fun `normalizeForUsername removes accents and special characters`() {
        assertThat(adminUserService.normalizeForUsername("René")).isEqualTo("rene")
        assertThat(adminUserService.normalizeForUsername("O'Brien")).isEqualTo("obrien")
    }

    @Test
    fun `createUser converts role to uppercase`() {
        // given
        val request =
            CreateUserRequest(
                firstName = "Max",
                lastName = "Mustermann",
                email = "max.mustermann@firma.de",
                role = "employer",
            )
        val role = RoleModel("EMPLOYER", null)

        every { userRepository.existsByEmail(request.email) } returns false
        every { roleRepository.findByName("EMPLOYER") } returns role
        every { userRepository.existsByUsername("max.mustermann") } returns false
        every { userRepository.save(any()) } returnsArgument 0
        every { invitationService.createAndSendInvitation(any()) } just runs

        // when
        val result = adminUserService.createUser(request)

        // then
        assertThat(result.role).isEqualTo("EMPLOYER")
        verify { roleRepository.findByName("EMPLOYER") }
    }
}
