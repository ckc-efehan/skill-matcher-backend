package org.efehan.skillmatcherbackend.integration.api

import org.efehan.skillmatcherbackend.core.auth.JwtService
import org.efehan.skillmatcherbackend.persistence.ProjectMemberModel
import org.efehan.skillmatcherbackend.persistence.ProjectMemberStatus
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
import java.time.Instant
import java.time.LocalDate

@DisplayName("ProjectMemberController Integration Tests")
class ProjectMemberControllerIT : AbstractIntegrationTest() {
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

    private fun createProject(
        owner: UserModel,
        maxMembers: Int = 5,
    ): ProjectModel =
        projectRepository.save(
            ProjectModel(
                name = "Test Project",
                description = "A test project",
                owner = owner,
                status = ProjectStatus.PLANNED,
                startDate = LocalDate.of(2026, 3, 1),
                endDate = LocalDate.of(2026, 12, 31),
                maxMembers = maxMembers,
            ),
        )

    @Test
    fun `should add member and return 201`() {
        // given
        val role = createRole("PROJECTMANAGER")
        val owner = createUser("owner@firma.de", role)
        val member = createUser("member@firma.de", role)
        val project = createProject(owner)
        val token = jwtService.generateAccessToken(owner)

        // when & then
        mockMvc
            .post("/api/projects/${project.id}/members") {
                header("Authorization", "Bearer $token")
                withBodyRequest(mapOf("userId" to member.id))
            }.andExpect {
                status { isCreated() }
                jsonPath("$.userId") { value(member.id) }
                jsonPath("$.status") { value("ACTIVE") }
                jsonPath("$.userName") { value("Test User") }
                jsonPath("$.email") { value("member@firma.de") }
            }
    }

    @Test
    fun `should return 409 when member already active`() {
        // given
        val role = createRole("PROJECTMANAGER")
        val owner = createUser("owner@firma.de", role)
        val member = createUser("member@firma.de", role)
        val project = createProject(owner)
        val token = jwtService.generateAccessToken(owner)
        projectMemberRepository.save(
            ProjectMemberModel(
                project = project,
                user = member,
                status = ProjectMemberStatus.ACTIVE,
                joinedDate = Instant.now(),
            ),
        )

        // when & then
        mockMvc
            .post("/api/projects/${project.id}/members") {
                header("Authorization", "Bearer $token")
                withBodyRequest(mapOf("userId" to member.id))
            }.andExpect {
                status { isConflict() }
                jsonPath("$.errorCode") { value("PROJECT_MEMBER_DUPLICATE") }
            }
    }

    @Test
    fun `should reactivate LEFT member`() {
        // given
        val role = createRole("PROJECTMANAGER")
        val owner = createUser("owner@firma.de", role)
        val member = createUser("member@firma.de", role)
        val project = createProject(owner)
        val token = jwtService.generateAccessToken(owner)
        projectMemberRepository.save(
            ProjectMemberModel(
                project = project,
                user = member,
                status = ProjectMemberStatus.LEFT,
                joinedDate = Instant.now(),
            ),
        )

        // when & then
        mockMvc
            .post("/api/projects/${project.id}/members") {
                header("Authorization", "Bearer $token")
                withBodyRequest(mapOf("userId" to member.id))
            }.andExpect {
                status { isCreated() }
                jsonPath("$.status") { value("ACTIVE") }
            }
    }

    @Test
    fun `should return 409 when project is full`() {
        // given
        val role = createRole("PROJECTMANAGER")
        val owner = createUser("owner@firma.de", role)
        val member1 = createUser("m1@firma.de", role)
        val member2 = createUser("m2@firma.de", role)
        val newMember = createUser("new@firma.de", role)
        val project = createProject(owner, maxMembers = 2)
        val token = jwtService.generateAccessToken(owner)

        projectMemberRepository.save(
            ProjectMemberModel(project = project, user = member1, status = ProjectMemberStatus.ACTIVE, joinedDate = Instant.now()),
        )
        projectMemberRepository.save(
            ProjectMemberModel(project = project, user = member2, status = ProjectMemberStatus.ACTIVE, joinedDate = Instant.now()),
        )

        // when & then
        mockMvc
            .post("/api/projects/${project.id}/members") {
                header("Authorization", "Bearer $token")
                withBodyRequest(mapOf("userId" to newMember.id))
            }.andExpect {
                status { isConflict() }
                jsonPath("$.errorCode") { value("PROJECT_FULL") }
            }
    }

    @Test
    fun `should return 403 when caller is not project owner`() {
        // given
        val role = createRole("PROJECTMANAGER")
        val owner = createUser("owner@firma.de", role)
        val nonOwner = createUser("other@firma.de", role)
        val project = createProject(owner)
        val token = jwtService.generateAccessToken(nonOwner)

        // when & then
        mockMvc
            .post("/api/projects/${project.id}/members") {
                header("Authorization", "Bearer $token")
                withBodyRequest(mapOf("userId" to nonOwner.id))
            }.andExpect {
                status { isForbidden() }
                jsonPath("$.errorCode") { value("PROJECT_ACCESS_DENIED") }
            }
    }

    @Test
    fun `should list only active members`() {
        // given
        val role = createRole("PROJECTMANAGER")
        val owner = createUser("owner@firma.de", role)
        val active = createUser("active@firma.de", role)
        val left = createUser("left@firma.de", role)
        val project = createProject(owner)
        val token = jwtService.generateAccessToken(owner)

        projectMemberRepository.save(
            ProjectMemberModel(project = project, user = active, status = ProjectMemberStatus.ACTIVE, joinedDate = Instant.now()),
        )
        projectMemberRepository.save(
            ProjectMemberModel(project = project, user = left, status = ProjectMemberStatus.LEFT, joinedDate = Instant.now()),
        )

        // when & then
        mockMvc
            .get("/api/projects/${project.id}/members") {
                header("Authorization", "Bearer $token")
            }.andExpect {
                status { isOk() }
                jsonPath("$.length()") { value(1) }
                jsonPath("$[0].email") { value("active@firma.de") }
            }
    }

    @Test
    fun `should remove member and return 204`() {
        // given
        val role = createRole("PROJECTMANAGER")
        val owner = createUser("owner@firma.de", role)
        val member = createUser("member@firma.de", role)
        val project = createProject(owner)
        val token = jwtService.generateAccessToken(owner)

        projectMemberRepository.save(
            ProjectMemberModel(project = project, user = member, status = ProjectMemberStatus.ACTIVE, joinedDate = Instant.now()),
        )

        // when & then
        mockMvc
            .delete("/api/projects/${project.id}/members/${member.id}") {
                header("Authorization", "Bearer $token")
            }.andExpect {
                status { isNoContent() }
            }
    }

    @Test
    fun `should return 404 when removing non-member`() {
        // given
        val role = createRole("PROJECTMANAGER")
        val owner = createUser("owner@firma.de", role)
        val nonMember = createUser("non@firma.de", role)
        val project = createProject(owner)
        val token = jwtService.generateAccessToken(owner)

        // when & then
        mockMvc
            .delete("/api/projects/${project.id}/members/${nonMember.id}") {
                header("Authorization", "Bearer $token")
            }.andExpect {
                status { isNotFound() }
                jsonPath("$.errorCode") { value("PROJECT_MEMBER_NOT_FOUND") }
            }
    }

    @Test
    fun `should allow member to leave project`() {
        // given
        val role = createRole("PROJECTMANAGER")
        val owner = createUser("owner@firma.de", role)
        val member = createUser("member@firma.de", role)
        val project = createProject(owner)
        val memberToken = jwtService.generateAccessToken(member)

        projectMemberRepository.save(
            ProjectMemberModel(project = project, user = member, status = ProjectMemberStatus.ACTIVE, joinedDate = Instant.now()),
        )

        // when & then
        mockMvc
            .post("/api/projects/${project.id}/members/leave") {
                header("Authorization", "Bearer $memberToken")
            }.andExpect {
                status { isNoContent() }
            }
    }

    @Test
    fun `should return 404 when leaving project user is not member of`() {
        // given
        val role = createRole("PROJECTMANAGER")
        val owner = createUser("owner@firma.de", role)
        val nonMember = createUser("non@firma.de", role)
        val project = createProject(owner)
        val token = jwtService.generateAccessToken(nonMember)

        // when & then
        mockMvc
            .post("/api/projects/${project.id}/members/leave") {
                header("Authorization", "Bearer $token")
            }.andExpect {
                status { isNotFound() }
                jsonPath("$.errorCode") { value("PROJECT_MEMBER_NOT_FOUND") }
            }
    }

    @Test
    fun `should return 401 when not authenticated`() {
        mockMvc
            .get("/api/projects/some-id/members")
            .andExpect { status { isUnauthorized() } }

        mockMvc
            .post("/api/projects/some-id/members")
            .andExpect { status { isUnauthorized() } }
    }
}
