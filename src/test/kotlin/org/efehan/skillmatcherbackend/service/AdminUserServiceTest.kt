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
import org.efehan.skillmatcherbackend.persistence.RefreshTokenRepository
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
import java.util.Optional

@ExtendWith(MockKExtension::class)
@DisplayName("Admin User Service Unit Tests")
class AdminUserServiceTest {
    @MockK
    private lateinit var userRepository: UserRepository

    @MockK
    private lateinit var roleRepository: RoleRepository

    @MockK
    private lateinit var invitationService: InvitationService

    @MockK
    private lateinit var refreshTokenRepository: RefreshTokenRepository

    private lateinit var adminUserService: AdminUserService

    @BeforeEach
    fun setUp() {
        adminUserService = AdminUserService(userRepository, roleRepository, invitationService, refreshTokenRepository)
    }

    @Test
    fun `createUser successfully creates user and sends invitation`() {
        // given
        val request =
            CreateUserRequest(
                email = "max.mustermann@firma.de",
                role = "EMPLOYER",
            )
        val role = RoleModel("EMPLOYER", null)
        val userSlot = slot<UserModel>()

        every { userRepository.existsByEmail(request.email) } returns false
        every { roleRepository.findByName("EMPLOYER") } returns role
        every { userRepository.save(capture(userSlot)) } returnsArgument 0
        every { invitationService.createAndSendInvitation(any()) } just runs

        // when
        val result = adminUserService.createUser(request)

        // then
        assertThat(result.email).isEqualTo("max.mustermann@firma.de")
        assertThat(result.role).isEqualTo("EMPLOYER")

        val savedUser = userSlot.captured
        assertThat(savedUser.isEnabled).isFalse()
        assertThat(savedUser.passwordHash).isNull()
        assertThat(savedUser.firstName).isNull()
        assertThat(savedUser.lastName).isNull()

        verify(exactly = 1) { invitationService.createAndSendInvitation(any()) }
    }

    @Test
    fun `createUser throws DuplicateEntryException when email already exists`() {
        // given
        val request =
            CreateUserRequest(
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
    fun `createUser converts role to uppercase`() {
        // given
        val request =
            CreateUserRequest(
                email = "max.mustermann@firma.de",
                role = "employer",
            )
        val role = RoleModel("EMPLOYER", null)

        every { userRepository.existsByEmail(request.email) } returns false
        every { roleRepository.findByName("EMPLOYER") } returns role
        every { userRepository.save(any()) } returnsArgument 0
        every { invitationService.createAndSendInvitation(any()) } just runs

        // when
        val result = adminUserService.createUser(request)

        // then
        assertThat(result.role).isEqualTo("EMPLOYER")
        verify { roleRepository.findByName("EMPLOYER") }
    }

    @Test
    fun `updateUserStatus disables user and revokes refresh tokens`() {
        // given
        val role = RoleModel("EMPLOYER", null)
        val user =
            UserModel(
                email = "max@firma.de",
                passwordHash = "hashed",
                firstName = "Max",
                lastName = "Mustermann",
                role = role,
            )
        user.isEnabled = true

        every { userRepository.findById(user.id) } returns Optional.of(user)
        every { userRepository.save(any()) } returnsArgument 0
        every { refreshTokenRepository.revokeAllUserTokens(user.id) } returns 2

        // when
        adminUserService.updateUserStatus(user.id, false)

        // then
        assertThat(user.isEnabled).isFalse()
        verify(exactly = 1) { userRepository.save(user) }
        verify(exactly = 1) { refreshTokenRepository.revokeAllUserTokens(user.id) }
    }

    @Test
    fun `updateUserStatus enables user without revoking refresh tokens`() {
        // given
        val role = RoleModel("EMPLOYER", null)
        val user =
            UserModel(
                email = "max@firma.de",
                passwordHash = "hashed",
                firstName = "Max",
                lastName = "Mustermann",
                role = role,
            )
        user.isEnabled = false

        every { userRepository.findById(user.id) } returns Optional.of(user)
        every { userRepository.save(any()) } returnsArgument 0

        // when
        adminUserService.updateUserStatus(user.id, true)

        // then
        assertThat(user.isEnabled).isTrue()
        verify(exactly = 1) { userRepository.save(user) }
        verify(exactly = 0) { refreshTokenRepository.revokeAllUserTokens(any()) }
    }

    @Test
    fun `updateUserStatus throws EntryNotFoundException when user not found`() {
        // given
        every { userRepository.findById("nonexistent") } returns Optional.empty()

        // then
        assertThatThrownBy {
            adminUserService.updateUserStatus("nonexistent", false)
        }.isInstanceOf(EntryNotFoundException::class.java)

        verify(exactly = 0) { userRepository.save(any()) }
    }

    @Test
    fun `listUsers returns all users mapped to response`() {
        // given
        val role1 = RoleModel("EMPLOYER", null)
        val user1 =
            UserModel(
                email = "max@firma.de",
                passwordHash = "hashed",
                firstName = "Max",
                lastName = "Mustermann",
                role = role1,
            )
        user1.isEnabled = true

        val role2 = RoleModel("ADMIN", null)
        val user2 =
            UserModel(
                email = "admin@firma.de",
                passwordHash = "hashed",
                firstName = "Admin",
                lastName = "User",
                role = role2,
            )
        user2.isEnabled = false

        every { userRepository.findAll() } returns listOf(user1, user2)

        // when
        val result = adminUserService.listUsers()

        // then
        assertThat(result).hasSize(2)
        assertThat(result[0].email).isEqualTo("max@firma.de")
        assertThat(result[0].role).isEqualTo("EMPLOYER")
        assertThat(result[0].isEnabled).isTrue()
        assertThat(result[1].email).isEqualTo("admin@firma.de")
        assertThat(result[1].role).isEqualTo("ADMIN")
        assertThat(result[1].isEnabled).isFalse()
    }

    @Test
    fun `listUsers returns empty list when no users exist`() {
        // given
        every { userRepository.findAll() } returns emptyList()

        // when
        val result = adminUserService.listUsers()

        // then
        assertThat(result).isEmpty()
    }

    @Test
    fun `updateUserRole updates role and revokes refresh tokens`() {
        // given
        val oldRole = RoleModel("EMPLOYER", null)
        val user =
            UserModel(
                email = "max@firma.de",
                passwordHash = "hashed",
                firstName = "Max",
                lastName = "Mustermann",
                role = oldRole,
            )
        user.isEnabled = true
        val newRole = RoleModel("ADMIN", null)

        every { userRepository.findById(user.id) } returns Optional.of(user)
        every { roleRepository.findByName("ADMIN") } returns newRole
        every { userRepository.save(any()) } returnsArgument 0
        every { refreshTokenRepository.revokeAllUserTokens(user.id) } returns 1

        // when
        adminUserService.updateUserRole(user.id, "ADMIN")

        // then
        assertThat(user.role).isEqualTo(newRole)
        verify(exactly = 1) { userRepository.save(user) }
        verify(exactly = 1) { refreshTokenRepository.revokeAllUserTokens(user.id) }
    }

    @Test
    fun `updateUserRole converts role name to uppercase`() {
        // given
        val role = RoleModel("EMPLOYER", null)
        val user =
            UserModel(
                email = "max@firma.de",
                passwordHash = "hashed",
                firstName = "Max",
                lastName = "Mustermann",
                role = role,
            )
        user.isEnabled = true
        val newRole = RoleModel("ADMIN", null)

        every { userRepository.findById(user.id) } returns Optional.of(user)
        every { roleRepository.findByName("ADMIN") } returns newRole
        every { userRepository.save(any()) } returnsArgument 0
        every { refreshTokenRepository.revokeAllUserTokens(user.id) } returns 1

        // when
        adminUserService.updateUserRole(user.id, "admin")

        // then
        verify { roleRepository.findByName("ADMIN") }
    }

    @Test
    fun `updateUserRole throws EntryNotFoundException when user not found`() {
        // given
        every { userRepository.findById("nonexistent") } returns Optional.empty()

        // then
        assertThatThrownBy {
            adminUserService.updateUserRole("nonexistent", "ADMIN")
        }.isInstanceOf(EntryNotFoundException::class.java)

        verify(exactly = 0) { userRepository.save(any()) }
    }

    @Test
    fun `updateUserRole throws EntryNotFoundException when role not found`() {
        // given
        val role = RoleModel("EMPLOYER", null)
        val user =
            UserModel(
                email = "max@firma.de",
                passwordHash = "hashed",
                firstName = "Max",
                lastName = "Mustermann",
                role = role,
            )
        user.isEnabled = true

        every { userRepository.findById(user.id) } returns Optional.of(user)
        every { roleRepository.findByName("NONEXISTENT") } returns null

        // then
        assertThatThrownBy {
            adminUserService.updateUserRole(user.id, "NONEXISTENT")
        }.isInstanceOf(EntryNotFoundException::class.java)
            .satisfies({ ex ->
                val e = ex as EntryNotFoundException
                assertThat(e.errorCode).isEqualTo(GlobalErrorCode.ROLE_NOT_FOUND)
            })

        verify(exactly = 0) { userRepository.save(any()) }
    }
}
