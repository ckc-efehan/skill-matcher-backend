package org.efehan.skillmatcherbackend.integration.api

import org.assertj.core.api.Assertions.assertThat
import org.efehan.skillmatcherbackend.core.auth.JwtService
import org.efehan.skillmatcherbackend.core.projectskill.AddProjectSkillRequest
import org.efehan.skillmatcherbackend.persistence.ProjectModel
import org.efehan.skillmatcherbackend.persistence.ProjectSkillModel
import org.efehan.skillmatcherbackend.persistence.ProjectStatus
import org.efehan.skillmatcherbackend.persistence.RoleModel
import org.efehan.skillmatcherbackend.persistence.SkillModel
import org.efehan.skillmatcherbackend.persistence.UserModel
import org.efehan.skillmatcherbackend.testcontainers.AbstractIntegrationTest
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.test.web.servlet.delete
import org.springframework.test.web.servlet.get
import org.springframework.test.web.servlet.post
import java.time.LocalDate

@DisplayName("ProjectSkillController Integration Tests")
class ProjectSkillControllerIT : AbstractIntegrationTest() {
    @Autowired
    private lateinit var passwordEncoder: PasswordEncoder

    @Autowired
    private lateinit var jwtService: JwtService

    private fun createProjectManagerAndGetToken(): Pair<UserModel, String> {
        val role = roleRepository.save(RoleModel("PROJECTMANAGER", null))
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

    private fun createOtherProjectManagerAndGetToken(): Pair<UserModel, String> {
        val role = roleRepository.findByName("PROJECTMANAGER")!!
        val user =
            UserModel(
                email = "other.pm@firma.de",
                passwordHash = passwordEncoder.encode("Test-Password1!"),
                firstName = "Other",
                lastName = "PM",
                role = role,
            )
        user.isEnabled = true
        userRepository.save(user)
        return user to jwtService.generateAccessToken(user)
    }

    private fun createProject(owner: UserModel): ProjectModel =
        projectRepository.save(
            ProjectModel(
                name = "Skill Matcher",
                description = "Internal tool",
                status = ProjectStatus.PLANNED,
                startDate = LocalDate.of(2026, 3, 1),
                endDate = LocalDate.of(2026, 9, 1),
                maxMembers = 5,
                owner = owner,
            ),
        )

    @Test
    fun `should add a new skill to project and return 201`() {
        // given
        val (owner, token) = createProjectManagerAndGetToken()
        val project = createProject(owner)
        val request = AddProjectSkillRequest(name = "Kotlin", level = 4)

        // when & then
        mockMvc
            .post("/api/projects/${project.id}/skills") {
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
        val (owner, token) = createProjectManagerAndGetToken()
        val project = createProject(owner)
        val skill = skillRepository.save(SkillModel(name = "kotlin"))
        projectSkillRepository.save(ProjectSkillModel(project = project, skill = skill, level = 2))

        val request = AddProjectSkillRequest(name = "Kotlin", level = 5)

        // when & then
        mockMvc
            .post("/api/projects/${project.id}/skills") {
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
        val (owner, token) = createProjectManagerAndGetToken()
        val project = createProject(owner)
        val request = AddProjectSkillRequest(name = "Kotlin", level = 0)

        // when & then
        mockMvc
            .post("/api/projects/${project.id}/skills") {
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
        val (owner, token) = createProjectManagerAndGetToken()
        val project = createProject(owner)
        val request = AddProjectSkillRequest(name = "Kotlin", level = 6)

        // when & then
        mockMvc
            .post("/api/projects/${project.id}/skills") {
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
        val (owner, token) = createProjectManagerAndGetToken()
        val project = createProject(owner)
        val request = AddProjectSkillRequest(name = "  ", level = 3)

        // when & then
        mockMvc
            .post("/api/projects/${project.id}/skills") {
                header("Authorization", "Bearer $token")
                withBodyRequest(request)
            }.andExpect {
                status { isBadRequest() }
                jsonPath("$.errorCode") { value("VALIDATION_ERROR") }
            }
    }

    @Test
    fun `should return all skills for project`() {
        // given
        val (owner, token) = createProjectManagerAndGetToken()
        val project = createProject(owner)
        val skill1 = skillRepository.save(SkillModel(name = "kotlin"))
        val skill2 = skillRepository.save(SkillModel(name = "java"))
        projectSkillRepository.save(ProjectSkillModel(project = project, skill = skill1, level = 4))
        projectSkillRepository.save(ProjectSkillModel(project = project, skill = skill2, level = 3))

        // when & then
        mockMvc
            .get("/api/projects/${project.id}/skills") {
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
    fun `should return empty list when project has no skills`() {
        // given
        val (owner, token) = createProjectManagerAndGetToken()
        val project = createProject(owner)

        // when & then
        mockMvc
            .get("/api/projects/${project.id}/skills") {
                header("Authorization", "Bearer $token")
            }.andExpect {
                status { isOk() }
                jsonPath("$.length()") { value(0) }
            }
    }

    @Test
    fun `should not return skills of other projects`() {
        // given
        val (owner1, token1) = createProjectManagerAndGetToken()
        val (owner2, _) = createOtherProjectManagerAndGetToken()
        val project1 = createProject(owner1)
        val project2 = createProject(owner2)
        val skill = skillRepository.save(SkillModel(name = "kotlin"))
        projectSkillRepository.save(ProjectSkillModel(project = project1, skill = skill, level = 4))
        projectSkillRepository.save(ProjectSkillModel(project = project2, skill = skill, level = 2))

        // when & then
        mockMvc
            .get("/api/projects/${project1.id}/skills") {
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
        val (owner, token) = createProjectManagerAndGetToken()
        val project = createProject(owner)
        val skill = skillRepository.save(SkillModel(name = "kotlin"))
        val projectSkill = projectSkillRepository.save(ProjectSkillModel(project = project, skill = skill, level = 3))

        // when & then
        mockMvc
            .delete("/api/projects/${project.id}/skills/${projectSkill.id}") {
                header("Authorization", "Bearer $token")
            }.andExpect {
                status { isNoContent() }
            }

        assertThat(projectSkillRepository.findById(projectSkill.id)).isEmpty
    }

    @Test
    fun `should return 404 when deleting nonexistent skill`() {
        // given
        val (owner, token) = createProjectManagerAndGetToken()
        val project = createProject(owner)

        // when & then
        mockMvc
            .delete("/api/projects/${project.id}/skills/nonexistent-id") {
                header("Authorization", "Bearer $token")
            }.andExpect {
                status { isNotFound() }
                jsonPath("$.errorCode") { value("PROJECT_SKILL_NOT_FOUND") }
            }
    }

    @Test
    fun `should return 403 when other project manager tries to access skills`() {
        // given
        val (owner, _) = createProjectManagerAndGetToken()
        val project = createProject(owner)
        val (_, otherToken) = createOtherProjectManagerAndGetToken()
        val skill = skillRepository.save(SkillModel(name = "kotlin"))
        val projectSkill = projectSkillRepository.save(ProjectSkillModel(project = project, skill = skill, level = 3))

        // when & then
        mockMvc
            .get("/api/projects/${project.id}/skills") {
                header("Authorization", "Bearer $otherToken")
            }.andExpect {
                status { isForbidden() }
                jsonPath("$.errorCode") { value("PROJECT_ACCESS_DENIED") }
            }

        mockMvc
            .post("/api/projects/${project.id}/skills") {
                header("Authorization", "Bearer $otherToken")
                withBodyRequest(AddProjectSkillRequest(name = "Java", level = 3))
            }.andExpect {
                status { isForbidden() }
                jsonPath("$.errorCode") { value("PROJECT_ACCESS_DENIED") }
            }

        mockMvc
            .delete("/api/projects/${project.id}/skills/${projectSkill.id}") {
                header("Authorization", "Bearer $otherToken")
            }.andExpect {
                status { isForbidden() }
                jsonPath("$.errorCode") { value("PROJECT_ACCESS_DENIED") }
            }
    }

    @Test
    fun `should return 404 when project does not exist`() {
        // given
        val (_, token) = createProjectManagerAndGetToken()
        val request = AddProjectSkillRequest(name = "Kotlin", level = 3)

        // when & then
        mockMvc
            .post("/api/projects/nonexistent-id/skills") {
                header("Authorization", "Bearer $token")
                withBodyRequest(request)
            }.andExpect {
                status { isNotFound() }
                jsonPath("$.errorCode") { value("PROJECT_NOT_FOUND") }
            }

        mockMvc
            .get("/api/projects/nonexistent-id/skills") {
                header("Authorization", "Bearer $token")
            }.andExpect {
                status { isNotFound() }
                jsonPath("$.errorCode") { value("PROJECT_NOT_FOUND") }
            }
    }

    @Test
    fun `should return 401 when not authenticated`() {
        // given
        val (owner, _) = createProjectManagerAndGetToken()
        val project = createProject(owner)

        // when & then
        mockMvc
            .get("/api/projects/${project.id}/skills")
            .andExpect {
                status { isUnauthorized() }
            }

        mockMvc
            .post("/api/projects/${project.id}/skills") {
                withBodyRequest(AddProjectSkillRequest(name = "Kotlin", level = 3))
            }.andExpect {
                status { isUnauthorized() }
            }

        mockMvc
            .delete("/api/projects/${project.id}/skills/some-id")
            .andExpect {
                status { isUnauthorized() }
            }
    }
}
