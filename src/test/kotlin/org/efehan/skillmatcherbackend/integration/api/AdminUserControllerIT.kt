package org.efehan.skillmatcherbackend.integration.api

import org.efehan.skillmatcherbackend.core.admin.CreateUserRequest
import org.efehan.skillmatcherbackend.core.auth.JwtService
import org.efehan.skillmatcherbackend.persistence.RoleModel
import org.efehan.skillmatcherbackend.persistence.UserModel
import org.efehan.skillmatcherbackend.testcontainers.AbstractIntegrationTest
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.test.web.servlet.post

@DisplayName("Admin User Controller Integration Tests")
class AdminUserControllerIT : AbstractIntegrationTest() {
    @Autowired
    private lateinit var passwordEncoder: PasswordEncoder

    @Autowired
    private lateinit var jwtService: JwtService

    private fun createAdminAndGetToken(): String {
        val role = roleRepository.save(RoleModel("ADMIN", null))
        val admin =
            UserModel(
                username = "admin",
                email = "admin@firma.de",
                passwordHash = passwordEncoder.encode("Admin-Password1!"),
                firstName = "Admin",
                lastName = "User",
                role = role,
            )
        admin.isEnabled = true
        userRepository.save(admin)
        return jwtService.generateAccessToken(admin)
    }

    private fun createNonAdminAndGetToken(): String {
        val role = roleRepository.save(RoleModel("EMPLOYER", null))
        val user =
            UserModel(
                username = "employer",
                email = "employer@firma.de",
                passwordHash = passwordEncoder.encode("User-Password1!"),
                firstName = "Normal",
                lastName = "User",
                role = role,
            )
        user.isEnabled = true
        userRepository.save(user)
        return jwtService.generateAccessToken(user)
    }

    @Test
    fun `should create user successfully as admin`() {
        // given
        val token = createAdminAndGetToken()
        roleRepository.save(RoleModel("EMPLOYER", null))

        val request =
            CreateUserRequest(
                firstName = "Max",
                lastName = "Mustermann",
                email = "max.mustermann@firma.de",
                role = "EMPLOYER",
            )

        // when & then
        mockMvc
            .post("/api/admin/users") {
                header("Authorization", "Bearer $token")
                withBodyRequest(request)
            }.andExpect {
                status { isCreated() }
                jsonPath("$.username") { value("max.mustermann") }
                jsonPath("$.email") { value("max.mustermann@firma.de") }
                jsonPath("$.firstName") { value("Max") }
                jsonPath("$.lastName") { value("Mustermann") }
                jsonPath("$.role") { value("EMPLOYER") }
                jsonPath("$.id") { isNotEmpty() }
            }
    }

    @Test
    fun `should return 409 when email already exists`() {
        // given
        val token = createAdminAndGetToken()

        val request =
            CreateUserRequest(
                firstName = "Admin",
                lastName = "User",
                email = "admin@firma.de",
                role = "ADMIN",
            )

        // when & then
        mockMvc
            .post("/api/admin/users") {
                header("Authorization", "Bearer $token")
                withBodyRequest(request)
            }.andExpect {
                status { isConflict() }
                jsonPath("$.errorCode") { value("USER_ALREADY_EXISTS") }
            }
    }

    @Test
    fun `should return 404 when role does not exist`() {
        // given
        val token = createAdminAndGetToken()

        val request =
            CreateUserRequest(
                firstName = "Max",
                lastName = "Mustermann",
                email = "max.mustermann@firma.de",
                role = "NONEXISTENT",
            )

        // when & then
        mockMvc
            .post("/api/admin/users") {
                header("Authorization", "Bearer $token")
                withBodyRequest(request)
            }.andExpect {
                status { isNotFound() }
                jsonPath("$.errorCode") { value("ROLE_NOT_FOUND") }
            }
    }

    @Test
    fun `should return 400 when request body is invalid`() {
        // given
        val token = createAdminAndGetToken()

        val request =
            CreateUserRequest(
                firstName = "",
                lastName = "",
                email = "not-an-email",
                role = "",
            )

        // when & then
        mockMvc
            .post("/api/admin/users") {
                header("Authorization", "Bearer $token")
                withBodyRequest(request)
            }.andExpect {
                status { isBadRequest() }
                jsonPath("$.errorCode") { value("VALIDATION_ERROR") }
            }
    }

    @Test
    fun `should return 403 when non-admin tries to create user`() {
        // given
        val token = createNonAdminAndGetToken()

        val request =
            CreateUserRequest(
                firstName = "Max",
                lastName = "Mustermann",
                email = "max.mustermann@firma.de",
                role = "EMPLOYER",
            )

        // when & then
        mockMvc
            .post("/api/admin/users") {
                header("Authorization", "Bearer $token")
                withBodyRequest(request)
            }.andExpect {
                status { isForbidden() }
            }
    }

    @Test
    fun `should return 401 when not authenticated`() {
        // given
        val request =
            CreateUserRequest(
                firstName = "Max",
                lastName = "Mustermann",
                email = "max.mustermann@firma.de",
                role = "EMPLOYER",
            )

        // when & then
        mockMvc
            .post("/api/admin/users") {
                withBodyRequest(request)
            }.andExpect {
                status { isUnauthorized() }
            }
    }
}
