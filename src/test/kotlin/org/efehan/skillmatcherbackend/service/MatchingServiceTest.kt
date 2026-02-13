package org.efehan.skillmatcherbackend.service

import io.mockk.every
import io.mockk.impl.annotations.MockK
import io.mockk.junit5.MockKExtension
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.efehan.skillmatcherbackend.core.matching.MatchingService
import org.efehan.skillmatcherbackend.exception.GlobalErrorCode
import org.efehan.skillmatcherbackend.persistence.ProjectMemberStatus
import org.efehan.skillmatcherbackend.persistence.ProjectModel
import org.efehan.skillmatcherbackend.persistence.ProjectRepository
import org.efehan.skillmatcherbackend.persistence.ProjectSkillModel
import org.efehan.skillmatcherbackend.persistence.ProjectSkillRepository
import org.efehan.skillmatcherbackend.persistence.ProjectStatus
import org.efehan.skillmatcherbackend.persistence.RoleModel
import org.efehan.skillmatcherbackend.persistence.SkillModel
import org.efehan.skillmatcherbackend.persistence.SkillPriority
import org.efehan.skillmatcherbackend.persistence.UserAvailabilityModel
import org.efehan.skillmatcherbackend.persistence.UserAvailabilityRepository
import org.efehan.skillmatcherbackend.persistence.UserModel
import org.efehan.skillmatcherbackend.persistence.UserSkillModel
import org.efehan.skillmatcherbackend.persistence.UserSkillRepository
import org.efehan.skillmatcherbackend.shared.exceptions.EntryNotFoundException
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import java.time.LocalDate
import java.util.Optional

@ExtendWith(MockKExtension::class)
@DisplayName("MatchingService Unit Tests")
class MatchingServiceTest {
    @MockK
    private lateinit var projectRepository: ProjectRepository

    @MockK
    private lateinit var projectSkillRepository: ProjectSkillRepository

    @MockK
    private lateinit var userSkillRepository: UserSkillRepository

    @MockK
    private lateinit var availabilityRepository: UserAvailabilityRepository

    private lateinit var matchingService: MatchingService

    private val role = RoleModel("EMPLOYER", null)
    private val pmRole = RoleModel("PROJECTMANAGER", null)

    private val kotlin = SkillModel(name = "kotlin")
    private val spring = SkillModel(name = "spring boot")
    private val docker = SkillModel(name = "docker")
    private val react = SkillModel(name = "react")

    private val owner =
        UserModel(
            email = "pm@firma.de",
            passwordHash = "hashed",
            firstName = "PM",
            lastName = "User",
            role = pmRole,
        ).apply { isEnabled = true }

    private val user1 =
        UserModel(
            email = "user1@firma.de",
            passwordHash = "hashed",
            firstName = "User",
            lastName = "One",
            role = role,
        ).apply { isEnabled = true }

    private val user2 =
        UserModel(
            email = "user2@firma.de",
            passwordHash = "hashed",
            firstName = "User",
            lastName = "Two",
            role = role,
        ).apply { isEnabled = true }

    private val project =
        ProjectModel(
            name = "Test Project",
            description = "Test",
            status = ProjectStatus.PLANNED,
            startDate = LocalDate.of(2026, 3, 1),
            endDate = LocalDate.of(2026, 9, 1),
            maxMembers = 5,
            owner = owner,
        )

    @BeforeEach
    fun setUp() {
        matchingService =
            MatchingService(
                projectRepository,
                projectSkillRepository,
                userSkillRepository,
                availabilityRepository,
            )
    }

    // ── helpers ───────────────────────────────────────────────────────────

    private fun stubMatchableCandidates(
        project: ProjectModel = this.project,
        skills: List<UserSkillModel>,
    ) {
        every {
            userSkillRepository.findMatchableBySkillsForProject(any(), project, ProjectMemberStatus.ACTIVE)
        } returns skills
    }

    private fun stubMatchableProjects(
        user: UserModel = user1,
        projects: List<ProjectModel>,
    ) {
        every {
            projectRepository.findMatchableForUser(user, any(), ProjectMemberStatus.ACTIVE)
        } returns projects
    }

    // ── findCandidatesForProject ─────────────────────────────────────────

    @Test
    fun `findCandidatesForProject throws EntryNotFoundException when project not found`() {
        every { projectRepository.findById("nonexistent") } returns Optional.empty()

        assertThatThrownBy {
            matchingService.findCandidatesForProject("nonexistent", 0.0, 20)
        }.isInstanceOf(EntryNotFoundException::class.java)
            .satisfies({ ex ->
                val e = ex as EntryNotFoundException
                assertThat(e.errorCode).isEqualTo(GlobalErrorCode.PROJECT_NOT_FOUND)
            })
    }

    @Test
    fun `findCandidatesForProject returns empty list when project has no skills`() {
        every { projectRepository.findById(project.id) } returns Optional.of(project)
        every { projectSkillRepository.findByProject(project) } returns emptyList()

        val result = matchingService.findCandidatesForProject(project.id, 0.0, 20)

        assertThat(result).isEmpty()
    }

    @Test
    fun `findCandidatesForProject returns empty list when no candidates match query`() {
        val psKotlin = ProjectSkillModel(project = project, skill = kotlin, level = 3, priority = SkillPriority.MUST_HAVE)
        every { projectRepository.findById(project.id) } returns Optional.of(project)
        every { projectSkillRepository.findByProject(project) } returns listOf(psKotlin)
        stubMatchableCandidates(skills = emptyList())

        val result = matchingService.findCandidatesForProject(project.id, 0.0, 20)

        assertThat(result).isEmpty()
    }

    @Test
    fun `findCandidatesForProject returns candidates sorted by score descending`() {
        val psKotlin = ProjectSkillModel(project = project, skill = kotlin, level = 3, priority = SkillPriority.MUST_HAVE)
        val psSpring = ProjectSkillModel(project = project, skill = spring, level = 2, priority = SkillPriority.NICE_TO_HAVE)
        val us1Kotlin = UserSkillModel(user = user1, skill = kotlin, level = 4)
        val us1Spring = UserSkillModel(user = user1, skill = spring, level = 3)
        val us2Kotlin = UserSkillModel(user = user2, skill = kotlin, level = 3)

        every { projectRepository.findById(project.id) } returns Optional.of(project)
        every { projectSkillRepository.findByProject(project) } returns listOf(psKotlin, psSpring)
        stubMatchableCandidates(skills = listOf(us1Kotlin, us1Spring, us2Kotlin))
        every { availabilityRepository.findByUserIn(any()) } returns emptyList()

        val result = matchingService.findCandidatesForProject(project.id, 0.0, 20)

        assertThat(result).hasSize(2)
        assertThat(result[0].userId).isEqualTo(user1.id)
        assertThat(result[1].userId).isEqualTo(user2.id)
        assertThat(result[0].score).isGreaterThanOrEqualTo(result[1].score)
    }

    @Test
    fun `findCandidatesForProject excludes active project members via query result`() {
        val psKotlin = ProjectSkillModel(project = project, skill = kotlin, level = 3, priority = SkillPriority.MUST_HAVE)
        val us2Kotlin = UserSkillModel(user = user2, skill = kotlin, level = 4)

        every { projectRepository.findById(project.id) } returns Optional.of(project)
        every { projectSkillRepository.findByProject(project) } returns listOf(psKotlin)
        // aktive Mitglieder sind bereits in der Query ausgeschlossen
        stubMatchableCandidates(skills = listOf(us2Kotlin))
        every { availabilityRepository.findByUserIn(any()) } returns emptyList()

        val result = matchingService.findCandidatesForProject(project.id, 0.0, 20)

        assertThat(result).hasSize(1)
        assertThat(result[0].userId).isEqualTo(user2.id)
    }

    @Test
    fun `findCandidatesForProject includes left members when query returns them`() {
        val psKotlin = ProjectSkillModel(project = project, skill = kotlin, level = 3, priority = SkillPriority.MUST_HAVE)
        val us1Kotlin = UserSkillModel(user = user1, skill = kotlin, level = 4)

        every { projectRepository.findById(project.id) } returns Optional.of(project)
        every { projectSkillRepository.findByProject(project) } returns listOf(psKotlin)
        stubMatchableCandidates(skills = listOf(us1Kotlin))
        every { availabilityRepository.findByUserIn(any()) } returns emptyList()

        val result = matchingService.findCandidatesForProject(project.id, 0.0, 20)

        assertThat(result).hasSize(1)
        assertThat(result[0].userId).isEqualTo(user1.id)
    }

    @Test
    fun `findCandidatesForProject filters candidates below minScore`() {
        val psKotlin = ProjectSkillModel(project = project, skill = kotlin, level = 3, priority = SkillPriority.MUST_HAVE)
        val psSpring = ProjectSkillModel(project = project, skill = spring, level = 2, priority = SkillPriority.NICE_TO_HAVE)
        val us1Kotlin = UserSkillModel(user = user1, skill = kotlin, level = 4)
        val us1Spring = UserSkillModel(user = user1, skill = spring, level = 3)
        val us2Spring = UserSkillModel(user = user2, skill = spring, level = 1)

        every { projectRepository.findById(project.id) } returns Optional.of(project)
        every { projectSkillRepository.findByProject(project) } returns listOf(psKotlin, psSpring)
        stubMatchableCandidates(skills = listOf(us1Kotlin, us1Spring, us2Spring))
        every { availabilityRepository.findByUserIn(any()) } returns emptyList()

        val result = matchingService.findCandidatesForProject(project.id, 0.8, 20)

        assertThat(result).allSatisfy { assertThat(it.score).isGreaterThanOrEqualTo(0.8) }
        assertThat(result.map { it.userId }).doesNotContain(user2.id)
    }

    @Test
    fun `findCandidatesForProject respects limit parameter`() {
        val psKotlin = ProjectSkillModel(project = project, skill = kotlin, level = 3, priority = SkillPriority.MUST_HAVE)
        val us1Kotlin = UserSkillModel(user = user1, skill = kotlin, level = 4)
        val us2Kotlin = UserSkillModel(user = user2, skill = kotlin, level = 3)

        every { projectRepository.findById(project.id) } returns Optional.of(project)
        every { projectSkillRepository.findByProject(project) } returns listOf(psKotlin)
        stubMatchableCandidates(skills = listOf(us1Kotlin, us2Kotlin))
        every { availabilityRepository.findByUserIn(any()) } returns emptyList()

        val result = matchingService.findCandidatesForProject(project.id, 0.0, 1)

        assertThat(result).hasSize(1)
    }

    @Test
    fun `findCandidatesForProject calculates full must-have coverage when all fulfilled`() {
        val psKotlin = ProjectSkillModel(project = project, skill = kotlin, level = 3, priority = SkillPriority.MUST_HAVE)
        val psSpring = ProjectSkillModel(project = project, skill = spring, level = 2, priority = SkillPriority.MUST_HAVE)
        val us1Kotlin = UserSkillModel(user = user1, skill = kotlin, level = 3)
        val us1Spring = UserSkillModel(user = user1, skill = spring, level = 2)

        every { projectRepository.findById(project.id) } returns Optional.of(project)
        every { projectSkillRepository.findByProject(project) } returns listOf(psKotlin, psSpring)
        stubMatchableCandidates(skills = listOf(us1Kotlin, us1Spring))
        every { availabilityRepository.findByUserIn(any()) } returns emptyList()

        val result = matchingService.findCandidatesForProject(project.id, 0.0, 20)

        assertThat(result).hasSize(1)
        assertThat(result[0].breakdown.mustHaveCoverage).isEqualTo(1.0)
    }

    @Test
    fun `findCandidatesForProject calculates partial must-have coverage`() {
        val psKotlin = ProjectSkillModel(project = project, skill = kotlin, level = 3, priority = SkillPriority.MUST_HAVE)
        val psSpring = ProjectSkillModel(project = project, skill = spring, level = 4, priority = SkillPriority.MUST_HAVE)
        val us1Kotlin = UserSkillModel(user = user1, skill = kotlin, level = 3)
        val us1Spring = UserSkillModel(user = user1, skill = spring, level = 2)

        every { projectRepository.findById(project.id) } returns Optional.of(project)
        every { projectSkillRepository.findByProject(project) } returns listOf(psKotlin, psSpring)
        stubMatchableCandidates(skills = listOf(us1Kotlin, us1Spring))
        every { availabilityRepository.findByUserIn(any()) } returns emptyList()

        val result = matchingService.findCandidatesForProject(project.id, 0.0, 20)

        assertThat(result).hasSize(1)
        assertThat(result[0].breakdown.mustHaveCoverage).isEqualTo(0.5)
    }

    @Test
    fun `findCandidatesForProject reports missing skills when user lacks a skill entirely`() {
        val psKotlin = ProjectSkillModel(project = project, skill = kotlin, level = 3, priority = SkillPriority.MUST_HAVE)
        val psDocker = ProjectSkillModel(project = project, skill = docker, level = 2, priority = SkillPriority.NICE_TO_HAVE)
        val us1Kotlin = UserSkillModel(user = user1, skill = kotlin, level = 3)

        every { projectRepository.findById(project.id) } returns Optional.of(project)
        every { projectSkillRepository.findByProject(project) } returns listOf(psKotlin, psDocker)
        stubMatchableCandidates(skills = listOf(us1Kotlin))
        every { availabilityRepository.findByUserIn(any()) } returns emptyList()

        val result = matchingService.findCandidatesForProject(project.id, 0.0, 20)

        assertThat(result).hasSize(1)
        assertThat(result[0].missingSkills).hasSize(1)
        assertThat(result[0].missingSkills[0].skillName).isEqualTo("docker")
        assertThat(result[0].missingSkills[0].priority).isEqualTo("NICE_TO_HAVE")
        assertThat(result[0].matchedSkills).hasSize(1)
        assertThat(result[0].matchedSkills[0].skillName).isEqualTo("kotlin")
    }

    @Test
    fun `findCandidatesForProject lists skill as matched even when user level is below required`() {
        val psKotlin = ProjectSkillModel(project = project, skill = kotlin, level = 5, priority = SkillPriority.MUST_HAVE)
        val us1Kotlin = UserSkillModel(user = user1, skill = kotlin, level = 2)

        every { projectRepository.findById(project.id) } returns Optional.of(project)
        every { projectSkillRepository.findByProject(project) } returns listOf(psKotlin)
        stubMatchableCandidates(skills = listOf(us1Kotlin))
        every { availabilityRepository.findByUserIn(any()) } returns emptyList()

        val result = matchingService.findCandidatesForProject(project.id, 0.0, 20)

        assertThat(result).hasSize(1)
        assertThat(result[0].matchedSkills).hasSize(1)
        assertThat(result[0].matchedSkills[0].userLevel).isEqualTo(2)
        assertThat(result[0].matchedSkills[0].requiredLevel).isEqualTo(5)
        assertThat(result[0].missingSkills).isEmpty()
        assertThat(result[0].breakdown.mustHaveCoverage).isEqualTo(0.0)
    }

    @Test
    fun `findCandidatesForProject calculates nice-to-have coverage correctly`() {
        val psDocker = ProjectSkillModel(project = project, skill = docker, level = 2, priority = SkillPriority.NICE_TO_HAVE)
        val psReact = ProjectSkillModel(project = project, skill = react, level = 3, priority = SkillPriority.NICE_TO_HAVE)
        val us1Docker = UserSkillModel(user = user1, skill = docker, level = 1)

        every { projectRepository.findById(project.id) } returns Optional.of(project)
        every { projectSkillRepository.findByProject(project) } returns listOf(psDocker, psReact)
        stubMatchableCandidates(skills = listOf(us1Docker))
        every { availabilityRepository.findByUserIn(any()) } returns emptyList()

        val result = matchingService.findCandidatesForProject(project.id, 0.0, 20)

        assertThat(result).hasSize(1)
        assertThat(result[0].breakdown.niceToHaveCoverage).isEqualTo(0.5)
    }

    @Test
    fun `findCandidatesForProject calculates level fit score with overfit cap`() {
        val psKotlin = ProjectSkillModel(project = project, skill = kotlin, level = 1, priority = SkillPriority.MUST_HAVE)
        val us1Kotlin = UserSkillModel(user = user1, skill = kotlin, level = 5)

        every { projectRepository.findById(project.id) } returns Optional.of(project)
        every { projectSkillRepository.findByProject(project) } returns listOf(psKotlin)
        stubMatchableCandidates(skills = listOf(us1Kotlin))
        every { availabilityRepository.findByUserIn(any()) } returns emptyList()

        val result = matchingService.findCandidatesForProject(project.id, 0.0, 20)

        assertThat(result).hasSize(1)
        assertThat(result[0].breakdown.levelFitScore).isEqualTo(1.0)
    }

    @Test
    fun `findCandidatesForProject calculates availability score for partial coverage`() {
        val psKotlin = ProjectSkillModel(project = project, skill = kotlin, level = 1, priority = SkillPriority.MUST_HAVE)
        val us1Kotlin = UserSkillModel(user = user1, skill = kotlin, level = 3)
        val availability =
            UserAvailabilityModel(
                user = user1,
                availableFrom = LocalDate.of(2026, 3, 1),
                availableTo = LocalDate.of(2026, 6, 1),
            )

        every { projectRepository.findById(project.id) } returns Optional.of(project)
        every { projectSkillRepository.findByProject(project) } returns listOf(psKotlin)
        stubMatchableCandidates(skills = listOf(us1Kotlin))
        every { availabilityRepository.findByUserIn(any()) } returns listOf(availability)

        val result = matchingService.findCandidatesForProject(project.id, 0.0, 20)

        assertThat(result).hasSize(1)
        assertThat(result[0].breakdown.availabilityScore).isGreaterThan(0.0)
        assertThat(result[0].breakdown.availabilityScore).isLessThan(1.0)
    }

    @Test
    fun `findCandidatesForProject returns availability 1 when user has no availability entries`() {
        val psKotlin = ProjectSkillModel(project = project, skill = kotlin, level = 1, priority = SkillPriority.MUST_HAVE)
        val us1Kotlin = UserSkillModel(user = user1, skill = kotlin, level = 3)

        every { projectRepository.findById(project.id) } returns Optional.of(project)
        every { projectSkillRepository.findByProject(project) } returns listOf(psKotlin)
        stubMatchableCandidates(skills = listOf(us1Kotlin))
        every { availabilityRepository.findByUserIn(any()) } returns emptyList()

        val result = matchingService.findCandidatesForProject(project.id, 0.0, 20)

        assertThat(result).hasSize(1)
        assertThat(result[0].breakdown.availabilityScore).isEqualTo(1.0)
    }

    // ── findProjectsForUser ──────────────────────────────────────────────

    @Test
    fun `findProjectsForUser returns empty list when user has no skills`() {
        every { userSkillRepository.findByUser(user1) } returns emptyList()

        val result = matchingService.findProjectsForUser(user1, 0.0, 20)

        assertThat(result).isEmpty()
    }

    @Test
    fun `findProjectsForUser excludes active memberships via query result`() {
        val usKotlin = UserSkillModel(user = user1, skill = kotlin, level = 4)
        every { userSkillRepository.findByUser(user1) } returns listOf(usKotlin)
        stubMatchableProjects(projects = emptyList())

        val result = matchingService.findProjectsForUser(user1, 0.0, 20)

        assertThat(result).isEmpty()
    }

    @Test
    fun `findProjectsForUser returns matching projects sorted by score`() {
        val project2 =
            ProjectModel(
                name = "Other Project",
                description = "Other",
                status = ProjectStatus.PLANNED,
                startDate = LocalDate.of(2026, 3, 1),
                endDate = LocalDate.of(2026, 9, 1),
                maxMembers = 3,
                owner = owner,
            )
        val usKotlin = UserSkillModel(user = user1, skill = kotlin, level = 4)
        val usSpring = UserSkillModel(user = user1, skill = spring, level = 3)
        val ps1Kotlin = ProjectSkillModel(project = project, skill = kotlin, level = 3, priority = SkillPriority.MUST_HAVE)
        val ps1Spring = ProjectSkillModel(project = project, skill = spring, level = 2, priority = SkillPriority.MUST_HAVE)
        val ps2Docker = ProjectSkillModel(project = project2, skill = docker, level = 3, priority = SkillPriority.MUST_HAVE)

        every { userSkillRepository.findByUser(user1) } returns listOf(usKotlin, usSpring)
        stubMatchableProjects(projects = listOf(project, project2))
        every { projectSkillRepository.findByProjectIn(any()) } returns listOf(ps1Kotlin, ps1Spring, ps2Docker)
        every { availabilityRepository.findByUser(user1) } returns emptyList()

        val result = matchingService.findProjectsForUser(user1, 0.0, 20)

        assertThat(result).hasSize(2)
        assertThat(result[0].score).isGreaterThanOrEqualTo(result[1].score)
        assertThat(result[0].projectName).isEqualTo("Test Project")
    }

    @Test
    fun `findProjectsForUser skips projects with no skills`() {
        val usKotlin = UserSkillModel(user = user1, skill = kotlin, level = 3)
        every { userSkillRepository.findByUser(user1) } returns listOf(usKotlin)
        stubMatchableProjects(projects = listOf(project))
        every { projectSkillRepository.findByProjectIn(any()) } returns emptyList()
        every { availabilityRepository.findByUser(user1) } returns emptyList()

        val result = matchingService.findProjectsForUser(user1, 0.0, 20)

        assertThat(result).isEmpty()
    }

    @Test
    fun `findProjectsForUser filters projects below minScore`() {
        val usDocker = UserSkillModel(user = user1, skill = docker, level = 2)
        val psKotlin = ProjectSkillModel(project = project, skill = kotlin, level = 4, priority = SkillPriority.MUST_HAVE)
        every { userSkillRepository.findByUser(user1) } returns listOf(usDocker)
        stubMatchableProjects(projects = listOf(project))
        every { projectSkillRepository.findByProjectIn(any()) } returns listOf(psKotlin)
        every { availabilityRepository.findByUser(user1) } returns emptyList()

        val result = matchingService.findProjectsForUser(user1, 0.9, 20)

        assertThat(result).isEmpty()
    }

    @Test
    fun `findProjectsForUser respects limit parameter`() {
        val project2 =
            ProjectModel(
                name = "Project 2",
                description = "Desc",
                status = ProjectStatus.ACTIVE,
                startDate = LocalDate.of(2026, 3, 1),
                endDate = LocalDate.of(2026, 9, 1),
                maxMembers = 3,
                owner = owner,
            )
        val usKotlin = UserSkillModel(user = user1, skill = kotlin, level = 4)
        val ps1Kotlin = ProjectSkillModel(project = project, skill = kotlin, level = 2, priority = SkillPriority.MUST_HAVE)
        val ps2Kotlin = ProjectSkillModel(project = project2, skill = kotlin, level = 2, priority = SkillPriority.MUST_HAVE)

        every { userSkillRepository.findByUser(user1) } returns listOf(usKotlin)
        stubMatchableProjects(projects = listOf(project, project2))
        every { projectSkillRepository.findByProjectIn(any()) } returns listOf(ps1Kotlin, ps2Kotlin)
        every { availabilityRepository.findByUser(user1) } returns emptyList()

        val result = matchingService.findProjectsForUser(user1, 0.0, 1)

        assertThat(result).hasSize(1)
    }

    @Test
    fun `findProjectsForUser returns correct project metadata in result`() {
        val usKotlin = UserSkillModel(user = user1, skill = kotlin, level = 3)
        val psKotlin = ProjectSkillModel(project = project, skill = kotlin, level = 2, priority = SkillPriority.MUST_HAVE)
        every { userSkillRepository.findByUser(user1) } returns listOf(usKotlin)
        stubMatchableProjects(projects = listOf(project))
        every { projectSkillRepository.findByProjectIn(any()) } returns listOf(psKotlin)
        every { availabilityRepository.findByUser(user1) } returns emptyList()

        val result = matchingService.findProjectsForUser(user1, 0.0, 20)

        assertThat(result).hasSize(1)
        assertThat(result[0].projectId).isEqualTo(project.id)
        assertThat(result[0].projectName).isEqualTo("Test Project")
        assertThat(result[0].projectDescription).isEqualTo("Test")
        assertThat(result[0].status).isEqualTo("PLANNED")
        assertThat(result[0].ownerName).isEqualTo("PM User")
    }
}
