package org.efehan.skillmatcherbackend.integration.api

import org.assertj.core.api.Assertions.assertThat
import org.efehan.skillmatcherbackend.core.auth.JwtService
import org.efehan.skillmatcherbackend.core.auth.LoginRequest
import org.efehan.skillmatcherbackend.core.auth.RefreshTokenRequest
import org.efehan.skillmatcherbackend.persistence.RefreshTokenModel
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

@DisplayName("Authentication Controller Integration Tests")
class AuthenticationControllerIT : AbstractIntegrationTest() {
    @Autowired
    private lateinit var passwordEncoder: PasswordEncoder

    @Autowired
    private lateinit var jwtService: JwtService

    @Test
    fun `should login successfully when valid credentials provided`() {
        // given
        val password = "Secret-Password1!"
        val role = roleRepository.save(RoleModel("ADMIN", null))
        val user =
            UserModel(
                email = "test@example.com",
                passwordHash = passwordEncoder.encode(password),
                firstName = "Test",
                lastName = "User",
                role = role,
            )
        user.isEnabled = true
        userRepository.save(user)

        val request = LoginRequest(email = "test@example.com", password = password)

        // when & then
        mockMvc
            .post("/api/auth/login") {
                withBodyRequest(request)
            }.andExpect {
                status { isOk() }
                jsonPath("$.accessToken") { isNotEmpty() }
                jsonPath("$.refreshToken") { isNotEmpty() }
                jsonPath("$.tokenType") { value("Bearer") }
                jsonPath("$.expiresIn") { isNumber() }
                jsonPath("$.user.email") { value("test@example.com") }
                jsonPath("$.user.firstName") { value("Test") }
                jsonPath("$.user.lastName") { value("User") }
                jsonPath("$.user.role") { value("ADMIN") }
            }
    }

    @Test
    fun `should return 401 when user not found`() {
        // given
        val request = LoginRequest(email = "notexisting@example.com", password = "Secret-Password1!")

        // when & then
        mockMvc
            .post("/api/auth/login") {
                withBodyRequest(request)
            }.andExpect {
                status { isUnauthorized() }
                jsonPath("$.errorCode") { value("BAD_CREDENTIALS") }
            }
    }

    @Test
    fun `should return 401 when password is wrong`() {
        // given
        val role = roleRepository.save(RoleModel("ADMIN", null))
        val user =
            UserModel(
                email = "test@example.com",
                passwordHash = passwordEncoder.encode("Secret-Password1!"),
                firstName = "Test",
                lastName = "User",
                role = role,
            )
        user.isEnabled = true
        userRepository.save(user)

        val request = LoginRequest(email = "test@example.com", password = "Wrong-Password1!")

        // when & then
        mockMvc
            .post("/api/auth/login") {
                withBodyRequest(request)
            }.andExpect {
                status { isUnauthorized() }
            }
    }

    @Test
    fun `should return 403 when account is disabled`() {
        // given
        val password = "Secret-Password1!"
        val role = roleRepository.save(RoleModel("ADMIN", null))
        val user =
            UserModel(
                email = "test@example.com",
                passwordHash = passwordEncoder.encode(password),
                firstName = "Test",
                lastName = "User",
                role = role,
            )
        user.isEnabled = false
        userRepository.save(user)

        val request = LoginRequest(email = "test@example.com", password = password)

        // when & then
        mockMvc
            .post("/api/auth/login") {
                withBodyRequest(request)
            }.andExpect {
                status { isForbidden() }
                jsonPath("$.errorCode") { value("ACCOUNT_DISABLED") }
            }
    }

    @Test
    fun `should return 400 when email is invalid`() {
        // given
        val request = LoginRequest(email = "not-an-email", password = "Secret-Password1!")

        // when & then
        mockMvc
            .post("/api/auth/login") {
                withBodyRequest(request)
            }.andExpect {
                status { isBadRequest() }
                jsonPath("$.errorCode") { value("VALIDATION_ERROR") }
            }
    }

    @Test
    fun `should return 400 when password is blank`() {
        // given
        val request = LoginRequest(email = "test@example.com", password = "")

        // when & then
        mockMvc
            .post("/api/auth/login") {
                withBodyRequest(request)
            }.andExpect {
                status { isBadRequest() }
                jsonPath("$.errorCode") { value("VALIDATION_ERROR") }
            }
    }

    private fun createUserWithRefreshToken(
        expiresAt: Instant = Instant.now().plus(7, ChronoUnit.DAYS),
        revoked: Boolean = false,
        tokenValue: String = "test-refresh-token",
    ): Pair<UserModel, RefreshTokenModel> {
        val role = roleRepository.save(RoleModel("ADMIN", null))
        val user =
            UserModel(
                email = "test@example.com",
                passwordHash = passwordEncoder.encode("Secret-Password1!"),
                firstName = "Test",
                lastName = "User",
                role = role,
            )
        user.isEnabled = true
        userRepository.save(user)

        val refreshToken =
            refreshTokenRepository.save(
                RefreshTokenModel(
                    tokenHash = jwtService.hashToken(tokenValue),
                    user = user,
                    expiresAt = expiresAt,
                    revoked = revoked,
                ),
            )
        return user to refreshToken
    }

    @Test
    fun `should refresh token successfully when valid refresh token provided`() {
        // given
        createUserWithRefreshToken()
        val request = RefreshTokenRequest(refreshToken = "test-refresh-token")

        // when & then
        mockMvc
            .post("/api/auth/refresh") {
                withBodyRequest(request)
            }.andExpect {
                status { isOk() }
                jsonPath("$.accessToken") { isNotEmpty() }
                jsonPath("$.refreshToken") { isNotEmpty() }
                jsonPath("$.tokenType") { value("Bearer") }
                jsonPath("$.expiresIn") { isNumber() }
                jsonPath("$.user.email") { value("test@example.com") }
                jsonPath("$.user.firstName") { value("Test") }
                jsonPath("$.user.lastName") { value("User") }
                jsonPath("$.user.role") { value("ADMIN") }
            }
    }

    @Test
    fun `should return same refresh token when expiry is far away`() {
        // given - expires in 7 days, above 2-day threshold
        createUserWithRefreshToken(
            expiresAt = Instant.now().plus(7, ChronoUnit.DAYS),
        )
        val request = RefreshTokenRequest(refreshToken = "test-refresh-token")

        // when & then
        mockMvc
            .post("/api/auth/refresh") {
                withBodyRequest(request)
            }.andExpect {
                status { isOk() }
                jsonPath("$.refreshToken") { value("test-refresh-token") }
            }
    }

    @Test
    fun `should rotate refresh token when expiry is below threshold`() {
        // given - expires in 1 day, below 2-day threshold
        createUserWithRefreshToken(
            expiresAt = Instant.now().plus(1, ChronoUnit.DAYS),
        )
        val request = RefreshTokenRequest(refreshToken = "test-refresh-token")

        // when & then
        mockMvc
            .post("/api/auth/refresh") {
                withBodyRequest(request)
            }.andExpect {
                status { isOk() }
                jsonPath("$.refreshToken") {
                    value(org.hamcrest.Matchers.not("test-refresh-token"))
                }
            }
    }

    @Test
    fun `should return 400 when refresh token not found`() {
        // given
        val request = RefreshTokenRequest(refreshToken = "non-existent-token")

        // when & then
        mockMvc
            .post("/api/auth/refresh") {
                withBodyRequest(request)
            }.andExpect {
                status { isBadRequest() }
                jsonPath("$.errorCode") { value("REFRESH_TOKEN_NOT_FOUND") }
            }
    }

    @Test
    fun `should return 401 when refresh token is revoked`() {
        // given
        createUserWithRefreshToken(revoked = true)
        val request = RefreshTokenRequest(refreshToken = "test-refresh-token")

        // when & then
        mockMvc
            .post("/api/auth/refresh") {
                withBodyRequest(request)
            }.andExpect {
                status { isUnauthorized() }
                jsonPath("$.errorCode") { value("INVALID_REFRESH_TOKEN") }
            }
    }

    @Test
    fun `should return 401 when refresh token is expired`() {
        // given
        createUserWithRefreshToken(
            expiresAt = Instant.now().minus(1, ChronoUnit.HOURS),
        )
        val request = RefreshTokenRequest(refreshToken = "test-refresh-token")

        // when & then
        mockMvc
            .post("/api/auth/refresh") {
                withBodyRequest(request)
            }.andExpect {
                status { isUnauthorized() }
                jsonPath("$.errorCode") { value("INVALID_REFRESH_TOKEN") }
            }
    }

    private fun createAuthenticatedUser(): Pair<UserModel, String> {
        val role = roleRepository.save(RoleModel("ADMIN", null))
        val user =
            UserModel(
                email = "test@example.com",
                passwordHash = passwordEncoder.encode("Secret-Password1!"),
                firstName = "Test",
                lastName = "User",
                role = role,
            )
        user.isEnabled = true
        userRepository.save(user)
        val accessToken = jwtService.generateAccessToken(user)
        return user to accessToken
    }

    @Test
    fun `should logout successfully and revoke all refresh tokens`() {
        // given
        val (user, accessToken) = createAuthenticatedUser()
        refreshTokenRepository.save(
            RefreshTokenModel(
                tokenHash = jwtService.hashToken("token-1"),
                user = user,
                expiresAt = Instant.now().plus(7, ChronoUnit.DAYS),
            ),
        )
        refreshTokenRepository.save(
            RefreshTokenModel(
                tokenHash = jwtService.hashToken("token-2"),
                user = user,
                expiresAt = Instant.now().plus(7, ChronoUnit.DAYS),
            ),
        )

        // when & then
        mockMvc
            .post("/api/auth/logout") {
                header("Authorization", "Bearer $accessToken")
            }.andExpect {
                status { isNoContent() }
            }

        val tokens = refreshTokenRepository.findAll()
        assertThat(tokens).allMatch { it.revoked }
    }

    @Test
    fun `should return 401 when logout without authentication`() {
        // when & then
        mockMvc
            .post("/api/auth/logout")
            .andExpect {
                status { isUnauthorized() }
            }
    }
}
