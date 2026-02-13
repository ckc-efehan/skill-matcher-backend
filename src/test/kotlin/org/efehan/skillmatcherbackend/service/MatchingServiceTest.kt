package org.efehan.skillmatcherbackend.service

import io.mockk.every
import io.mockk.impl.annotations.MockK
import io.mockk.junit5.MockKExtension
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.efehan.skillmatcherbackend.core.matching.MatchingService
import org.efehan.skillmatcherbackend.exception.GlobalErrorCode
import org.efehan.skillmatcherbackend.persistence.ProjectMemberModel
import org.efehan.skillmatcherbackend.persistence.ProjectMemberRepository
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
import org.efehan.skillmatcherbackend.persistence.UserAvailibilityRepository
import org.efehan.skillmatcherbackend.persistence.UserModel
import org.efehan.skillmatcherbackend.persistence.UserSkillModel
import org.efehan.skillmatcherbackend.persistence.UserSkillRepository
import org.efehan.skillmatcherbackend.shared.exceptions.EntryNotFoundException
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import java.time.Instant
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
    private lateinit var projectMemberRepository: ProjectMemberRepository

    @MockK
    private lateinit var availabilityRepository: UserAvailibilityRepository

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
                projectSkillRepository,
                projectMemberRepository,
                availabilityRepository,
            )
    }

    // ── findCandidatesForProject ─────────────────────────────────────────

    @Test
    fun `findCandidatesForProject throws EntryNotFoundException when project not found`() {
        // given
        every { projectRepository.findById("nonexistent") } returns Optional.empty()

        // then
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
        // given
        every { projectRepository.findById(project.id) } returns Optional.of(project)
        every { projectSkillRepository.findByProject(project) } returns emptyList()

        // when
        val result = matchingService.findCandidatesForProject(project.id, 0.0, 20)

        // then
        assertThat(result).isEmpty()
    }

    @Test
    fun `findCandidatesForProject returns empty list when no users have matching skills`() {
        // given
        val projectSkill = ProjectSkillModel(project = project, skill = kotlin, level = 3, priority = SkillPriority.MUST_HAVE)
        every { projectRepository.findById(project.id) } returns Optional.of(project)
        every { projectSkillRepository.findByProject(project) } returns listOf(projectSkill)
        every { projectMemberRepository.findByProject(project) } returns emptyList()
        every { userSkillRepository.findBySkillIn(listOf(kotlin)) } returns emptyList()

        // when
        val result = matchingService.findCandidatesForProject(project.id, 0.0, 20)

        // then
        assertThat(result).isEmpty()
    }

    @Test
    fun `findCandidatesForProject returns candidates sorted by score descending`() {
        // given
        val psKotlin = ProjectSkillModel(project = project, skill = kotlin, level = 3, priority = SkillPriority.MUST_HAVE)
        val psSpring = ProjectSkillModel(project = project, skill = spring, level = 2, priority = SkillPriority.NICE_TO_HAVE)

        // user1 hat kotlin(4) + spring(3) → höherer Score
        val us1Kotlin = UserSkillModel(user = user1, skill = kotlin, level = 4)
        val us1Spring = UserSkillModel(user = user1, skill = spring, level = 3)
        // user2 hat nur kotlin(3) → niedrigerer Score
        val us2Kotlin = UserSkillModel(user = user2, skill = kotlin, level = 3)

        every { projectRepository.findById(project.id) } returns Optional.of(project)
        every { projectSkillRepository.findByProject(project) } returns listOf(psKotlin, psSpring)
        every { projectMemberRepository.findByProject(project) } returns emptyList()
        every { userSkillRepository.findBySkillIn(listOf(kotlin, spring)) } returns
            listOf(us1Kotlin, us1Spring, us2Kotlin)
        every { availabilityRepository.findByUser(user1) } returns emptyList()
        every { availabilityRepository.findByUser(user2) } returns emptyList()

        // when
        val result = matchingService.findCandidatesForProject(project.id, 0.0, 20)

        // then
        assertThat(result).hasSize(2)
        assertThat(result[0].userId).isEqualTo(user1.id)
        assertThat(result[1].userId).isEqualTo(user2.id)
        assertThat(result[0].score).isGreaterThanOrEqualTo(result[1].score)
    }

    @Test
    fun `findCandidatesForProject excludes active project members`() {
        // given
        val psKotlin = ProjectSkillModel(project = project, skill = kotlin, level = 3, priority = SkillPriority.MUST_HAVE)
        val us1Kotlin = UserSkillModel(user = user1, skill = kotlin, level = 4)
        val us2Kotlin = UserSkillModel(user = user2, skill = kotlin, level = 4)
        val activeMember =
            ProjectMemberModel(
                project = project,
                user = user1,
                status = ProjectMemberStatus.ACTIVE,
                joinedDate = Instant.now(),
            )

        every { projectRepository.findById(project.id) } returns Optional.of(project)
        every { projectSkillRepository.findByProject(project) } returns listOf(psKotlin)
        every { projectMemberRepository.findByProject(project) } returns listOf(activeMember)
        every { userSkillRepository.findBySkillIn(listOf(kotlin)) } returns listOf(us1Kotlin, us2Kotlin)
        every { availabilityRepository.findByUser(user2) } returns emptyList()

        // when
        val result = matchingService.findCandidatesForProject(project.id, 0.0, 20)

        // then
        assertThat(result).hasSize(1)
        assertThat(result[0].userId).isEqualTo(user2.id)
    }

    @Test
    fun `findCandidatesForProject includes left members as candidates`() {
        // given
        val psKotlin = ProjectSkillModel(project = project, skill = kotlin, level = 3, priority = SkillPriority.MUST_HAVE)
        val us1Kotlin = UserSkillModel(user = user1, skill = kotlin, level = 4)
        val leftMember =
            ProjectMemberModel(
                project = project,
                user = user1,
                status = ProjectMemberStatus.LEFT,
                joinedDate = Instant.now(),
            )

        every { projectRepository.findById(project.id) } returns Optional.of(project)
        every { projectSkillRepository.findByProject(project) } returns listOf(psKotlin)
        every { projectMemberRepository.findByProject(project) } returns listOf(leftMember)
        every { userSkillRepository.findBySkillIn(listOf(kotlin)) } returns listOf(us1Kotlin)
        every { availabilityRepository.findByUser(user1) } returns emptyList()

        // when
        val result = matchingService.findCandidatesForProject(project.id, 0.0, 20)

        // then
        assertThat(result).hasSize(1)
        assertThat(result[0].userId).isEqualTo(user1.id)
    }

    @Test
    fun `findCandidatesForProject excludes disabled users`() {
        // given
        val disabledUser =
            UserModel(
                email = "disabled@firma.de",
                passwordHash = "hashed",
                firstName = "Disabled",
                lastName = "User",
                role = role,
            ).apply { isEnabled = false }

        val psKotlin = ProjectSkillModel(project = project, skill = kotlin, level = 3, priority = SkillPriority.MUST_HAVE)
        val usDisabled = UserSkillModel(user = disabledUser, skill = kotlin, level = 5)

        every { projectRepository.findById(project.id) } returns Optional.of(project)
        every { projectSkillRepository.findByProject(project) } returns listOf(psKotlin)
        every { projectMemberRepository.findByProject(project) } returns emptyList()
        every { userSkillRepository.findBySkillIn(listOf(kotlin)) } returns listOf(usDisabled)

        // when
        val result = matchingService.findCandidatesForProject(project.id, 0.0, 20)

        // then
        assertThat(result).isEmpty()
    }

    @Test
    fun `findCandidatesForProject filters candidates below minScore`() {
        // given – user2 hat nur nice-to-have → niedriger score
        val psKotlin = ProjectSkillModel(project = project, skill = kotlin, level = 3, priority = SkillPriority.MUST_HAVE)
        val psSpring = ProjectSkillModel(project = project, skill = spring, level = 2, priority = SkillPriority.NICE_TO_HAVE)

        val us1Kotlin = UserSkillModel(user = user1, skill = kotlin, level = 4)
        val us1Spring = UserSkillModel(user = user1, skill = spring, level = 3)
        val us2Spring = UserSkillModel(user = user2, skill = spring, level = 1)

        every { projectRepository.findById(project.id) } returns Optional.of(project)
        every { projectSkillRepository.findByProject(project) } returns listOf(psKotlin, psSpring)
        every { projectMemberRepository.findByProject(project) } returns emptyList()
        every { userSkillRepository.findBySkillIn(listOf(kotlin, spring)) } returns
            listOf(us1Kotlin, us1Spring, us2Spring)
        every { availabilityRepository.findByUser(user1) } returns emptyList()
        every { availabilityRepository.findByUser(user2) } returns emptyList()

        // when – hoher minScore schließt user2 aus
        val result = matchingService.findCandidatesForProject(project.id, 0.8, 20)

        // then
        assertThat(result).allSatisfy { assertThat(it.score).isGreaterThanOrEqualTo(0.8) }
        assertThat(result.map { it.userId }).doesNotContain(user2.id)
    }

    @Test
    fun `findCandidatesForProject respects limit parameter`() {
        // given
        val psKotlin = ProjectSkillModel(project = project, skill = kotlin, level = 3, priority = SkillPriority.MUST_HAVE)
        val us1Kotlin = UserSkillModel(user = user1, skill = kotlin, level = 4)
        val us2Kotlin = UserSkillModel(user = user2, skill = kotlin, level = 3)

        every { projectRepository.findById(project.id) } returns Optional.of(project)
        every { projectSkillRepository.findByProject(project) } returns listOf(psKotlin)
        every { projectMemberRepository.findByProject(project) } returns emptyList()
        every { userSkillRepository.findBySkillIn(listOf(kotlin)) } returns listOf(us1Kotlin, us2Kotlin)
        every { availabilityRepository.findByUser(user1) } returns emptyList()
        every { availabilityRepository.findByUser(user2) } returns emptyList()

        // when
        val result = matchingService.findCandidatesForProject(project.id, 0.0, 1)

        // then
        assertThat(result).hasSize(1)
    }

    @Test
    fun `findCandidatesForProject calculates full must-have coverage when all fulfilled`() {
        // given – user1 erfüllt alle must-haves
        val psKotlin = ProjectSkillModel(project = project, skill = kotlin, level = 3, priority = SkillPriority.MUST_HAVE)
        val psSpring = ProjectSkillModel(project = project, skill = spring, level = 2, priority = SkillPriority.MUST_HAVE)

        val us1Kotlin = UserSkillModel(user = user1, skill = kotlin, level = 3)
        val us1Spring = UserSkillModel(user = user1, skill = spring, level = 2)

        every { projectRepository.findById(project.id) } returns Optional.of(project)
        every { projectSkillRepository.findByProject(project) } returns listOf(psKotlin, psSpring)
        every { projectMemberRepository.findByProject(project) } returns emptyList()
        every { userSkillRepository.findBySkillIn(listOf(kotlin, spring)) } returns listOf(us1Kotlin, us1Spring)
        every { availabilityRepository.findByUser(user1) } returns emptyList()

        // when
        val result = matchingService.findCandidatesForProject(project.id, 0.0, 20)

        // then
        assertThat(result).hasSize(1)
        assertThat(result[0].breakdown.mustHaveCoverage).isEqualTo(1.0)
    }

    @Test
    fun `findCandidatesForProject calculates partial must-have coverage`() {
        // given – user1 erfüllt nur 1 von 2 must-haves (hat spring nicht mit genug level)
        val psKotlin = ProjectSkillModel(project = project, skill = kotlin, level = 3, priority = SkillPriority.MUST_HAVE)
        val psSpring = ProjectSkillModel(project = project, skill = spring, level = 4, priority = SkillPriority.MUST_HAVE)

        val us1Kotlin = UserSkillModel(user = user1, skill = kotlin, level = 3)
        val us1Spring = UserSkillModel(user = user1, skill = spring, level = 2) // level zu niedrig

        every { projectRepository.findById(project.id) } returns Optional.of(project)
        every { projectSkillRepository.findByProject(project) } returns listOf(psKotlin, psSpring)
        every { projectMemberRepository.findByProject(project) } returns emptyList()
        every { userSkillRepository.findBySkillIn(listOf(kotlin, spring)) } returns listOf(us1Kotlin, us1Spring)
        every { availabilityRepository.findByUser(user1) } returns emptyList()

        // when
        val result = matchingService.findCandidatesForProject(project.id, 0.0, 20)

        // then
        assertThat(result).hasSize(1)
        assertThat(result[0].breakdown.mustHaveCoverage).isEqualTo(0.5)
    }

    @Test
    fun `findCandidatesForProject reports missing skills when user lacks a skill entirely`() {
        // given
        val psKotlin = ProjectSkillModel(project = project, skill = kotlin, level = 3, priority = SkillPriority.MUST_HAVE)
        val psDocker = ProjectSkillModel(project = project, skill = docker, level = 2, priority = SkillPriority.NICE_TO_HAVE)

        // user1 hat nur kotlin, nicht docker
        val us1Kotlin = UserSkillModel(user = user1, skill = kotlin, level = 3)

        every { projectRepository.findById(project.id) } returns Optional.of(project)
        every { projectSkillRepository.findByProject(project) } returns listOf(psKotlin, psDocker)
        every { projectMemberRepository.findByProject(project) } returns emptyList()
        every { userSkillRepository.findBySkillIn(listOf(kotlin, docker)) } returns listOf(us1Kotlin)
        every { availabilityRepository.findByUser(user1) } returns emptyList()

        // when
        val result = matchingService.findCandidatesForProject(project.id, 0.0, 20)

        // then
        assertThat(result).hasSize(1)
        assertThat(result[0].missingSkills).hasSize(1)
        assertThat(result[0].missingSkills[0].skillName).isEqualTo("docker")
        assertThat(result[0].missingSkills[0].priority).isEqualTo("NICE_TO_HAVE")
        assertThat(result[0].matchedSkills).hasSize(1)
        assertThat(result[0].matchedSkills[0].skillName).isEqualTo("kotlin")
    }

    @Test
    fun `findCandidatesForProject lists skill as matched even when user level is below required`() {
        // given – user hat skill aber unter dem geforderten level
        val psKotlin = ProjectSkillModel(project = project, skill = kotlin, level = 5, priority = SkillPriority.MUST_HAVE)
        val us1Kotlin = UserSkillModel(user = user1, skill = kotlin, level = 2)

        every { projectRepository.findById(project.id) } returns Optional.of(project)
        every { projectSkillRepository.findByProject(project) } returns listOf(psKotlin)
        every { projectMemberRepository.findByProject(project) } returns emptyList()
        every { userSkillRepository.findBySkillIn(listOf(kotlin)) } returns listOf(us1Kotlin)
        every { availabilityRepository.findByUser(user1) } returns emptyList()

        // when
        val result = matchingService.findCandidatesForProject(project.id, 0.0, 20)

        // then
        assertThat(result).hasSize(1)
        assertThat(result[0].matchedSkills).hasSize(1)
        assertThat(result[0].matchedSkills[0].userLevel).isEqualTo(2)
        assertThat(result[0].matchedSkills[0].requiredLevel).isEqualTo(5)
        assertThat(result[0].missingSkills).isEmpty()
        // must-have nicht erfüllt weil level zu niedrig
        assertThat(result[0].breakdown.mustHaveCoverage).isEqualTo(0.0)
    }

    @Test
    fun `findCandidatesForProject returns correct userName and email`() {
        // given
        val psKotlin = ProjectSkillModel(project = project, skill = kotlin, level = 1, priority = SkillPriority.MUST_HAVE)
        val us1Kotlin = UserSkillModel(user = user1, skill = kotlin, level = 3)

        every { projectRepository.findById(project.id) } returns Optional.of(project)
        every { projectSkillRepository.findByProject(project) } returns listOf(psKotlin)
        every { projectMemberRepository.findByProject(project) } returns emptyList()
        every { userSkillRepository.findBySkillIn(listOf(kotlin)) } returns listOf(us1Kotlin)
        every { availabilityRepository.findByUser(user1) } returns emptyList()

        // when
        val result = matchingService.findCandidatesForProject(project.id, 0.0, 20)

        // then
        assertThat(result).hasSize(1)
        assertThat(result[0].userName).isEqualTo("User One")
        assertThat(result[0].email).isEqualTo("user1@firma.de")
    }

    @Test
    fun `findCandidatesForProject calculates nice-to-have coverage correctly`() {
        // given – 2 nice-to-haves, user hat nur einen
        val psDocker = ProjectSkillModel(project = project, skill = docker, level = 2, priority = SkillPriority.NICE_TO_HAVE)
        val psReact = ProjectSkillModel(project = project, skill = react, level = 3, priority = SkillPriority.NICE_TO_HAVE)

        val us1Docker = UserSkillModel(user = user1, skill = docker, level = 1)

        every { projectRepository.findById(project.id) } returns Optional.of(project)
        every { projectSkillRepository.findByProject(project) } returns listOf(psDocker, psReact)
        every { projectMemberRepository.findByProject(project) } returns emptyList()
        every { userSkillRepository.findBySkillIn(listOf(docker, react)) } returns listOf(us1Docker)
        every { availabilityRepository.findByUser(user1) } returns emptyList()

        // when
        val result = matchingService.findCandidatesForProject(project.id, 0.0, 20)

        // then
        assertThat(result).hasSize(1)
        assertThat(result[0].breakdown.niceToHaveCoverage).isEqualTo(0.5)
    }

    @Test
    fun `findCandidatesForProject calculates level fit score with overfit cap`() {
        // given – user level weit über required → wird bei 1.2 gekappt
        val psKotlin = ProjectSkillModel(project = project, skill = kotlin, level = 1, priority = SkillPriority.MUST_HAVE)
        val us1Kotlin = UserSkillModel(user = user1, skill = kotlin, level = 5) // 5/1 = 5.0 → capped bei 1.2

        every { projectRepository.findById(project.id) } returns Optional.of(project)
        every { projectSkillRepository.findByProject(project) } returns listOf(psKotlin)
        every { projectMemberRepository.findByProject(project) } returns emptyList()
        every { userSkillRepository.findBySkillIn(listOf(kotlin)) } returns listOf(us1Kotlin)
        every { availabilityRepository.findByUser(user1) } returns emptyList()

        // when
        val result = matchingService.findCandidatesForProject(project.id, 0.0, 20)

        // then
        // levelFitScore = min(5.0/1.0, 1.2) / 1 / 1.2 = 1.2 / 1.2 = 1.0
        assertThat(result).hasSize(1)
        assertThat(result[0].breakdown.levelFitScore).isEqualTo(1.0)
    }

    @Test
    fun `findCandidatesForProject calculates availability score for partial coverage`() {
        // given – Projekt 6 Monate, User nur 3 Monate verfügbar
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
        every { projectMemberRepository.findByProject(project) } returns emptyList()
        every { userSkillRepository.findBySkillIn(listOf(kotlin)) } returns listOf(us1Kotlin)
        every { availabilityRepository.findByUser(user1) } returns listOf(availability)

        // when
        val result = matchingService.findCandidatesForProject(project.id, 0.0, 20)

        // then
        assertThat(result).hasSize(1)
        assertThat(result[0].breakdown.availabilityScore).isGreaterThan(0.0)
        assertThat(result[0].breakdown.availabilityScore).isLessThan(1.0)
    }

    @Test
    fun `findCandidatesForProject returns availability 1 when user has no availability entries`() {
        // given
        val psKotlin = ProjectSkillModel(project = project, skill = kotlin, level = 1, priority = SkillPriority.MUST_HAVE)
        val us1Kotlin = UserSkillModel(user = user1, skill = kotlin, level = 3)

        every { projectRepository.findById(project.id) } returns Optional.of(project)
        every { projectSkillRepository.findByProject(project) } returns listOf(psKotlin)
        every { projectMemberRepository.findByProject(project) } returns emptyList()
        every { userSkillRepository.findBySkillIn(listOf(kotlin)) } returns listOf(us1Kotlin)
        every { availabilityRepository.findByUser(user1) } returns emptyList()

        // when
        val result = matchingService.findCandidatesForProject(project.id, 0.0, 20)

        // then
        assertThat(result).hasSize(1)
        assertThat(result[0].breakdown.availabilityScore).isEqualTo(1.0)
    }

    @Test
    fun `findCandidatesForProject computes weighted total score correctly`() {
        // given – alle Komponenten auf 1.0 → Score = 1.0
        val psKotlin = ProjectSkillModel(project = project, skill = kotlin, level = 3, priority = SkillPriority.MUST_HAVE)
        val us1Kotlin = UserSkillModel(user = user1, skill = kotlin, level = 3)

        every { projectRepository.findById(project.id) } returns Optional.of(project)
        every { projectSkillRepository.findByProject(project) } returns listOf(psKotlin)
        every { projectMemberRepository.findByProject(project) } returns emptyList()
        every { userSkillRepository.findBySkillIn(listOf(kotlin)) } returns listOf(us1Kotlin)
        every { availabilityRepository.findByUser(user1) } returns emptyList()

        // when
        val result = matchingService.findCandidatesForProject(project.id, 0.0, 20)

        // then
        // mustHave=1.0, levelFit=3/3/1.2=0.833, niceToHave=1.0(keine), avail=1.0
        // score = 0.40*1.0 + 0.25*0.83 + 0.15*1.0 + 0.20*1.0 = 0.40+0.21+0.15+0.20 = 0.96
        assertThat(result).hasSize(1)
        assertThat(result[0].score).isBetween(0.9, 1.0)
    }

    // ── findProjectsForUser ──────────────────────────────────────────────

    @Test
    fun `findProjectsForUser returns empty list when user has no skills`() {
        // given
        every { userSkillRepository.findByUser(user1) } returns emptyList()

        // when
        val result = matchingService.findProjectsForUser(user1, 0.0, 20)

        // then
        assertThat(result).isEmpty()
    }

    @Test
    fun `findProjectsForUser returns matching projects sorted by score`() {
        // given
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

        // project hat kotlin + spring → besserer Match
        val ps1Kotlin = ProjectSkillModel(project = project, skill = kotlin, level = 3, priority = SkillPriority.MUST_HAVE)
        val ps1Spring = ProjectSkillModel(project = project, skill = spring, level = 2, priority = SkillPriority.MUST_HAVE)
        // project2 hat nur docker → kein Match
        val ps2Docker = ProjectSkillModel(project = project2, skill = docker, level = 3, priority = SkillPriority.MUST_HAVE)

        every { userSkillRepository.findByUser(user1) } returns listOf(usKotlin, usSpring)
        every { projectRepository.findAll() } returns listOf(project, project2)
        every { projectMemberRepository.findByProjectAndUser(project, user1) } returns null
        every { projectMemberRepository.findByProjectAndUser(project2, user1) } returns null
        every { projectSkillRepository.findByProject(project) } returns listOf(ps1Kotlin, ps1Spring)
        every { projectSkillRepository.findByProject(project2) } returns listOf(ps2Docker)
        every { availabilityRepository.findByUser(user1) } returns emptyList()

        // when
        val result = matchingService.findProjectsForUser(user1, 0.0, 20)

        // then
        assertThat(result).hasSize(2)
        assertThat(result[0].score).isGreaterThanOrEqualTo(result[1].score)
        assertThat(result[0].projectName).isEqualTo("Test Project")
    }

    @Test
    fun `findProjectsForUser excludes projects where user is active member`() {
        // given
        val usKotlin = UserSkillModel(user = user1, skill = kotlin, level = 4)
        val psKotlin = ProjectSkillModel(project = project, skill = kotlin, level = 3, priority = SkillPriority.MUST_HAVE)
        val activeMember =
            ProjectMemberModel(
                project = project,
                user = user1,
                status = ProjectMemberStatus.ACTIVE,
                joinedDate = Instant.now(),
            )

        every { userSkillRepository.findByUser(user1) } returns listOf(usKotlin)
        every { projectRepository.findAll() } returns listOf(project)
        every { projectMemberRepository.findByProjectAndUser(project, user1) } returns activeMember
        every { projectSkillRepository.findByProject(project) } returns listOf(psKotlin)

        // when
        val result = matchingService.findProjectsForUser(user1, 0.0, 20)

        // then
        assertThat(result).isEmpty()
    }

    @Test
    fun `findProjectsForUser includes projects where user has LEFT status`() {
        // given
        val usKotlin = UserSkillModel(user = user1, skill = kotlin, level = 4)
        val psKotlin = ProjectSkillModel(project = project, skill = kotlin, level = 3, priority = SkillPriority.MUST_HAVE)
        val leftMember =
            ProjectMemberModel(
                project = project,
                user = user1,
                status = ProjectMemberStatus.LEFT,
                joinedDate = Instant.now(),
            )

        every { userSkillRepository.findByUser(user1) } returns listOf(usKotlin)
        every { projectRepository.findAll() } returns listOf(project)
        every { projectMemberRepository.findByProjectAndUser(project, user1) } returns leftMember
        every { projectSkillRepository.findByProject(project) } returns listOf(psKotlin)
        every { availabilityRepository.findByUser(user1) } returns emptyList()

        // when
        val result = matchingService.findProjectsForUser(user1, 0.0, 20)

        // then
        assertThat(result).hasSize(1)
        assertThat(result[0].projectId).isEqualTo(project.id)
    }

    @Test
    fun `findProjectsForUser excludes COMPLETED and PAUSED projects`() {
        // given
        val completedProject =
            ProjectModel(
                name = "Completed",
                description = "Done",
                status = ProjectStatus.COMPLETED,
                startDate = LocalDate.of(2025, 1, 1),
                endDate = LocalDate.of(2025, 6, 1),
                maxMembers = 3,
                owner = owner,
            )
        val pausedProject =
            ProjectModel(
                name = "Paused",
                description = "On hold",
                status = ProjectStatus.PAUSED,
                startDate = LocalDate.of(2026, 1, 1),
                endDate = LocalDate.of(2026, 6, 1),
                maxMembers = 3,
                owner = owner,
            )

        val usKotlin = UserSkillModel(user = user1, skill = kotlin, level = 4)

        every { userSkillRepository.findByUser(user1) } returns listOf(usKotlin)
        every { projectRepository.findAll() } returns listOf(completedProject, pausedProject)

        // when
        val result = matchingService.findProjectsForUser(user1, 0.0, 20)

        // then
        assertThat(result).isEmpty()
    }

    @Test
    fun `findProjectsForUser includes PLANNED and ACTIVE projects`() {
        // given
        val activeProject =
            ProjectModel(
                name = "Active",
                description = "Running",
                status = ProjectStatus.ACTIVE,
                startDate = LocalDate.of(2026, 1, 1),
                endDate = LocalDate.of(2026, 12, 1),
                maxMembers = 5,
                owner = owner,
            )
        val usKotlin = UserSkillModel(user = user1, skill = kotlin, level = 3)
        val ps1Kotlin = ProjectSkillModel(project = project, skill = kotlin, level = 2, priority = SkillPriority.MUST_HAVE)
        val ps2Kotlin = ProjectSkillModel(project = activeProject, skill = kotlin, level = 2, priority = SkillPriority.MUST_HAVE)

        every { userSkillRepository.findByUser(user1) } returns listOf(usKotlin)
        every { projectRepository.findAll() } returns listOf(project, activeProject)
        every { projectMemberRepository.findByProjectAndUser(project, user1) } returns null
        every { projectMemberRepository.findByProjectAndUser(activeProject, user1) } returns null
        every { projectSkillRepository.findByProject(project) } returns listOf(ps1Kotlin)
        every { projectSkillRepository.findByProject(activeProject) } returns listOf(ps2Kotlin)
        every { availabilityRepository.findByUser(user1) } returns emptyList()

        // when
        val result = matchingService.findProjectsForUser(user1, 0.0, 20)

        // then
        assertThat(result).hasSize(2)
        assertThat(result.map { it.status }).containsExactlyInAnyOrder("PLANNED", "ACTIVE")
    }

    @Test
    fun `findProjectsForUser skips projects with no skills`() {
        // given
        val usKotlin = UserSkillModel(user = user1, skill = kotlin, level = 3)

        every { userSkillRepository.findByUser(user1) } returns listOf(usKotlin)
        every { projectRepository.findAll() } returns listOf(project)
        every { projectMemberRepository.findByProjectAndUser(project, user1) } returns null
        every { projectSkillRepository.findByProject(project) } returns emptyList()

        // when
        val result = matchingService.findProjectsForUser(user1, 0.0, 20)

        // then
        assertThat(result).isEmpty()
    }

    @Test
    fun `findProjectsForUser filters projects below minScore`() {
        // given – user hat nur docker, projekt will kotlin → niedriger score
        val usDocker = UserSkillModel(user = user1, skill = docker, level = 2)
        val psKotlin = ProjectSkillModel(project = project, skill = kotlin, level = 4, priority = SkillPriority.MUST_HAVE)

        every { userSkillRepository.findByUser(user1) } returns listOf(usDocker)
        every { projectRepository.findAll() } returns listOf(project)
        every { projectMemberRepository.findByProjectAndUser(project, user1) } returns null
        every { projectSkillRepository.findByProject(project) } returns listOf(psKotlin)
        every { availabilityRepository.findByUser(user1) } returns emptyList()

        // when
        val result = matchingService.findProjectsForUser(user1, 0.9, 20)

        // then
        assertThat(result).isEmpty()
    }

    @Test
    fun `findProjectsForUser respects limit parameter`() {
        // given
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
        every { projectRepository.findAll() } returns listOf(project, project2)
        every { projectMemberRepository.findByProjectAndUser(project, user1) } returns null
        every { projectMemberRepository.findByProjectAndUser(project2, user1) } returns null
        every { projectSkillRepository.findByProject(project) } returns listOf(ps1Kotlin)
        every { projectSkillRepository.findByProject(project2) } returns listOf(ps2Kotlin)
        every { availabilityRepository.findByUser(user1) } returns emptyList()

        // when
        val result = matchingService.findProjectsForUser(user1, 0.0, 1)

        // then
        assertThat(result).hasSize(1)
    }

    @Test
    fun `findProjectsForUser returns correct project metadata in result`() {
        // given
        val usKotlin = UserSkillModel(user = user1, skill = kotlin, level = 3)
        val psKotlin = ProjectSkillModel(project = project, skill = kotlin, level = 2, priority = SkillPriority.MUST_HAVE)

        every { userSkillRepository.findByUser(user1) } returns listOf(usKotlin)
        every { projectRepository.findAll() } returns listOf(project)
        every { projectMemberRepository.findByProjectAndUser(project, user1) } returns null
        every { projectSkillRepository.findByProject(project) } returns listOf(psKotlin)
        every { availabilityRepository.findByUser(user1) } returns emptyList()

        // when
        val result = matchingService.findProjectsForUser(user1, 0.0, 20)

        // then
        assertThat(result).hasSize(1)
        assertThat(result[0].projectId).isEqualTo(project.id)
        assertThat(result[0].projectName).isEqualTo("Test Project")
        assertThat(result[0].projectDescription).isEqualTo("Test")
        assertThat(result[0].status).isEqualTo("PLANNED")
        assertThat(result[0].ownerName).isEqualTo("PM User")
    }
}
