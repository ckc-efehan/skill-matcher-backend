package org.efehan.skillmatcherbackend.integration.api

import org.efehan.skillmatcherbackend.core.auth.JwtService
import org.efehan.skillmatcherbackend.persistence.RoleModel
import org.efehan.skillmatcherbackend.persistence.UserAvailabilityModel
import org.efehan.skillmatcherbackend.persistence.UserModel
import org.efehan.skillmatcherbackend.testcontainers.AbstractIntegrationTest
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.test.web.servlet.delete
import org.springframework.test.web.servlet.get
import org.springframework.test.web.servlet.post
import org.springframework.test.web.servlet.put
import java.time.LocalDate

@DisplayName("UserAvailabilityController Integration Tests")
class UserAvailabilityControllerIT : AbstractIntegrationTest() {
    @Autowired
    private lateinit var passwordEncoder: PasswordEncoder

    @Autowired
    private lateinit var jwtService: JwtService

    private fun createRole(name: String): RoleModel = roleRepository.save(RoleModel(name, null))

    private fun createUser(
        email: String,
        role: RoleModel,
    ): UserModel {
        val user =
            UserModel(
                email = email,
                passwordHash = passwordEncoder.encode("Test-Password1!"),
                firstName = "Test",
                lastName = "User",
                role = role,
            )
        user.isEnabled = true
        return userRepository.save(user)
    }

    @Test
    fun `should create availability and return 201`() {
        // given
        val role = createRole("EMPLOYER")
        val user = createUser("user@firma.de", role)
        val token = jwtService.generateAccessToken(user)

        // when & then
        mockMvc
            .post("/api/me/availability") {
                header("Authorization", "Bearer $token")
                withBodyRequest(
                    mapOf(
                        "availableFrom" to "2026-03-01",
                        "availableTo" to "2026-06-01",
                    ),
                )
            }.andExpect {
                status { isCreated() }
                jsonPath("$.availableFrom") { value("2026-03-01") }
                jsonPath("$.availableTo") { value("2026-06-01") }
                jsonPath("$.id") { isNotEmpty() }
            }
    }

    @Test
    fun `should return 409 when availability periods overlap`() {
        // given
        val role = createRole("EMPLOYER")
        val user = createUser("user@firma.de", role)
        val token = jwtService.generateAccessToken(user)
        userAvailabilityRepository.save(
            UserAvailabilityModel(
                user = user,
                availableFrom = LocalDate.of(2026, 3, 1),
                availableTo = LocalDate.of(2026, 6, 1),
            ),
        )

        // when & then
        mockMvc
            .post("/api/me/availability") {
                header("Authorization", "Bearer $token")
                withBodyRequest(
                    mapOf(
                        "availableFrom" to "2026-05-01",
                        "availableTo" to "2026-08-01",
                    ),
                )
            }.andExpect {
                status { isConflict() }
                jsonPath("$.errorCode") { value("USER_AVAILABILITY_OVERLAP") }
            }
    }

    @Test
    fun `should return 400 when availability date range is invalid on create`() {
        // given
        val role = createRole("EMPLOYER")
        val user = createUser("user@firma.de", role)
        val token = jwtService.generateAccessToken(user)

        // when & then
        mockMvc
            .post("/api/me/availability") {
                header("Authorization", "Bearer $token")
                withBodyRequest(
                    mapOf(
                        "availableFrom" to "2026-06-01",
                        "availableTo" to "2026-03-01",
                    ),
                )
            }.andExpect {
                status { isBadRequest() }
                jsonPath("$.errorCode") { value("VALIDATION_ERROR") }
            }
    }

    @Test
    fun `should return all availability entries sorted`() {
        // given
        val role = createRole("EMPLOYER")
        val user = createUser("user@firma.de", role)
        val token = jwtService.generateAccessToken(user)
        userAvailabilityRepository.save(
            UserAvailabilityModel(
                user = user,
                availableFrom = LocalDate.of(2026, 6, 1),
                availableTo = LocalDate.of(2026, 9, 1),
            ),
        )
        userAvailabilityRepository.save(
            UserAvailabilityModel(
                user = user,
                availableFrom = LocalDate.of(2026, 1, 1),
                availableTo = LocalDate.of(2026, 3, 1),
            ),
        )

        // when & then
        mockMvc
            .get("/api/me/availability") {
                header("Authorization", "Bearer $token")
            }.andExpect {
                status { isOk() }
                jsonPath("$.length()") { value(2) }
                jsonPath("$[0].availableFrom") { value("2026-01-01") }
                jsonPath("$[1].availableFrom") { value("2026-06-01") }
            }
    }

    @Test
    fun `should return empty list when no entries exist`() {
        // given
        val role = createRole("EMPLOYER")
        val user = createUser("user@firma.de", role)
        val token = jwtService.generateAccessToken(user)

        // when & then
        mockMvc
            .get("/api/me/availability") {
                header("Authorization", "Bearer $token")
            }.andExpect {
                status { isOk() }
                jsonPath("$.length()") { value(0) }
            }
    }

    @Test
    fun `should update availability and return 200`() {
        // given
        val role = createRole("EMPLOYER")
        val user = createUser("user@firma.de", role)
        val token = jwtService.generateAccessToken(user)
        val entry =
            userAvailabilityRepository.save(
                UserAvailabilityModel(
                    user = user,
                    availableFrom = LocalDate.of(2026, 3, 1),
                    availableTo = LocalDate.of(2026, 6, 1),
                ),
            )

        // when & then
        mockMvc
            .put("/api/me/availability/${entry.id}") {
                header("Authorization", "Bearer $token")
                withBodyRequest(
                    mapOf(
                        "availableFrom" to "2026-04-01",
                        "availableTo" to "2026-07-01",
                    ),
                )
            }.andExpect {
                status { isOk() }
                jsonPath("$.availableFrom") { value("2026-04-01") }
                jsonPath("$.availableTo") { value("2026-07-01") }
            }
    }

    @Test
    fun `should return 404 when updating nonexistent entry`() {
        // given
        val role = createRole("EMPLOYER")
        val user = createUser("user@firma.de", role)
        val token = jwtService.generateAccessToken(user)

        // when & then
        mockMvc
            .put("/api/me/availability/nonexistent-id") {
                header("Authorization", "Bearer $token")
                withBodyRequest(
                    mapOf(
                        "availableFrom" to "2026-04-01",
                        "availableTo" to "2026-07-01",
                    ),
                )
            }.andExpect {
                status { isNotFound() }
                jsonPath("$.errorCode") { value("USER_AVAILABILITY_NOT_FOUND") }
            }
    }

    @Test
    fun `should return 403 when updating other users entry`() {
        // given
        val role = createRole("EMPLOYER")
        val user = createUser("user@firma.de", role)
        val otherUser = createUser("other@firma.de", role)
        val token = jwtService.generateAccessToken(user)
        val entry =
            userAvailabilityRepository.save(
                UserAvailabilityModel(
                    user = otherUser,
                    availableFrom = LocalDate.of(2026, 3, 1),
                    availableTo = LocalDate.of(2026, 6, 1),
                ),
            )

        // when & then
        mockMvc
            .put("/api/me/availability/${entry.id}") {
                header("Authorization", "Bearer $token")
                withBodyRequest(
                    mapOf(
                        "availableFrom" to "2026-04-01",
                        "availableTo" to "2026-07-01",
                    ),
                )
            }.andExpect {
                status { isForbidden() }
                jsonPath("$.errorCode") { value("USER_AVAILABILITY_ACCESS_DENIED") }
            }
    }

    @Test
    fun `should return 400 when availability date range is invalid on update`() {
        // given
        val role = createRole("EMPLOYER")
        val user = createUser("user@firma.de", role)
        val token = jwtService.generateAccessToken(user)
        val entry =
            userAvailabilityRepository.save(
                UserAvailabilityModel(
                    user = user,
                    availableFrom = LocalDate.of(2026, 3, 1),
                    availableTo = LocalDate.of(2026, 6, 1),
                ),
            )

        // when & then
        mockMvc
            .put("/api/me/availability/${entry.id}") {
                header("Authorization", "Bearer $token")
                withBodyRequest(
                    mapOf(
                        "availableFrom" to "2026-07-01",
                        "availableTo" to "2026-04-01",
                    ),
                )
            }.andExpect {
                status { isBadRequest() }
                jsonPath("$.errorCode") { value("VALIDATION_ERROR") }
            }
    }

    @Test
    fun `should delete availability and return 204`() {
        // given
        val role = createRole("EMPLOYER")
        val user = createUser("user@firma.de", role)
        val token = jwtService.generateAccessToken(user)
        val entry =
            userAvailabilityRepository.save(
                UserAvailabilityModel(
                    user = user,
                    availableFrom = LocalDate.of(2026, 3, 1),
                    availableTo = LocalDate.of(2026, 6, 1),
                ),
            )

        // when & then
        mockMvc
            .delete("/api/me/availability/${entry.id}") {
                header("Authorization", "Bearer $token")
            }.andExpect {
                status { isNoContent() }
            }
    }

    @Test
    fun `should return 401 when not authenticated`() {
        mockMvc
            .get("/api/me/availability")
            .andExpect { status { isUnauthorized() } }

        mockMvc
            .post("/api/me/availability")
            .andExpect { status { isUnauthorized() } }
    }
}
