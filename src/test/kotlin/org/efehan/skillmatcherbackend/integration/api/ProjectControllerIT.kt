package org.efehan.skillmatcherbackend.integration.api

import org.assertj.core.api.Assertions.assertThat
import org.efehan.skillmatcherbackend.core.auth.JwtService
import org.efehan.skillmatcherbackend.core.project.CreateProjectRequest
import org.efehan.skillmatcherbackend.core.project.UpdateProjectRequest
import org.efehan.skillmatcherbackend.persistence.ProjectModel
import org.efehan.skillmatcherbackend.persistence.ProjectStatus
import org.efehan.skillmatcherbackend.persistence.RoleModel
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

@DisplayName("ProjectController Integration Tests")
class ProjectControllerIT : AbstractIntegrationTest() {
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

    private fun createEmployerAndGetToken(): Pair<UserModel, String> {
        roleRepository.save(RoleModel("EMPLOYER", null))
        val role = roleRepository.findByName("EMPLOYER")!!
        val user =
            UserModel(
                email = "employer@firma.de",
                passwordHash = passwordEncoder.encode("Test-Password1!"),
                firstName = "Employer",
                lastName = "User",
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
    fun `should create project and return 201`() {
        // given
        val (_, token) = createProjectManagerAndGetToken()
        val request =
            CreateProjectRequest(
                name = "Skill Matcher",
                description = "Internal tool",
                startDate = LocalDate.of(2026, 3, 1),
                endDate = LocalDate.of(2026, 9, 1),
                maxMembers = 5,
            )

        // when & then
        mockMvc
            .post("/api/projects") {
                header("Authorization", "Bearer $token")
                withBodyRequest(request)
            }.andExpect {
                status { isCreated() }
                jsonPath("$.id") { isNotEmpty() }
                jsonPath("$.name") { value("Skill Matcher") }
                jsonPath("$.description") { value("Internal tool") }
                jsonPath("$.status") { value("PLANNED") }
                jsonPath("$.startDate") { value("2026-03-01") }
                jsonPath("$.endDate") { value("2026-09-01") }
                jsonPath("$.maxMembers") { value(5) }
                jsonPath("$.ownerName") { value("Max Mustermann") }
                jsonPath("$.createdDate") { isNotEmpty() }
            }
    }

    @Test
    fun `should return 400 when a field is blank`() {
        val (_, token) = createProjectManagerAndGetToken()
        val request =
            CreateProjectRequest(
                name = "",
                description = "Internal tool",
                startDate = LocalDate.of(2026, 3, 1),
                endDate = LocalDate.of(2026, 9, 1),
                maxMembers = 5,
            )

        // when & then
        mockMvc
            .post("/api/projects") {
                header("Authorization", "Bearer $token")
                withBodyRequest(request)
            }.andExpect {
                status { isBadRequest() }
                jsonPath("$.errorCode") { value("VALIDATION_ERROR") }
            }
    }

    @Test
    fun `should return 401 when not authenticated`() {
        // when & then
        mockMvc
            .post("/api/projects") {
                withBodyRequest(
                    CreateProjectRequest(
                        name = "Skill Matcher",
                        description = "Internal tool",
                        startDate = LocalDate.of(2026, 3, 1),
                        endDate = LocalDate.of(2026, 9, 1),
                        maxMembers = 5,
                    ),
                )
            }.andExpect {
                status { isUnauthorized() }
            }
    }

    @Test
    fun `should return 403 when employer tries to create project`() {
        val (_, token) = createEmployerAndGetToken()
        val request =
            CreateProjectRequest(
                name = "Skill Matcher",
                description = "Internal tool",
                startDate = LocalDate.of(2026, 3, 1),
                endDate = LocalDate.of(2026, 9, 1),
                maxMembers = 5,
            )

        // when & then
        mockMvc
            .post("/api/projects") {
                header("Authorization", "Bearer $token")
                withBodyRequest(request)
            }.andExpect {
                status { isForbidden() }
                jsonPath("$.errorCode") { value("FORBIDDEN") }
            }
    }

    @Test
    fun `should return project and 200`() {
        // given
        val (owner, token) = createProjectManagerAndGetToken()
        val project = createProject(owner)

        // when & then
        mockMvc
            .get("/api/projects/${project.id}") {
                header("Authorization", "Bearer $token")
            }.andExpect {
                status { isOk() }
                jsonPath("$.id") { value(project.id) }
                jsonPath("$.name") { value("Skill Matcher") }
                jsonPath("$.description") { value("Internal tool") }
                jsonPath("$.status") { value("PLANNED") }
                jsonPath("$.startDate") { value("2026-03-01") }
                jsonPath("$.endDate") { value("2026-09-01") }
                jsonPath("$.maxMembers") { value(5) }
                jsonPath("$.ownerName") { value("Max Mustermann") }
            }
    }

    @Test
    fun `should return 404 when project not found`() {
        // given
        val (_, token) = createProjectManagerAndGetToken()

        // when & then
        mockMvc
            .get("/api/projects/nonexistent-id") {
                header("Authorization", "Bearer $token")
            }.andExpect {
                status { isNotFound() }
                jsonPath("$.errorCode") { value("PROJECT_NOT_FOUND") }
            }
    }

    @Test
    fun `should return 401 when getting project not authenticated`() {
        // when & then
        mockMvc
            .get("/api/projects/some-id")
            .andExpect {
                status { isUnauthorized() }
            }
    }

    @Test
    fun `should return all projects and 200`() {
        // given
        val (owner, token) = createProjectManagerAndGetToken()
        createProject(owner)
        projectRepository.save(
            ProjectModel(
                name = "Another Project",
                description = "Another description",
                status = ProjectStatus.ACTIVE,
                startDate = LocalDate.of(2026, 4, 1),
                endDate = LocalDate.of(2026, 12, 1),
                maxMembers = 10,
                owner = owner,
            ),
        )

        // when & then
        mockMvc
            .get("/api/projects") {
                header("Authorization", "Bearer $token")
            }.andExpect {
                status { isOk() }
                jsonPath("$.length()") { value(2) }
                jsonPath("$[0].id") { isNotEmpty() }
                jsonPath("$[1].id") { isNotEmpty() }
            }
    }

    @Test
    fun `should return empty list when no projects exist`() {
        // given
        val (_, token) = createProjectManagerAndGetToken()

        // when & then
        mockMvc
            .get("/api/projects") {
                header("Authorization", "Bearer $token")
            }.andExpect {
                status { isOk() }
                jsonPath("$.length()") { value(0) }
            }
    }

    @Test
    fun `should return 401 when listing projects not authenticated`() {
        // when & then
        mockMvc
            .get("/api/projects")
            .andExpect {
                status { isUnauthorized() }
            }
    }

    @Test
    fun `should update project and return 200`() {
        // given
        val (owner, token) = createProjectManagerAndGetToken()
        val project = createProject(owner)
        val request =
            UpdateProjectRequest(
                name = "Updated Name",
                description = "Updated description",
                status = "ACTIVE",
                startDate = LocalDate.of(2026, 4, 1),
                endDate = LocalDate.of(2026, 12, 1),
                maxMembers = 8,
            )

        // when & then
        mockMvc
            .put("/api/projects/${project.id}") {
                header("Authorization", "Bearer $token")
                withBodyRequest(request)
            }.andExpect {
                status { isOk() }
                jsonPath("$.name") { value("Updated Name") }
                jsonPath("$.description") { value("Updated description") }
                jsonPath("$.status") { value("ACTIVE") }
                jsonPath("$.startDate") { value("2026-04-01") }
                jsonPath("$.endDate") { value("2026-12-01") }
                jsonPath("$.maxMembers") { value(8) }
            }
    }

    @Test
    fun `should return 404 when updating nonexistent project`() {
        // given
        val (_, token) = createProjectManagerAndGetToken()
        val request =
            UpdateProjectRequest(
                name = "Updated",
                description = "Updated",
                status = "ACTIVE",
                startDate = LocalDate.of(2026, 4, 1),
                endDate = LocalDate.of(2026, 12, 1),
                maxMembers = 8,
            )

        // when & then
        mockMvc
            .put("/api/projects/nonexistent-id") {
                header("Authorization", "Bearer $token")
                withBodyRequest(request)
            }.andExpect {
                status { isNotFound() }
                jsonPath("$.errorCode") { value("PROJECT_NOT_FOUND") }
            }
    }

    @Test
    fun `should return 403 when other project manager tries to update`() {
        // given
        val (owner, _) = createProjectManagerAndGetToken()
        val project = createProject(owner)
        val (_, otherToken) = createOtherProjectManagerAndGetToken()
        val request =
            UpdateProjectRequest(
                name = "Hijacked",
                description = "Hijacked",
                status = "ACTIVE",
                startDate = LocalDate.of(2026, 4, 1),
                endDate = LocalDate.of(2026, 12, 1),
                maxMembers = 8,
            )

        // when & then
        mockMvc
            .put("/api/projects/${project.id}") {
                header("Authorization", "Bearer $otherToken")
                withBodyRequest(request)
            }.andExpect {
                status { isForbidden() }
                jsonPath("$.errorCode") { value("PROJECT_ACCESS_DENIED") }
            }
    }

    @Test
    fun `should return 403 when employer tries to update project`() {
        // given
        val (owner, _) = createProjectManagerAndGetToken()
        val project = createProject(owner)
        val (_, employerToken) = createEmployerAndGetToken()
        val request =
            UpdateProjectRequest(
                name = "Hijacked",
                description = "Hijacked",
                status = "ACTIVE",
                startDate = LocalDate.of(2026, 4, 1),
                endDate = LocalDate.of(2026, 12, 1),
                maxMembers = 8,
            )

        // when & then
        mockMvc
            .put("/api/projects/${project.id}") {
                header("Authorization", "Bearer $employerToken")
                withBodyRequest(request)
            }.andExpect {
                status { isForbidden() }
            }
    }

    @Test
    fun `should return 400 when update validation fails`() {
        // given
        val (owner, token) = createProjectManagerAndGetToken()
        val project = createProject(owner)
        val request =
            UpdateProjectRequest(
                name = "",
                description = "Updated",
                status = "ACTIVE",
                startDate = LocalDate.of(2026, 4, 1),
                endDate = LocalDate.of(2026, 12, 1),
                maxMembers = 8,
            )

        // when & then
        mockMvc
            .put("/api/projects/${project.id}") {
                header("Authorization", "Bearer $token")
                withBodyRequest(request)
            }.andExpect {
                status { isBadRequest() }
                jsonPath("$.errorCode") { value("VALIDATION_ERROR") }
            }
    }

    @Test
    fun `should return 401 when updating project not authenticated`() {
        // when & then
        mockMvc
            .put("/api/projects/some-id") {
                withBodyRequest(
                    UpdateProjectRequest(
                        name = "Updated",
                        description = "Updated",
                        status = "ACTIVE",
                        startDate = LocalDate.of(2026, 4, 1),
                        endDate = LocalDate.of(2026, 12, 1),
                        maxMembers = 8,
                    ),
                )
            }.andExpect {
                status { isUnauthorized() }
            }
    }

    @Test
    fun `should delete project and return 204`() {
        // given
        val (owner, token) = createProjectManagerAndGetToken()
        val project = createProject(owner)

        // when & then
        mockMvc
            .delete("/api/projects/${project.id}") {
                header("Authorization", "Bearer $token")
            }.andExpect {
                status { isNoContent() }
            }

        assertThat(projectRepository.findById(project.id)).isEmpty
    }

    @Test
    fun `should return 404 when deleting nonexistent project`() {
        // given
        val (_, token) = createProjectManagerAndGetToken()

        // when & then
        mockMvc
            .delete("/api/projects/nonexistent-id") {
                header("Authorization", "Bearer $token")
            }.andExpect {
                status { isNotFound() }
                jsonPath("$.errorCode") { value("PROJECT_NOT_FOUND") }
            }
    }

    @Test
    fun `should return 403 when other project manager tries to delete`() {
        // given
        val (owner, _) = createProjectManagerAndGetToken()
        val project = createProject(owner)
        val (_, otherToken) = createOtherProjectManagerAndGetToken()

        // when & then
        mockMvc
            .delete("/api/projects/${project.id}") {
                header("Authorization", "Bearer $otherToken")
            }.andExpect {
                status { isForbidden() }
                jsonPath("$.errorCode") { value("PROJECT_ACCESS_DENIED") }
            }
    }

    @Test
    fun `should return 403 when employer tries to delete project`() {
        // given
        val (owner, _) = createProjectManagerAndGetToken()
        val project = createProject(owner)
        val (_, employerToken) = createEmployerAndGetToken()

        // when & then
        mockMvc
            .delete("/api/projects/${project.id}") {
                header("Authorization", "Bearer $employerToken")
            }.andExpect {
                status { isForbidden() }
            }
    }

    @Test
    fun `should return 401 when deleting project not authenticated`() {
        // when & then
        mockMvc
            .delete("/api/projects/some-id")
            .andExpect {
                status { isUnauthorized() }
            }
    }
}
