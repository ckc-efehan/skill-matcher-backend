package org.efehan.skillmatcherbackend.integration.api

import org.assertj.core.api.Assertions.assertThat
import org.efehan.skillmatcherbackend.core.auth.JwtService
import org.efehan.skillmatcherbackend.core.skill.AddSkillRequest
import org.efehan.skillmatcherbackend.persistence.RoleModel
import org.efehan.skillmatcherbackend.persistence.SkillModel
import org.efehan.skillmatcherbackend.persistence.UserModel
import org.efehan.skillmatcherbackend.persistence.UserSkillModel
import org.efehan.skillmatcherbackend.testcontainers.AbstractIntegrationTest
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.test.web.servlet.delete
import org.springframework.test.web.servlet.get
import org.springframework.test.web.servlet.post

@DisplayName("MySkillController Integration Tests")
class MySkillControllerIT : AbstractIntegrationTest() {
    @Autowired
    private lateinit var passwordEncoder: PasswordEncoder

    @Autowired
    private lateinit var jwtService: JwtService

    private fun createUserAndGetToken(): Pair<UserModel, String> {
        val role = roleRepository.save(RoleModel("EMPLOYER", null))
        val user =
            UserModel(
                email = "max@firma.de",
                passwordHash = passwordEncoder.encode("Test-Password1!"),
                firstName = "Max",
                lastName = "Mustermann",
                role = role,
            )
        user.isEnabled = true
        userRepository.save(user)
        return user to jwtService.generateAccessToken(user)
    }

    private fun createSecondUserAndGetToken(): Pair<UserModel, String> {
        val role = roleRepository.findAll().first()
        val user =
            UserModel(
                email = "other@firma.de",
                passwordHash = passwordEncoder.encode("Test-Password1!"),
                firstName = "Other",
                lastName = "User",
                role = role,
            )
        user.isEnabled = true
        userRepository.save(user)
        return user to jwtService.generateAccessToken(user)
    }

    @Test
    fun `should add a new skill and return 201`() {
        // given
        val (_, token) = createUserAndGetToken()
        val request = AddSkillRequest(name = "Kotlin", level = 4)

        // when & then
        mockMvc
            .post("/api/me/skills") {
                header("Authorization", "Bearer $token")
                withBodyRequest(request)
            }.andExpect {
                status { isCreated() }
                jsonPath("$.id") { isNotEmpty() }
                jsonPath("$.name") { value("kotlin") }
                jsonPath("$.level") { value(4) }
            }
    }

    @Test
    fun `should update existing skill and return 200`() {
        // given
        val (user, token) = createUserAndGetToken()
        val skill = skillRepository.save(SkillModel(name = "kotlin"))
        userSkillRepository.save(UserSkillModel(user = user, skill = skill, level = 2))

        val request = AddSkillRequest(name = "Kotlin", level = 5)

        // when & then
        mockMvc
            .post("/api/me/skills") {
                header("Authorization", "Bearer $token")
                withBodyRequest(request)
            }.andExpect {
                status { isOk() }
                jsonPath("$.name") { value("kotlin") }
                jsonPath("$.level") { value(5) }
            }
    }

    @Test
    fun `should return 400 when level is below 1`() {
        // given
        val (_, token) = createUserAndGetToken()
        val request = AddSkillRequest(name = "Kotlin", level = 0)

        // when & then
        mockMvc
            .post("/api/me/skills") {
                header("Authorization", "Bearer $token")
                withBodyRequest(request)
            }.andExpect {
                status { isBadRequest() }
                jsonPath("$.errorCode") { value("VALIDATION_ERROR") }
            }
    }

    @Test
    fun `should return 400 when level is above 5`() {
        // given
        val (_, token) = createUserAndGetToken()
        val request = AddSkillRequest(name = "Kotlin", level = 6)

        // when & then
        mockMvc
            .post("/api/me/skills") {
                header("Authorization", "Bearer $token")
                withBodyRequest(request)
            }.andExpect {
                status { isBadRequest() }
                jsonPath("$.errorCode") { value("VALIDATION_ERROR") }
            }
    }

    @Test
    fun `should return 400 when name is blank`() {
        // given
        val (_, token) = createUserAndGetToken()
        val request = AddSkillRequest(name = "  ", level = 3)

        // when & then
        mockMvc
            .post("/api/me/skills") {
                header("Authorization", "Bearer $token")
                withBodyRequest(request)
            }.andExpect {
                status { isBadRequest() }
                jsonPath("$.errorCode") { value("VALIDATION_ERROR") }
            }
    }

    @Test
    fun `should return all skills for authenticated user`() {
        // given
        val (user, token) = createUserAndGetToken()
        val skill1 = skillRepository.save(SkillModel(name = "kotlin"))
        val skill2 = skillRepository.save(SkillModel(name = "java"))
        userSkillRepository.save(UserSkillModel(user = user, skill = skill1, level = 4))
        userSkillRepository.save(UserSkillModel(user = user, skill = skill2, level = 3))

        // when & then
        mockMvc
            .get("/api/me/skills") {
                header("Authorization", "Bearer $token")
            }.andExpect {
                status { isOk() }
                jsonPath("$.length()") { value(2) }
                jsonPath("$[0].id") { isNotEmpty() }
                jsonPath("$[0].name") { isNotEmpty() }
                jsonPath("$[0].level") { isNotEmpty() }
            }
    }

    @Test
    fun `should return empty list when user has no skills`() {
        // given
        val (_, token) = createUserAndGetToken()

        // when & then
        mockMvc
            .get("/api/me/skills") {
                header("Authorization", "Bearer $token")
            }.andExpect {
                status { isOk() }
                jsonPath("$.length()") { value(0) }
            }
    }

    @Test
    fun `should not return skills of other users`() {
        // given
        val (user1, token1) = createUserAndGetToken()
        val (user2, _) = createSecondUserAndGetToken()
        val skill = skillRepository.save(SkillModel(name = "kotlin"))
        userSkillRepository.save(UserSkillModel(user = user1, skill = skill, level = 4))
        userSkillRepository.save(UserSkillModel(user = user2, skill = skill, level = 2))

        // when & then
        mockMvc
            .get("/api/me/skills") {
                header("Authorization", "Bearer $token1")
            }.andExpect {
                status { isOk() }
                jsonPath("$.length()") { value(1) }
                jsonPath("$[0].level") { value(4) }
            }
    }

    @Test
    fun `should delete skill and return 204`() {
        // given
        val (user, token) = createUserAndGetToken()
        val skill = skillRepository.save(SkillModel(name = "kotlin"))
        val userSkill = userSkillRepository.save(UserSkillModel(user = user, skill = skill, level = 3))

        // when & then
        mockMvc
            .delete("/api/me/skills/${userSkill.id}") {
                header("Authorization", "Bearer $token")
            }.andExpect {
                status { isNoContent() }
            }

        assertThat(userSkillRepository.findById(userSkill.id)).isEmpty
    }

    @Test
    fun `should return 404 when deleting nonexistent skill`() {
        // given
        val (_, token) = createUserAndGetToken()

        // when & then
        mockMvc
            .delete("/api/me/skills/nonexistent-id") {
                header("Authorization", "Bearer $token")
            }.andExpect {
                status { isNotFound() }
                jsonPath("$.errorCode") { value("USER_SKILL_NOT_FOUND") }
            }
    }

    @Test
    fun `should return 403 when deleting another users skill`() {
        // given
        val (_, token1) = createUserAndGetToken()
        val (user2, _) = createSecondUserAndGetToken()
        val skill = skillRepository.save(SkillModel(name = "kotlin"))
        val otherUsersSkill = userSkillRepository.save(UserSkillModel(user = user2, skill = skill, level = 3))

        // when & then
        mockMvc
            .delete("/api/me/skills/${otherUsersSkill.id}") {
                header("Authorization", "Bearer $token1")
            }.andExpect {
                status { isForbidden() }
                jsonPath("$.errorCode") { value("USER_SKILL_ACCESS_DENIED") }
            }
    }

    @Test
    fun `should return 401 when not authenticated`() {
        // when & then
        mockMvc
            .get("/api/me/skills")
            .andExpect {
                status { isUnauthorized() }
            }

        mockMvc
            .post("/api/me/skills") {
                withBodyRequest(AddSkillRequest(name = "Kotlin", level = 3))
            }.andExpect {
                status { isUnauthorized() }
            }

        mockMvc
            .delete("/api/me/skills/some-id")
            .andExpect {
                status { isUnauthorized() }
            }
    }
}
