package org.efehan.skillmatcherbackend.integration.api

import org.assertj.core.api.Assertions.assertThat
import org.efehan.skillmatcherbackend.core.auth.JwtService
import org.efehan.skillmatcherbackend.core.invitation.AcceptInvitationRequest
import org.efehan.skillmatcherbackend.core.invitation.ValidateInvitationRequest
import org.efehan.skillmatcherbackend.persistence.InvitationTokenModel
import org.efehan.skillmatcherbackend.persistence.RoleModel
import org.efehan.skillmatcherbackend.persistence.UserModel
import org.efehan.skillmatcherbackend.testcontainers.AbstractIntegrationTest
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.test.web.servlet.post
import java.time.Instant
import java.time.temporal.ChronoUnit

@DisplayName("Invitation Controller Integration Tests")
class InvitationControllerIT : AbstractIntegrationTest() {
    @Autowired
    private lateinit var passwordEncoder: PasswordEncoder

    @Autowired
    private lateinit var jwtService: JwtService

    private fun createInvitedUser(
        expiresAt: Instant = Instant.now().plus(72, ChronoUnit.HOURS),
        used: Boolean = false,
        tokenValue: String = "test-invitation-token",
    ): Pair<UserModel, InvitationTokenModel> {
        val role = roleRepository.save(RoleModel("EMPLOYER", null))
        val user =
            UserModel(
                username = "max.mustermann",
                email = "max@firma.de",
                passwordHash = null,
                firstName = "Max",
                lastName = "Mustermann",
                role = role,
            )
        user.isEnabled = false
        userRepository.save(user)

        val invitation =
            invitationTokenRepository.save(
                InvitationTokenModel(
                    tokenHash = jwtService.hashToken(tokenValue),
                    user = user,
                    expiresAt = expiresAt,
                    used = used,
                ),
            )
        return user to invitation
    }

    @Test
    fun `should validate invitation with valid token`() {
        // given
        createInvitedUser()
        val request = ValidateInvitationRequest(token = "test-invitation-token")

        // when & then
        mockMvc
            .post("/api/auth/invitations/validate") {
                withBodyRequest(request)
            }.andExpect {
                status { isOk() }
                jsonPath("$.valid") { value(true) }
                jsonPath("$.email") { value("max@firma.de") }
            }
    }

    @Test
    fun `should return 400 when validating invalid token`() {
        // given
        val request = ValidateInvitationRequest(token = "non-existent-token")

        // when & then
        mockMvc
            .post("/api/auth/invitations/validate") {
                withBodyRequest(request)
            }.andExpect {
                status { isBadRequest() }
                jsonPath("$.errorCode") { value("INVALID_INVITATION_TOKEN") }
            }
    }

    @Test
    fun `should return 400 when validating expired token`() {
        // given
        createInvitedUser(expiresAt = Instant.now().minus(1, ChronoUnit.HOURS))
        val request = ValidateInvitationRequest(token = "test-invitation-token")

        // when & then
        mockMvc
            .post("/api/auth/invitations/validate") {
                withBodyRequest(request)
            }.andExpect {
                status { isBadRequest() }
                jsonPath("$.errorCode") { value("INVITATION_TOKEN_EXPIRED") }
            }
    }

    @Test
    fun `should return 400 when validating already used token`() {
        // given
        createInvitedUser(used = true)
        val request = ValidateInvitationRequest(token = "test-invitation-token")

        // when & then
        mockMvc
            .post("/api/auth/invitations/validate") {
                withBodyRequest(request)
            }.andExpect {
                status { isBadRequest() }
                jsonPath("$.errorCode") { value("INVITATION_ALREADY_ACCEPTED") }
            }
    }

    @Test
    fun `should accept invitation with valid token and return auth response`() {
        // given
        createInvitedUser()
        val request = AcceptInvitationRequest(token = "test-invitation-token", password = "NewSecret-Password1!")

        // when & then
        mockMvc
            .post("/api/auth/invitations/accept") {
                withBodyRequest(request)
            }.andExpect {
                status { isOk() }
                jsonPath("$.accessToken") { isNotEmpty() }
                jsonPath("$.refreshToken") { isNotEmpty() }
                jsonPath("$.tokenType") { value("Bearer") }
                jsonPath("$.user.email") { value("max@firma.de") }
                jsonPath("$.user.firstName") { value("Max") }
                jsonPath("$.user.role") { value("EMPLOYER") }
            }

        // verify user was activated
        val updatedUser = userRepository.findByEmail("max@firma.de")!!
        assertThat(updatedUser.isEnabled).isTrue()
        assertThat(updatedUser.passwordHash).isNotNull()
        assertThat(passwordEncoder.matches("NewSecret-Password1!", updatedUser.passwordHash)).isTrue()
    }

    @Test
    fun `should return 400 for invalid token`() {
        // given
        val request = AcceptInvitationRequest(token = "non-existent-token", password = "NewSecret-Password1!")

        // when & then
        mockMvc
            .post("/api/auth/invitations/accept") {
                withBodyRequest(request)
            }.andExpect {
                status { isBadRequest() }
                jsonPath("$.errorCode") { value("INVALID_INVITATION_TOKEN") }
            }
    }

    @Test
    fun `should return 400 for expired token`() {
        // given
        createInvitedUser(expiresAt = Instant.now().minus(1, ChronoUnit.HOURS))
        val request = AcceptInvitationRequest(token = "test-invitation-token", password = "NewSecret-Password1!")

        // when & then
        mockMvc
            .post("/api/auth/invitations/accept") {
                withBodyRequest(request)
            }.andExpect {
                status { isBadRequest() }
                jsonPath("$.errorCode") { value("INVITATION_TOKEN_EXPIRED") }
            }
    }

    @Test
    fun `should return 400 for already used token`() {
        // given
        createInvitedUser(used = true)
        val request = AcceptInvitationRequest(token = "test-invitation-token", password = "NewSecret-Password1!")

        // when & then
        mockMvc
            .post("/api/auth/invitations/accept") {
                withBodyRequest(request)
            }.andExpect {
                status { isBadRequest() }
                jsonPath("$.errorCode") { value("INVITATION_ALREADY_ACCEPTED") }
            }
    }

    @Test
    fun `should return 400 for weak password`() {
        // given
        createInvitedUser()
        val request = AcceptInvitationRequest(token = "test-invitation-token", password = "weak")

        // when & then
        mockMvc
            .post("/api/auth/invitations/accept") {
                withBodyRequest(request)
            }.andExpect {
                status { isBadRequest() }
            }

        // verify user was NOT activated
        val user = userRepository.findByEmail("max@firma.de")!!
        assertThat(user.isEnabled).isFalse()
        assertThat(user.passwordHash).isNull()
    }

    @Test
    fun `should not allow login before invitation is accepted`() {
        // given
        createInvitedUser()

        // when & then - try to login with the user that hasn't accepted yet
        mockMvc
            .post("/api/auth/login") {
                withBodyRequest(mapOf("email" to "max@firma.de", "password" to "anything"))
            }.andExpect {
                status { isForbidden() }
                jsonPath("$.errorCode") { value("ACCOUNT_DISABLED") }
            }
    }
}
