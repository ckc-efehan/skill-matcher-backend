package org.efehan.skillmatcherbackend.service

import io.mockk.every
import io.mockk.impl.annotations.MockK
import io.mockk.junit5.MockKExtension
import io.mockk.slot
import io.mockk.verify
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.efehan.skillmatcherbackend.core.projectmember.AddProjectMemberRequest
import org.efehan.skillmatcherbackend.core.projectmember.ProjectMemberService
import org.efehan.skillmatcherbackend.exception.GlobalErrorCode
import org.efehan.skillmatcherbackend.persistence.ProjectMemberModel
import org.efehan.skillmatcherbackend.persistence.ProjectMemberRepository
import org.efehan.skillmatcherbackend.persistence.ProjectMemberStatus
import org.efehan.skillmatcherbackend.persistence.ProjectModel
import org.efehan.skillmatcherbackend.persistence.ProjectRepository
import org.efehan.skillmatcherbackend.persistence.ProjectStatus
import org.efehan.skillmatcherbackend.persistence.RoleModel
import org.efehan.skillmatcherbackend.persistence.UserModel
import org.efehan.skillmatcherbackend.persistence.UserRepository
import org.efehan.skillmatcherbackend.shared.exceptions.AccessDeniedException
import org.efehan.skillmatcherbackend.shared.exceptions.DuplicateEntryException
import org.efehan.skillmatcherbackend.shared.exceptions.EntryNotFoundException
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import java.time.Instant
import java.time.LocalDate
import java.util.Optional

@ExtendWith(MockKExtension::class)
@DisplayName("ProjectMemberService Unit Tests")
class ProjectMemberServiceTest {
    @MockK
    private lateinit var projectRepo: ProjectRepository

    @MockK
    private lateinit var userRepo: UserRepository

    @MockK
    private lateinit var memberRepo: ProjectMemberRepository

    private lateinit var service: ProjectMemberService

    private val role = RoleModel("PROJECTMANAGER", null)

    private val owner =
        UserModel(
            email = "owner@firma.de",
            passwordHash = "hashed",
            firstName = "Owner",
            lastName = "User",
            role = role,
        ).apply { isEnabled = true }

    private val member =
        UserModel(
            email = "member@firma.de",
            passwordHash = "hashed",
            firstName = "Member",
            lastName = "User",
            role = RoleModel("EMPLOYER", null),
        ).apply { isEnabled = true }

    private val project =
        ProjectModel(
            name = "Test Project",
            description = "A test project",
            owner = owner,
            status = ProjectStatus.PLANNED,
            startDate = LocalDate.of(2026, 3, 1),
            endDate = LocalDate.of(2026, 12, 31),
            maxMembers = 5,
        )

    @BeforeEach
    fun setUp() {
        service = ProjectMemberService(projectRepo, userRepo, memberRepo)
    }

    // ── addMember ────────────────────────────────────────────────────────

    @Test
    fun `addMember saves and returns new member`() {
        // given
        val request = AddProjectMemberRequest(userId = member.id)
        every { projectRepo.findById(project.id) } returns Optional.of(project)
        every { userRepo.findById(member.id) } returns Optional.of(member)
        every { memberRepo.findByProjectAndUser(project, member) } returns null
        every { memberRepo.countByProjectAndStatus(project, ProjectMemberStatus.ACTIVE) } returns 2
        every { memberRepo.save(any()) } returnsArgument 0

        // when
        val result = service.addMember(owner, project.id, request)

        // then
        assertThat(result.userId).isEqualTo(member.id)
        assertThat(result.status).isEqualTo("ACTIVE")
        assertThat(result.userName).isEqualTo("Member User")
        verify(exactly = 1) { memberRepo.save(any()) }
    }

    @Test
    fun `addMember throws AccessDeniedException when caller is not owner`() {
        // given
        val request = AddProjectMemberRequest(userId = member.id)
        every { projectRepo.findById(project.id) } returns Optional.of(project)

        // then
        assertThatThrownBy { service.addMember(member, project.id, request) }
            .isInstanceOf(AccessDeniedException::class.java)
            .satisfies({ ex ->
                val e = ex as AccessDeniedException
                assertThat(e.errorCode).isEqualTo(GlobalErrorCode.PROJECT_ACCESS_DENIED)
            })
    }

    @Test
    fun `addMember throws EntryNotFoundException when project not found`() {
        // given
        val request = AddProjectMemberRequest(userId = member.id)
        every { projectRepo.findById("nonexistent") } returns Optional.empty()

        // then
        assertThatThrownBy { service.addMember(owner, "nonexistent", request) }
            .isInstanceOf(EntryNotFoundException::class.java)
            .satisfies({ ex ->
                val e = ex as EntryNotFoundException
                assertThat(e.errorCode).isEqualTo(GlobalErrorCode.PROJECT_NOT_FOUND)
            })
    }

    @Test
    fun `addMember throws EntryNotFoundException when user not found`() {
        // given
        val request = AddProjectMemberRequest(userId = "nonexistent")
        every { projectRepo.findById(project.id) } returns Optional.of(project)
        every { userRepo.findById("nonexistent") } returns Optional.empty()

        // then
        assertThatThrownBy { service.addMember(owner, project.id, request) }
            .isInstanceOf(EntryNotFoundException::class.java)
            .satisfies({ ex ->
                val e = ex as EntryNotFoundException
                assertThat(e.errorCode).isEqualTo(GlobalErrorCode.USER_NOT_FOUND)
            })
    }

    @Test
    fun `addMember throws DuplicateEntryException when user is already active member`() {
        // given
        val request = AddProjectMemberRequest(userId = member.id)
        val existingMember =
            ProjectMemberModel(
                project = project,
                user = member,
                status = ProjectMemberStatus.ACTIVE,
                joinedDate = Instant.now(),
            )
        every { projectRepo.findById(project.id) } returns Optional.of(project)
        every { userRepo.findById(member.id) } returns Optional.of(member)
        every { memberRepo.findByProjectAndUser(project, member) } returns existingMember

        // then
        assertThatThrownBy { service.addMember(owner, project.id, request) }
            .isInstanceOf(DuplicateEntryException::class.java)
            .satisfies({ ex ->
                val e = ex as DuplicateEntryException
                assertThat(e.errorCode).isEqualTo(GlobalErrorCode.PROJECT_MEMBER_DUPLICATE)
            })
    }

    @Test
    fun `addMember reactivates LEFT member`() {
        // given
        val request = AddProjectMemberRequest(userId = member.id)
        val leftMember =
            ProjectMemberModel(
                project = project,
                user = member,
                status = ProjectMemberStatus.LEFT,
                joinedDate = Instant.now(),
            )
        every { projectRepo.findById(project.id) } returns Optional.of(project)
        every { userRepo.findById(member.id) } returns Optional.of(member)
        every { memberRepo.findByProjectAndUser(project, member) } returns leftMember

        val savedSlot = slot<ProjectMemberModel>()
        every { memberRepo.save(capture(savedSlot)) } returnsArgument 0

        // when
        val result = service.addMember(owner, project.id, request)

        // then
        assertThat(result.status).isEqualTo("ACTIVE")
        assertThat(savedSlot.captured.status).isEqualTo(ProjectMemberStatus.ACTIVE)
    }

    @Test
    fun `addMember throws when project is full`() {
        // given
        val fullProject =
            ProjectModel(
                name = "Full Project",
                description = "Full",
                owner = owner,
                status = ProjectStatus.PLANNED,
                startDate = LocalDate.of(2026, 3, 1),
                endDate = LocalDate.of(2026, 12, 31),
                maxMembers = 2,
            )
        val request = AddProjectMemberRequest(userId = member.id)
        every { projectRepo.findById(fullProject.id) } returns Optional.of(fullProject)
        every { userRepo.findById(member.id) } returns Optional.of(member)
        every { memberRepo.findByProjectAndUser(fullProject, member) } returns null
        every { memberRepo.countByProjectAndStatus(fullProject, ProjectMemberStatus.ACTIVE) } returns 2

        // then
        assertThatThrownBy { service.addMember(owner, fullProject.id, request) }
            .isInstanceOf(DuplicateEntryException::class.java)
            .satisfies({ ex ->
                val e = ex as DuplicateEntryException
                assertThat(e.errorCode).isEqualTo(GlobalErrorCode.PROJECT_FULL)
            })
    }

    // ── getMembers ───────────────────────────────────────────────────────

    @Test
    fun `getMembers returns only active members`() {
        // given
        val activeMember =
            ProjectMemberModel(
                project = project,
                user = member,
                status = ProjectMemberStatus.ACTIVE,
                joinedDate = Instant.now(),
            )
        val leftMember =
            ProjectMemberModel(
                project = project,
                user = owner,
                status = ProjectMemberStatus.LEFT,
                joinedDate = Instant.now(),
            )
        every { projectRepo.findById(project.id) } returns Optional.of(project)
        every { memberRepo.findByProject(project) } returns listOf(activeMember, leftMember)

        // when
        val result = service.getMembers(project.id)

        // then
        assertThat(result).hasSize(1)
        assertThat(result[0].userId).isEqualTo(member.id)
    }

    @Test
    fun `getMembers throws when project not found`() {
        // given
        every { projectRepo.findById("nonexistent") } returns Optional.empty()

        // then
        assertThatThrownBy { service.getMembers("nonexistent") }
            .isInstanceOf(EntryNotFoundException::class.java)
    }

    // ── removeMember ─────────────────────────────────────────────────────

    @Test
    fun `removeMember sets status to LEFT`() {
        // given
        val activeMember =
            ProjectMemberModel(
                project = project,
                user = member,
                status = ProjectMemberStatus.ACTIVE,
                joinedDate = Instant.now(),
            )
        every { projectRepo.findById(project.id) } returns Optional.of(project)
        every { userRepo.findById(member.id) } returns Optional.of(member)
        every { memberRepo.findByProjectAndUser(project, member) } returns activeMember

        val savedSlot = slot<ProjectMemberModel>()
        every { memberRepo.save(capture(savedSlot)) } returnsArgument 0

        // when
        service.removeMember(owner, project.id, member.id)

        // then
        assertThat(savedSlot.captured.status).isEqualTo(ProjectMemberStatus.LEFT)
    }

    @Test
    fun `removeMember throws AccessDeniedException when caller is not owner`() {
        // given
        every { projectRepo.findById(project.id) } returns Optional.of(project)

        // then
        assertThatThrownBy { service.removeMember(member, project.id, member.id) }
            .isInstanceOf(AccessDeniedException::class.java)
    }

    @Test
    fun `removeMember throws EntryNotFoundException when member not found`() {
        // given
        every { projectRepo.findById(project.id) } returns Optional.of(project)
        every { userRepo.findById(member.id) } returns Optional.of(member)
        every { memberRepo.findByProjectAndUser(project, member) } returns null

        // then
        assertThatThrownBy { service.removeMember(owner, project.id, member.id) }
            .isInstanceOf(EntryNotFoundException::class.java)
            .satisfies({ ex ->
                val e = ex as EntryNotFoundException
                assertThat(e.errorCode).isEqualTo(GlobalErrorCode.PROJECT_MEMBER_NOT_FOUND)
            })
    }

    // ── leaveProject ─────────────────────────────────────────────────────

    @Test
    fun `leaveProject sets own membership to LEFT`() {
        // given
        val activeMember =
            ProjectMemberModel(
                project = project,
                user = member,
                status = ProjectMemberStatus.ACTIVE,
                joinedDate = Instant.now(),
            )
        every { projectRepo.findById(project.id) } returns Optional.of(project)
        every { memberRepo.findByProjectAndUser(project, member) } returns activeMember

        val savedSlot = slot<ProjectMemberModel>()
        every { memberRepo.save(capture(savedSlot)) } returnsArgument 0

        // when
        service.leaveProject(member, project.id)

        // then
        assertThat(savedSlot.captured.status).isEqualTo(ProjectMemberStatus.LEFT)
    }

    @Test
    fun `leaveProject throws EntryNotFoundException when not a member`() {
        // given
        every { projectRepo.findById(project.id) } returns Optional.of(project)
        every { memberRepo.findByProjectAndUser(project, member) } returns null

        // then
        assertThatThrownBy { service.leaveProject(member, project.id) }
            .isInstanceOf(EntryNotFoundException::class.java)
    }
}
