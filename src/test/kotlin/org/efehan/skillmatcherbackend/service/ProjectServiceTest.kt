package org.efehan.skillmatcherbackend.service

import io.mockk.every
import io.mockk.impl.annotations.MockK
import io.mockk.junit5.MockKExtension
import io.mockk.verify
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.efehan.skillmatcherbackend.core.project.CreateProjectRequest
import org.efehan.skillmatcherbackend.core.project.ProjectService
import org.efehan.skillmatcherbackend.core.project.UpdateProjectRequest
import org.efehan.skillmatcherbackend.exception.GlobalErrorCode
import org.efehan.skillmatcherbackend.persistence.ProjectModel
import org.efehan.skillmatcherbackend.persistence.ProjectRepository
import org.efehan.skillmatcherbackend.persistence.ProjectSkillModel
import org.efehan.skillmatcherbackend.persistence.ProjectSkillRepository
import org.efehan.skillmatcherbackend.persistence.ProjectStatus
import org.efehan.skillmatcherbackend.persistence.RoleModel
import org.efehan.skillmatcherbackend.persistence.SkillModel
import org.efehan.skillmatcherbackend.persistence.UserModel
import org.efehan.skillmatcherbackend.shared.exceptions.AccessDeniedException
import org.efehan.skillmatcherbackend.shared.exceptions.EntryNotFoundException
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import java.time.LocalDate
import java.util.Optional

@ExtendWith(MockKExtension::class)
@DisplayName("ProjectService Unit Tests")
class ProjectServiceTest {
    @MockK
    private lateinit var projectRepo: ProjectRepository

    @MockK
    private lateinit var projectSkillRepo: ProjectSkillRepository

    private lateinit var projectService: ProjectService

    private val role = RoleModel("PROJECTMANAGER", null)

    private val owner =
        UserModel(
            username = "max.mustermann",
            email = "max@firma.de",
            passwordHash = "hashed",
            firstName = "Max",
            lastName = "Mustermann",
            role = role,
        )

    private val otherUser =
        UserModel(
            username = "other.user",
            email = "other@firma.de",
            passwordHash = "hashed",
            firstName = "Other",
            lastName = "User",
            role = role,
        )

    @BeforeEach
    fun setUp() {
        projectService = ProjectService(projectRepo, projectSkillRepo)
    }

    @Test
    fun `createProject saves and returns project with status PLANNED`() {
        // given
        val request =
            CreateProjectRequest(
                name = "Skill Matcher",
                description = "Internal tool",
                startDate = LocalDate.of(2026, 3, 1),
                endDate = LocalDate.of(2026, 9, 1),
                maxMembers = 5,
            )
        every { projectRepo.save(any()) } returnsArgument 0

        // when
        val result = projectService.createProject(owner, request)

        // then
        assertThat(result.name).isEqualTo("Skill Matcher")
        assertThat(result.description).isEqualTo("Internal tool")
        assertThat(result.status).isEqualTo("PLANNED")
        assertThat(result.startDate).isEqualTo(LocalDate.of(2026, 3, 1))
        assertThat(result.endDate).isEqualTo(LocalDate.of(2026, 9, 1))
        assertThat(result.maxMembers).isEqualTo(5)
        assertThat(result.ownerName).isEqualTo("Max Mustermann")
        verify(exactly = 1) { projectRepo.save(any()) }
    }

    @Test
    fun `getProject returns project when found`() {
        // given
        val project = buildProject(owner)
        every { projectRepo.findById(project.id) } returns Optional.of(project)

        // when
        val result = projectService.getProject(project.id)

        // then
        assertThat(result.name).isEqualTo("Skill Matcher")
        assertThat(result.ownerName).isEqualTo("Max Mustermann")
    }

    @Test
    fun `getProject throws EntryNotFoundException when not found`() {
        // given
        every { projectRepo.findById("nonexistent") } returns Optional.empty()

        // then
        assertThatThrownBy {
            projectService.getProject("nonexistent")
        }.isInstanceOf(EntryNotFoundException::class.java)
            .satisfies({ ex ->
                val e = ex as EntryNotFoundException
                assertThat(e.errorCode).isEqualTo(GlobalErrorCode.PROJECT_NOT_FOUND)
            })
    }

    @Test
    fun `getAllProjects returns all projects`() {
        // given
        val project1 = buildProject(owner)
        val project2 = buildProject(owner, name = "Another Project")
        every { projectRepo.findAll() } returns listOf(project1, project2)

        // when
        val result = projectService.getAllProjects()

        // then
        assertThat(result).hasSize(2)
        assertThat(result[0].name).isEqualTo("Skill Matcher")
        assertThat(result[1].name).isEqualTo("Another Project")
    }

    @Test
    fun `getAllProjects returns empty list when no projects exist`() {
        // given
        every { projectRepo.findAll() } returns emptyList()

        // when
        val result = projectService.getAllProjects()

        // then
        assertThat(result).isEmpty()
    }

    @Test
    fun `updateProject updates and returns project when owner`() {
        // given
        val project = buildProject(owner)
        val request =
            UpdateProjectRequest(
                name = "Updated Name",
                description = "Updated description",
                status = "ACTIVE",
                startDate = LocalDate.of(2026, 4, 1),
                endDate = LocalDate.of(2026, 12, 1),
                maxMembers = 8,
            )
        every { projectRepo.findById(project.id) } returns Optional.of(project)
        every { projectRepo.save(any()) } returnsArgument 0

        // when
        val result = projectService.updateProject(owner, project.id, request)

        // then
        assertThat(result.name).isEqualTo("Updated Name")
        assertThat(result.description).isEqualTo("Updated description")
        assertThat(result.status).isEqualTo("ACTIVE")
        assertThat(result.startDate).isEqualTo(LocalDate.of(2026, 4, 1))
        assertThat(result.endDate).isEqualTo(LocalDate.of(2026, 12, 1))
        assertThat(result.maxMembers).isEqualTo(8)
        verify(exactly = 1) { projectRepo.save(any()) }
    }

    @Test
    fun `updateProject throws EntryNotFoundException when project not found`() {
        // given
        val request =
            UpdateProjectRequest(
                name = "Updated",
                description = "Updated",
                status = "ACTIVE",
                startDate = LocalDate.of(2026, 4, 1),
                endDate = LocalDate.of(2026, 12, 1),
                maxMembers = 8,
            )
        every { projectRepo.findById("nonexistent") } returns Optional.empty()

        // then
        assertThatThrownBy {
            projectService.updateProject(owner, "nonexistent", request)
        }.isInstanceOf(EntryNotFoundException::class.java)
            .satisfies({ ex ->
                val e = ex as EntryNotFoundException
                assertThat(e.errorCode).isEqualTo(GlobalErrorCode.PROJECT_NOT_FOUND)
            })

        verify(exactly = 0) { projectRepo.save(any()) }
    }

    @Test
    fun `updateProject throws AccessDeniedException when not owner`() {
        // given
        val project = buildProject(owner)
        val request =
            UpdateProjectRequest(
                name = "Updated",
                description = "Updated",
                status = "ACTIVE",
                startDate = LocalDate.of(2026, 4, 1),
                endDate = LocalDate.of(2026, 12, 1),
                maxMembers = 8,
            )
        every { projectRepo.findById(project.id) } returns Optional.of(project)

        // then
        assertThatThrownBy {
            projectService.updateProject(otherUser, project.id, request)
        }.isInstanceOf(AccessDeniedException::class.java)
            .satisfies({ ex ->
                val e = ex as AccessDeniedException
                assertThat(e.errorCode).isEqualTo(GlobalErrorCode.PROJECT_ACCESS_DENIED)
            })

        verify(exactly = 0) { projectRepo.save(any()) }
    }

    @Test
    fun `deleteProject deletes project and its skills when owner`() {
        // given
        val project = buildProject(owner)
        val skill = SkillModel(name = "kotlin")
        val projectSkill = ProjectSkillModel(project = project, skill = skill, level = 3)
        every { projectRepo.findById(project.id) } returns Optional.of(project)
        every { projectSkillRepo.findByProject(project) } returns listOf(projectSkill)
        every { projectSkillRepo.deleteAll(listOf(projectSkill)) } returns Unit
        every { projectRepo.delete(project) } returns Unit

        // when
        projectService.deleteProject(owner, project.id)

        // then
        verify(exactly = 1) { projectSkillRepo.deleteAll(listOf(projectSkill)) }
        verify(exactly = 1) { projectRepo.delete(project) }
    }

    @Test
    fun `deleteProject works when project has no skills`() {
        // given
        val project = buildProject(owner)
        every { projectRepo.findById(project.id) } returns Optional.of(project)
        every { projectSkillRepo.findByProject(project) } returns emptyList()
        every { projectSkillRepo.deleteAll(emptyList()) } returns Unit
        every { projectRepo.delete(project) } returns Unit

        // when
        projectService.deleteProject(owner, project.id)

        // then
        verify(exactly = 1) { projectSkillRepo.deleteAll(emptyList()) }
        verify(exactly = 1) { projectRepo.delete(project) }
    }

    @Test
    fun `deleteProject throws EntryNotFoundException when project not found`() {
        // given
        every { projectRepo.findById("nonexistent") } returns Optional.empty()

        // then
        assertThatThrownBy {
            projectService.deleteProject(owner, "nonexistent")
        }.isInstanceOf(EntryNotFoundException::class.java)
            .satisfies({ ex ->
                val e = ex as EntryNotFoundException
                assertThat(e.errorCode).isEqualTo(GlobalErrorCode.PROJECT_NOT_FOUND)
            })

        verify(exactly = 0) { projectRepo.delete(any<ProjectModel>()) }
    }

    @Test
    fun `deleteProject throws AccessDeniedException when not owner`() {
        // given
        val project = buildProject(owner)
        every { projectRepo.findById(project.id) } returns Optional.of(project)

        // then
        assertThatThrownBy {
            projectService.deleteProject(otherUser, project.id)
        }.isInstanceOf(AccessDeniedException::class.java)
            .satisfies({ ex ->
                val e = ex as AccessDeniedException
                assertThat(e.errorCode).isEqualTo(GlobalErrorCode.PROJECT_ACCESS_DENIED)
            })

        verify(exactly = 0) { projectRepo.delete(any<ProjectModel>()) }
    }

    private fun buildProject(
        projectOwner: UserModel,
        name: String = "Skill Matcher",
    ): ProjectModel =
        ProjectModel(
            name = name,
            description = "Internal tool",
            status = ProjectStatus.PLANNED,
            startDate = LocalDate.of(2026, 3, 1),
            endDate = LocalDate.of(2026, 9, 1),
            maxMembers = 5,
            owner = projectOwner,
        )
}
