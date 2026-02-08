package org.efehan.skillmatcherbackend.integration.api

import org.efehan.skillmatcherbackend.core.auth.LoginRequest
import org.efehan.skillmatcherbackend.persistence.RoleModel
import org.efehan.skillmatcherbackend.persistence.UserModel
import org.efehan.skillmatcherbackend.testcontainers.AbstractIntegrationTest
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.test.web.servlet.post

@DisplayName("Authentication Controller Integration Tests")
class AuthenticationControllerIT : AbstractIntegrationTest() {
    @Autowired
    private lateinit var passwordEncoder: PasswordEncoder

    @Test
    fun `should login successfully when valid credentials provided`() {
        // given
        val password = "Secret-Password1!"
        val role = roleRepository.save(RoleModel("ADMIN", null))
        val user =
            UserModel(
                username = "testuser",
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
                username = "testuser",
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
                username = "testuser",
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
}
