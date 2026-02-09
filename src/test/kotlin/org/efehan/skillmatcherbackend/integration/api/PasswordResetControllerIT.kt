package org.efehan.skillmatcherbackend.integration.api
import org.assertj.core.api.Assertions.assertThat
import org.efehan.skillmatcherbackend.core.auth.JwtService
import org.efehan.skillmatcherbackend.core.auth.PasswordResetConfirmDTO
import org.efehan.skillmatcherbackend.core.auth.PasswordResetRequestDTO
import org.efehan.skillmatcherbackend.core.auth.ValidatePasswordResetTokenDTO
import org.efehan.skillmatcherbackend.persistence.PasswordResetTokenModel
import org.efehan.skillmatcherbackend.persistence.RoleModel
import org.efehan.skillmatcherbackend.persistence.UserModel
import org.efehan.skillmatcherbackend.testcontainers.AbstractIntegrationTest
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.test.web.servlet.post
import java.time.Instant
import java.time.temporal.ChronoUnit

@DisplayName("Password Reset Controller Integration Tests")
class PasswordResetControllerIT : AbstractIntegrationTest() {
    @Autowired
    private lateinit var passwordEncoder: PasswordEncoder

    @Autowired
    private lateinit var jwtService: JwtService

    private fun createEnabledUser(
        email: String = "test@example.com",
        password: String = "OldSecret-Password1!",
    ): UserModel {
        val role = roleRepository.save(RoleModel("ADMIN", null))
        val user =
            UserModel(
                username = "testuser",
                email = email,
                passwordHash = passwordEncoder.encode(password),
                firstName = "Test",
                lastName = "User",
                role = role,
            )
        user.isEnabled = true
        return userRepository.save(user)
    }

    private fun createPasswordResetToken(
        user: UserModel,
        expiresAt: Instant = Instant.now().plus(24, ChronoUnit.HOURS),
        used: Boolean = false,
        tokenValue: String = "test-reset-token",
    ): PasswordResetTokenModel =
        passwordResetTokenRepository.save(
            PasswordResetTokenModel(
                tokenHash = jwtService.hashToken(tokenValue),
                user = user,
                expiresAt = expiresAt,
                used = used,
            ),
        )

    @Nested
    @DisplayName("POST /api/auth/password-reset/request")
    inner class RequestPasswordReset {
        @Test
        fun `should return 204 for existing user`() {
            createEnabledUser()
            val request = PasswordResetRequestDTO(email = "test@example.com")
            mockMvc
                .post("/api/auth/password-reset/request") {
                    withBodyRequest(request)
                }.andExpect {
                    status { isNoContent() }
                }
            val tokens = passwordResetTokenRepository.findAll()
            assertThat(tokens).hasSize(1)
        }

        @Test
        fun `should return 204 even for non-existing user`() {
            val request = PasswordResetRequestDTO(email = "nonexistent@example.com")
            mockMvc
                .post("/api/auth/password-reset/request") {
                    withBodyRequest(request)
                }.andExpect {
                    status { isNoContent() }
                }
        }

        @Test
        fun `should return 400 for invalid email format`() {
            val request = PasswordResetRequestDTO(email = "not-an-email")
            mockMvc
                .post("/api/auth/password-reset/request") {
                    withBodyRequest(request)
                }.andExpect {
                    status { isBadRequest() }
                    jsonPath("$.errorCode") { value("VALIDATION_ERROR") }
                }
        }
    }

    @Nested
    @DisplayName("POST /api/auth/password-reset/validate")
    inner class ValidateToken {
        @Test
        fun `should return valid true for valid token`() {
            val user = createEnabledUser()
            createPasswordResetToken(user)
            val request = ValidatePasswordResetTokenDTO(token = "test-reset-token")
            mockMvc
                .post("/api/auth/password-reset/validate") {
                    withBodyRequest(request)
                }.andExpect {
                    status { isOk() }
                    jsonPath("$.valid") { value(true) }
                    jsonPath("$.email") { value("t***t@example.com") }
                }
        }

        @Test
        fun `should return valid false for non-existing token`() {
            val request = ValidatePasswordResetTokenDTO(token = "non-existent-token")
            mockMvc
                .post("/api/auth/password-reset/validate") {
                    withBodyRequest(request)
                }.andExpect {
                    status { isOk() }
                    jsonPath("$.valid") { value(false) }
                }
        }

        @Test
        fun `should return valid false for expired token`() {
            val user = createEnabledUser()
            createPasswordResetToken(user, expiresAt = Instant.now().minus(1, ChronoUnit.HOURS))
            val request = ValidatePasswordResetTokenDTO(token = "test-reset-token")
            mockMvc
                .post("/api/auth/password-reset/validate") {
                    withBodyRequest(request)
                }.andExpect {
                    status { isOk() }
                    jsonPath("$.valid") { value(false) }
                }
        }

        @Test
        fun `should return valid false for already used token`() {
            val user = createEnabledUser()
            createPasswordResetToken(user, used = true)
            val request = ValidatePasswordResetTokenDTO(token = "test-reset-token")
            mockMvc
                .post("/api/auth/password-reset/validate") {
                    withBodyRequest(request)
                }.andExpect {
                    status { isOk() }
                    jsonPath("$.valid") { value(false) }
                }
        }
    }

    @Nested
    @DisplayName("POST /api/auth/password-reset/confirm")
    inner class ConfirmPasswordReset {
        @Test
        fun `should reset password successfully with valid token`() {
            val user = createEnabledUser()
            createPasswordResetToken(user)
            val newPassword = "NewSecret-Password1!"
            val request = PasswordResetConfirmDTO(token = "test-reset-token", newPassword = newPassword)
            mockMvc
                .post("/api/auth/password-reset/confirm") {
                    withBodyRequest(request)
                }.andExpect {
                    status { isNoContent() }
                }
            val updatedUser = userRepository.findByEmail("test@example.com")!!
            assertThat(passwordEncoder.matches(newPassword, updatedUser.passwordHash)).isTrue()
        }

        @Test
        fun `should return 400 for non-existing token`() {
            val request = PasswordResetConfirmDTO(token = "non-existent-token", newPassword = "NewSecret-Password1!")
            mockMvc
                .post("/api/auth/password-reset/confirm") {
                    withBodyRequest(request)
                }.andExpect {
                    status { isBadRequest() }
                    jsonPath("$.errorCode") { value("INVALID_PASSWORD_RESET_TOKEN") }
                }
        }

        @Test
        fun `should return 400 for expired token`() {
            val user = createEnabledUser()
            createPasswordResetToken(user, expiresAt = Instant.now().minus(1, ChronoUnit.HOURS))
            val request = PasswordResetConfirmDTO(token = "test-reset-token", newPassword = "NewSecret-Password1!")
            mockMvc
                .post("/api/auth/password-reset/confirm") {
                    withBodyRequest(request)
                }.andExpect {
                    status { isBadRequest() }
                    jsonPath("$.errorCode") { value("PASSWORD_RESET_TOKEN_EXPIRED") }
                }
        }

        @Test
        fun `should return 400 for already used token`() {
            val user = createEnabledUser()
            createPasswordResetToken(user, used = true)
            val request = PasswordResetConfirmDTO(token = "test-reset-token", newPassword = "NewSecret-Password1!")
            mockMvc
                .post("/api/auth/password-reset/confirm") {
                    withBodyRequest(request)
                }.andExpect {
                    status { isBadRequest() }
                    jsonPath("$.errorCode") { value("PASSWORD_RESET_TOKEN_USED") }
                }
        }

        @Test
        fun `should return 400 for weak password`() {
            val user = createEnabledUser()
            createPasswordResetToken(user)
            val request = PasswordResetConfirmDTO(token = "test-reset-token", newPassword = "weak")
            mockMvc
                .post("/api/auth/password-reset/confirm") {
                    withBodyRequest(request)
                }.andExpect {
                    status { isBadRequest() }
                    jsonPath("$.errorCode") { value("VALIDATION_ERROR") }
                }
        }

        @Test
        fun `should allow login with new password after reset`() {
            val user = createEnabledUser(password = "OldSecret-Password1!")
            createPasswordResetToken(user)
            val newPassword = "NewSecret-Password1!"
            val resetRequest = PasswordResetConfirmDTO(token = "test-reset-token", newPassword = newPassword)
            mockMvc
                .post("/api/auth/password-reset/confirm") { withBodyRequest(resetRequest) }
                .andExpect { status { isNoContent() } }
            val loginRequest = mapOf("email" to "test@example.com", "password" to newPassword)
            mockMvc
                .post("/api/auth/login") { withBodyRequest(loginRequest) }
                .andExpect { status { isOk() } }
        }
    }
}
