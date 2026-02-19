package org.efehan.skillmatcherbackend.service

import io.mockk.every
import io.mockk.impl.annotations.InjectMockKs
import io.mockk.impl.annotations.MockK
import io.mockk.junit5.MockKExtension
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.efehan.skillmatcherbackend.core.matching.MatchingService
import org.efehan.skillmatcherbackend.exception.GlobalErrorCode
import org.efehan.skillmatcherbackend.fixtures.builder.ProjectBuilder
import org.efehan.skillmatcherbackend.fixtures.builder.ProjectSkillBuilder
import org.efehan.skillmatcherbackend.fixtures.builder.SkillBuilder
import org.efehan.skillmatcherbackend.fixtures.builder.UserAvailabilityBuilder
import org.efehan.skillmatcherbackend.fixtures.builder.UserBuilder
import org.efehan.skillmatcherbackend.fixtures.builder.UserSkillBuilder
import org.efehan.skillmatcherbackend.persistence.ProjectMemberStatus
import org.efehan.skillmatcherbackend.persistence.ProjectRepository
import org.efehan.skillmatcherbackend.persistence.ProjectSkillRepository
import org.efehan.skillmatcherbackend.persistence.ProjectStatus
import org.efehan.skillmatcherbackend.persistence.SkillPriority
import org.efehan.skillmatcherbackend.persistence.UserAvailabilityRepository
import org.efehan.skillmatcherbackend.persistence.UserSkillRepository
import org.efehan.skillmatcherbackend.shared.exceptions.EntryNotFoundException
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import java.time.LocalDate
import java.util.Optional

@ExtendWith(MockKExtension::class)
@DisplayName("MatchingService Unit Tests")
class MatchingServiceTest {
    @MockK
    private lateinit var projectRepo: ProjectRepository

    @MockK
    private lateinit var projectSkillRepo: ProjectSkillRepository

    @MockK
    private lateinit var userSkillRepo: UserSkillRepository

    @MockK
    private lateinit var availabilityRepo: UserAvailabilityRepository

    @InjectMockKs
    private lateinit var matchingService: MatchingService

    @Test
    fun `findCandidatesForProject throws EntryNotFoundException when project not found`() {
        // given
        every { projectRepo.findById("nonexistent") } returns Optional.empty()

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
        val owner = UserBuilder().build(email = "pm@firma.de", firstName = "PM", lastName = "User")
        val project = ProjectBuilder().build(owner = owner)
        every { projectRepo.findById(project.id) } returns Optional.of(project)
        every { projectSkillRepo.findByProject(project) } returns emptyList()

        // when
        val result = matchingService.findCandidatesForProject(project.id, 0.0, 20)

        // then
        assertThat(result).isEmpty()
    }

    @Test
    fun `findCandidatesForProject returns empty list when no candidates match query`() {
        // given
        val owner = UserBuilder().build(email = "pm@firma.de", firstName = "PM", lastName = "User")
        val project = ProjectBuilder().build(owner = owner)
        val kotlin = SkillBuilder().build(name = "kotlin")
        val psKotlin = ProjectSkillBuilder().build(project = project, skill = kotlin, level = 3, priority = SkillPriority.MUST_HAVE)
        every { projectRepo.findById(project.id) } returns Optional.of(project)
        every { projectSkillRepo.findByProject(project) } returns listOf(psKotlin)
        every { userSkillRepo.findMatchableBySkillsForProject(any(), project, ProjectMemberStatus.ACTIVE) } returns emptyList()

        // when
        val result = matchingService.findCandidatesForProject(project.id, 0.0, 20)

        // then
        assertThat(result).isEmpty()
    }

    @Test
    fun `findCandidatesForProject returns candidates sorted by score descending`() {
        // given
        val owner = UserBuilder().build(email = "pm@firma.de", firstName = "PM", lastName = "User")
        val project = ProjectBuilder().build(owner = owner)
        val user1 = UserBuilder().build(email = "user1@firma.de", firstName = "User", lastName = "One")
        val user2 = UserBuilder().build(email = "user2@firma.de", firstName = "User", lastName = "Two")
        val kotlin = SkillBuilder().build(name = "kotlin")
        val spring = SkillBuilder().build(name = "spring boot")
        val psKotlin = ProjectSkillBuilder().build(project = project, skill = kotlin, level = 3, priority = SkillPriority.MUST_HAVE)
        val psSpring = ProjectSkillBuilder().build(project = project, skill = spring, level = 2, priority = SkillPriority.NICE_TO_HAVE)
        val us1Kotlin = UserSkillBuilder().build(user = user1, skill = kotlin, level = 4)
        val us1Spring = UserSkillBuilder().build(user = user1, skill = spring, level = 3)
        val us2Kotlin = UserSkillBuilder().build(user = user2, skill = kotlin, level = 3)

        every { projectRepo.findById(project.id) } returns Optional.of(project)
        every { projectSkillRepo.findByProject(project) } returns listOf(psKotlin, psSpring)
        every { userSkillRepo.findMatchableBySkillsForProject(any(), project, ProjectMemberStatus.ACTIVE) } returns
            listOf(us1Kotlin, us1Spring, us2Kotlin)
        every { availabilityRepo.findByUserIn(any()) } returns emptyList()

        // when
        val result = matchingService.findCandidatesForProject(project.id, 0.0, 20)

        // then
        assertThat(result).hasSize(2)
        assertThat(result[0].userId).isEqualTo(user1.id)
        assertThat(result[1].userId).isEqualTo(user2.id)
        assertThat(result[0].score).isGreaterThanOrEqualTo(result[1].score)
    }

    @Test
    fun `findCandidatesForProject excludes active project members via query result`() {
        // given
        val owner = UserBuilder().build(email = "pm@firma.de", firstName = "PM", lastName = "User")
        val project = ProjectBuilder().build(owner = owner)
        val user2 = UserBuilder().build(email = "user2@firma.de", firstName = "User", lastName = "Two")
        val kotlin = SkillBuilder().build(name = "kotlin")
        val psKotlin = ProjectSkillBuilder().build(project = project, skill = kotlin, level = 3, priority = SkillPriority.MUST_HAVE)
        val us2Kotlin = UserSkillBuilder().build(user = user2, skill = kotlin, level = 4)

        every { projectRepo.findById(project.id) } returns Optional.of(project)
        every { projectSkillRepo.findByProject(project) } returns listOf(psKotlin)
        every { userSkillRepo.findMatchableBySkillsForProject(any(), project, ProjectMemberStatus.ACTIVE) } returns listOf(us2Kotlin)
        every { availabilityRepo.findByUserIn(any()) } returns emptyList()

        // when
        val result = matchingService.findCandidatesForProject(project.id, 0.0, 20)

        // then
        assertThat(result).hasSize(1)
        assertThat(result[0].userId).isEqualTo(user2.id)
    }

    @Test
    fun `findCandidatesForProject includes left members when query returns them`() {
        // given
        val owner = UserBuilder().build(email = "pm@firma.de", firstName = "PM", lastName = "User")
        val project = ProjectBuilder().build(owner = owner)
        val user1 = UserBuilder().build(email = "user1@firma.de", firstName = "User", lastName = "One")
        val kotlin = SkillBuilder().build(name = "kotlin")
        val psKotlin = ProjectSkillBuilder().build(project = project, skill = kotlin, level = 3, priority = SkillPriority.MUST_HAVE)
        val us1Kotlin = UserSkillBuilder().build(user = user1, skill = kotlin, level = 4)

        every { projectRepo.findById(project.id) } returns Optional.of(project)
        every { projectSkillRepo.findByProject(project) } returns listOf(psKotlin)
        every { userSkillRepo.findMatchableBySkillsForProject(any(), project, ProjectMemberStatus.ACTIVE) } returns listOf(us1Kotlin)
        every { availabilityRepo.findByUserIn(any()) } returns emptyList()

        // when
        val result = matchingService.findCandidatesForProject(project.id, 0.0, 20)

        // then
        assertThat(result).hasSize(1)
        assertThat(result[0].userId).isEqualTo(user1.id)
    }

    @Test
    fun `findCandidatesForProject filters candidates below minScore`() {
        // given
        val owner = UserBuilder().build(email = "pm@firma.de", firstName = "PM", lastName = "User")
        val project = ProjectBuilder().build(owner = owner)
        val user1 = UserBuilder().build(email = "user1@firma.de", firstName = "User", lastName = "One")
        val user2 = UserBuilder().build(email = "user2@firma.de", firstName = "User", lastName = "Two")
        val kotlin = SkillBuilder().build(name = "kotlin")
        val spring = SkillBuilder().build(name = "spring boot")
        val psKotlin = ProjectSkillBuilder().build(project = project, skill = kotlin, level = 3, priority = SkillPriority.MUST_HAVE)
        val psSpring = ProjectSkillBuilder().build(project = project, skill = spring, level = 2, priority = SkillPriority.NICE_TO_HAVE)
        val us1Kotlin = UserSkillBuilder().build(user = user1, skill = kotlin, level = 4)
        val us1Spring = UserSkillBuilder().build(user = user1, skill = spring, level = 3)
        val us2Spring = UserSkillBuilder().build(user = user2, skill = spring, level = 1)

        every { projectRepo.findById(project.id) } returns Optional.of(project)
        every { projectSkillRepo.findByProject(project) } returns listOf(psKotlin, psSpring)
        every { userSkillRepo.findMatchableBySkillsForProject(any(), project, ProjectMemberStatus.ACTIVE) } returns
            listOf(us1Kotlin, us1Spring, us2Spring)
        every { availabilityRepo.findByUserIn(any()) } returns emptyList()

        // when
        val result = matchingService.findCandidatesForProject(project.id, 0.8, 20)

        // then
        assertThat(result).allSatisfy { assertThat(it.score).isGreaterThanOrEqualTo(0.8) }
        assertThat(result.map { it.userId }).doesNotContain(user2.id)
    }

    @Test
    fun `findCandidatesForProject respects limit parameter`() {
        // given
        val owner = UserBuilder().build(email = "pm@firma.de", firstName = "PM", lastName = "User")
        val project = ProjectBuilder().build(owner = owner)
        val user1 = UserBuilder().build(email = "user1@firma.de", firstName = "User", lastName = "One")
        val user2 = UserBuilder().build(email = "user2@firma.de", firstName = "User", lastName = "Two")
        val kotlin = SkillBuilder().build(name = "kotlin")
        val psKotlin = ProjectSkillBuilder().build(project = project, skill = kotlin, level = 3, priority = SkillPriority.MUST_HAVE)
        val us1Kotlin = UserSkillBuilder().build(user = user1, skill = kotlin, level = 4)
        val us2Kotlin = UserSkillBuilder().build(user = user2, skill = kotlin, level = 3)

        every { projectRepo.findById(project.id) } returns Optional.of(project)
        every { projectSkillRepo.findByProject(project) } returns listOf(psKotlin)
        every { userSkillRepo.findMatchableBySkillsForProject(any(), project, ProjectMemberStatus.ACTIVE) } returns
            listOf(us1Kotlin, us2Kotlin)
        every { availabilityRepo.findByUserIn(any()) } returns emptyList()

        // when
        val result = matchingService.findCandidatesForProject(project.id, 0.0, 1)

        // then
        assertThat(result).hasSize(1)
    }

    @Test
    fun `findCandidatesForProject calculates full must-have coverage when all fulfilled`() {
        // given
        val owner = UserBuilder().build(email = "pm@firma.de", firstName = "PM", lastName = "User")
        val project = ProjectBuilder().build(owner = owner)
        val user1 = UserBuilder().build(email = "user1@firma.de", firstName = "User", lastName = "One")
        val kotlin = SkillBuilder().build(name = "kotlin")
        val spring = SkillBuilder().build(name = "spring boot")
        val psKotlin = ProjectSkillBuilder().build(project = project, skill = kotlin, level = 3, priority = SkillPriority.MUST_HAVE)
        val psSpring = ProjectSkillBuilder().build(project = project, skill = spring, level = 2, priority = SkillPriority.MUST_HAVE)
        val us1Kotlin = UserSkillBuilder().build(user = user1, skill = kotlin, level = 3)
        val us1Spring = UserSkillBuilder().build(user = user1, skill = spring, level = 2)

        every { projectRepo.findById(project.id) } returns Optional.of(project)
        every { projectSkillRepo.findByProject(project) } returns listOf(psKotlin, psSpring)
        every { userSkillRepo.findMatchableBySkillsForProject(any(), project, ProjectMemberStatus.ACTIVE) } returns
            listOf(us1Kotlin, us1Spring)
        every { availabilityRepo.findByUserIn(any()) } returns emptyList()

        // when
        val result = matchingService.findCandidatesForProject(project.id, 0.0, 20)

        // then
        assertThat(result).hasSize(1)
        assertThat(result[0].breakdown.mustHaveCoverage).isEqualTo(1.0)
    }

    @Test
    fun `findCandidatesForProject calculates partial must-have coverage`() {
        // given
        val owner = UserBuilder().build(email = "pm@firma.de", firstName = "PM", lastName = "User")
        val project = ProjectBuilder().build(owner = owner)
        val user1 = UserBuilder().build(email = "user1@firma.de", firstName = "User", lastName = "One")
        val kotlin = SkillBuilder().build(name = "kotlin")
        val spring = SkillBuilder().build(name = "spring boot")
        val psKotlin = ProjectSkillBuilder().build(project = project, skill = kotlin, level = 3, priority = SkillPriority.MUST_HAVE)
        val psSpring = ProjectSkillBuilder().build(project = project, skill = spring, level = 4, priority = SkillPriority.MUST_HAVE)
        val us1Kotlin = UserSkillBuilder().build(user = user1, skill = kotlin, level = 3)
        val us1Spring = UserSkillBuilder().build(user = user1, skill = spring, level = 2)

        every { projectRepo.findById(project.id) } returns Optional.of(project)
        every { projectSkillRepo.findByProject(project) } returns listOf(psKotlin, psSpring)
        every { userSkillRepo.findMatchableBySkillsForProject(any(), project, ProjectMemberStatus.ACTIVE) } returns
            listOf(us1Kotlin, us1Spring)
        every { availabilityRepo.findByUserIn(any()) } returns emptyList()

        // when
        val result = matchingService.findCandidatesForProject(project.id, 0.0, 20)

        // then
        assertThat(result).hasSize(1)
        assertThat(result[0].breakdown.mustHaveCoverage).isEqualTo(0.5)
    }

    @Test
    fun `findCandidatesForProject reports missing skills when user lacks a skill entirely`() {
        // given
        val owner = UserBuilder().build(email = "pm@firma.de", firstName = "PM", lastName = "User")
        val project = ProjectBuilder().build(owner = owner)
        val user1 = UserBuilder().build(email = "user1@firma.de", firstName = "User", lastName = "One")
        val kotlin = SkillBuilder().build(name = "kotlin")
        val docker = SkillBuilder().build(name = "docker")
        val psKotlin = ProjectSkillBuilder().build(project = project, skill = kotlin, level = 3, priority = SkillPriority.MUST_HAVE)
        val psDocker = ProjectSkillBuilder().build(project = project, skill = docker, level = 2, priority = SkillPriority.NICE_TO_HAVE)
        val us1Kotlin = UserSkillBuilder().build(user = user1, skill = kotlin, level = 3)

        every { projectRepo.findById(project.id) } returns Optional.of(project)
        every { projectSkillRepo.findByProject(project) } returns listOf(psKotlin, psDocker)
        every { userSkillRepo.findMatchableBySkillsForProject(any(), project, ProjectMemberStatus.ACTIVE) } returns listOf(us1Kotlin)
        every { availabilityRepo.findByUserIn(any()) } returns emptyList()

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
        // given
        val owner = UserBuilder().build(email = "pm@firma.de", firstName = "PM", lastName = "User")
        val project = ProjectBuilder().build(owner = owner)
        val user1 = UserBuilder().build(email = "user1@firma.de", firstName = "User", lastName = "One")
        val kotlin = SkillBuilder().build(name = "kotlin")
        val psKotlin = ProjectSkillBuilder().build(project = project, skill = kotlin, level = 5, priority = SkillPriority.MUST_HAVE)
        val us1Kotlin = UserSkillBuilder().build(user = user1, skill = kotlin, level = 2)

        every { projectRepo.findById(project.id) } returns Optional.of(project)
        every { projectSkillRepo.findByProject(project) } returns listOf(psKotlin)
        every { userSkillRepo.findMatchableBySkillsForProject(any(), project, ProjectMemberStatus.ACTIVE) } returns listOf(us1Kotlin)
        every { availabilityRepo.findByUserIn(any()) } returns emptyList()

        // when
        val result = matchingService.findCandidatesForProject(project.id, 0.0, 20)

        // then
        assertThat(result).hasSize(1)
        assertThat(result[0].matchedSkills).hasSize(1)
        assertThat(result[0].matchedSkills[0].userLevel).isEqualTo(2)
        assertThat(result[0].matchedSkills[0].requiredLevel).isEqualTo(5)
        assertThat(result[0].missingSkills).isEmpty()
        assertThat(result[0].breakdown.mustHaveCoverage).isEqualTo(0.0)
    }

    @Test
    fun `findCandidatesForProject calculates nice-to-have coverage correctly`() {
        // given
        val owner = UserBuilder().build(email = "pm@firma.de", firstName = "PM", lastName = "User")
        val project = ProjectBuilder().build(owner = owner)
        val user1 = UserBuilder().build(email = "user1@firma.de", firstName = "User", lastName = "One")
        val docker = SkillBuilder().build(name = "docker")
        val react = SkillBuilder().build(name = "react")
        val psDocker = ProjectSkillBuilder().build(project = project, skill = docker, level = 2, priority = SkillPriority.NICE_TO_HAVE)
        val psReact = ProjectSkillBuilder().build(project = project, skill = react, level = 3, priority = SkillPriority.NICE_TO_HAVE)
        val us1Docker = UserSkillBuilder().build(user = user1, skill = docker, level = 1)

        every { projectRepo.findById(project.id) } returns Optional.of(project)
        every { projectSkillRepo.findByProject(project) } returns listOf(psDocker, psReact)
        every { userSkillRepo.findMatchableBySkillsForProject(any(), project, ProjectMemberStatus.ACTIVE) } returns listOf(us1Docker)
        every { availabilityRepo.findByUserIn(any()) } returns emptyList()

        // when
        val result = matchingService.findCandidatesForProject(project.id, 0.0, 20)

        // then
        assertThat(result).hasSize(1)
        assertThat(result[0].breakdown.niceToHaveCoverage).isEqualTo(0.5)
    }

    @Test
    fun `findCandidatesForProject calculates level fit score with overfit cap`() {
        // given
        val owner = UserBuilder().build(email = "pm@firma.de", firstName = "PM", lastName = "User")
        val project = ProjectBuilder().build(owner = owner)
        val user1 = UserBuilder().build(email = "user1@firma.de", firstName = "User", lastName = "One")
        val kotlin = SkillBuilder().build(name = "kotlin")
        val psKotlin = ProjectSkillBuilder().build(project = project, skill = kotlin, level = 1, priority = SkillPriority.MUST_HAVE)
        val us1Kotlin = UserSkillBuilder().build(user = user1, skill = kotlin, level = 5)

        every { projectRepo.findById(project.id) } returns Optional.of(project)
        every { projectSkillRepo.findByProject(project) } returns listOf(psKotlin)
        every { userSkillRepo.findMatchableBySkillsForProject(any(), project, ProjectMemberStatus.ACTIVE) } returns listOf(us1Kotlin)
        every { availabilityRepo.findByUserIn(any()) } returns emptyList()

        // when
        val result = matchingService.findCandidatesForProject(project.id, 0.0, 20)

        // then
        assertThat(result).hasSize(1)
        assertThat(result[0].breakdown.levelFitScore).isEqualTo(1.0)
    }

    @Test
    fun `findCandidatesForProject calculates availability score for partial coverage`() {
        // given
        val owner = UserBuilder().build(email = "pm@firma.de", firstName = "PM", lastName = "User")
        val project = ProjectBuilder().build(owner = owner)
        val user1 = UserBuilder().build(email = "user1@firma.de", firstName = "User", lastName = "One")
        val kotlin = SkillBuilder().build(name = "kotlin")
        val psKotlin = ProjectSkillBuilder().build(project = project, skill = kotlin, level = 1, priority = SkillPriority.MUST_HAVE)
        val us1Kotlin = UserSkillBuilder().build(user = user1, skill = kotlin, level = 3)
        val availability =
            UserAvailabilityBuilder().build(
                user = user1,
                availableFrom = LocalDate.of(2026, 3, 1),
                availableTo = LocalDate.of(2026, 6, 1),
            )

        every { projectRepo.findById(project.id) } returns Optional.of(project)
        every { projectSkillRepo.findByProject(project) } returns listOf(psKotlin)
        every { userSkillRepo.findMatchableBySkillsForProject(any(), project, ProjectMemberStatus.ACTIVE) } returns listOf(us1Kotlin)
        every { availabilityRepo.findByUserIn(any()) } returns listOf(availability)

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
        val owner = UserBuilder().build(email = "pm@firma.de", firstName = "PM", lastName = "User")
        val project = ProjectBuilder().build(owner = owner)
        val user1 = UserBuilder().build(email = "user1@firma.de", firstName = "User", lastName = "One")
        val kotlin = SkillBuilder().build(name = "kotlin")
        val psKotlin = ProjectSkillBuilder().build(project = project, skill = kotlin, level = 1, priority = SkillPriority.MUST_HAVE)
        val us1Kotlin = UserSkillBuilder().build(user = user1, skill = kotlin, level = 3)

        every { projectRepo.findById(project.id) } returns Optional.of(project)
        every { projectSkillRepo.findByProject(project) } returns listOf(psKotlin)
        every { userSkillRepo.findMatchableBySkillsForProject(any(), project, ProjectMemberStatus.ACTIVE) } returns listOf(us1Kotlin)
        every { availabilityRepo.findByUserIn(any()) } returns emptyList()

        // when
        val result = matchingService.findCandidatesForProject(project.id, 0.0, 20)

        // then
        assertThat(result).hasSize(1)
        assertThat(result[0].breakdown.availabilityScore).isEqualTo(1.0)
    }

    @Test
    fun `findProjectsForUser returns empty list when user has no skills`() {
        // given
        val user = UserBuilder().build()
        every { userSkillRepo.findByUser(user) } returns emptyList()

        // when
        val result = matchingService.findProjectsForUser(user, 0.0, 20)

        // then
        assertThat(result).isEmpty()
    }

    @Test
    fun `findProjectsForUser excludes active memberships via query result`() {
        // given
        val user = UserBuilder().build()
        val kotlin = SkillBuilder().build(name = "kotlin")
        val usKotlin = UserSkillBuilder().build(user = user, skill = kotlin, level = 4)
        every { userSkillRepo.findByUser(user) } returns listOf(usKotlin)
        every {
            projectRepo.findMatchableForUser(user, any(), ProjectMemberStatus.ACTIVE)
        } returns emptyList()

        // when
        val result = matchingService.findProjectsForUser(user, 0.0, 20)

        // then
        assertThat(result).isEmpty()
    }

    @Test
    fun `findProjectsForUser returns matching projects sorted by score`() {
        // given
        val owner = UserBuilder().build(email = "pm@firma.de", firstName = "PM", lastName = "User")
        val user = UserBuilder().build()
        val project1 = ProjectBuilder().build(name = "Test Project", description = "Test", owner = owner)
        val project2 =
            ProjectBuilder().build(name = "Other Project", description = "Other", owner = owner)
        val kotlin = SkillBuilder().build(name = "kotlin")
        val spring = SkillBuilder().build(name = "spring boot")
        val docker = SkillBuilder().build(name = "docker")
        val usKotlin = UserSkillBuilder().build(user = user, skill = kotlin, level = 4)
        val usSpring = UserSkillBuilder().build(user = user, skill = spring, level = 3)
        val ps1Kotlin = ProjectSkillBuilder().build(project = project1, skill = kotlin, level = 3, priority = SkillPriority.MUST_HAVE)
        val ps1Spring = ProjectSkillBuilder().build(project = project1, skill = spring, level = 2, priority = SkillPriority.MUST_HAVE)
        val ps2Docker = ProjectSkillBuilder().build(project = project2, skill = docker, level = 3, priority = SkillPriority.MUST_HAVE)

        every { userSkillRepo.findByUser(user) } returns listOf(usKotlin, usSpring)
        every {
            projectRepo.findMatchableForUser(user, any(), ProjectMemberStatus.ACTIVE)
        } returns listOf(project1, project2)
        every { projectSkillRepo.findByProjectIn(any()) } returns listOf(ps1Kotlin, ps1Spring, ps2Docker)
        every { availabilityRepo.findByUser(user) } returns emptyList()

        // when
        val result = matchingService.findProjectsForUser(user, 0.0, 20)

        // then
        assertThat(result).hasSize(2)
        assertThat(result[0].score).isGreaterThanOrEqualTo(result[1].score)
        assertThat(result[0].projectName).isEqualTo("Test Project")
    }

    @Test
    fun `findProjectsForUser skips projects with no skills`() {
        // given
        val owner = UserBuilder().build(email = "pm@firma.de", firstName = "PM", lastName = "User")
        val user = UserBuilder().build()
        val project = ProjectBuilder().build(owner = owner)
        val kotlin = SkillBuilder().build(name = "kotlin")
        val usKotlin = UserSkillBuilder().build(user = user, skill = kotlin, level = 3)

        every { userSkillRepo.findByUser(user) } returns listOf(usKotlin)
        every {
            projectRepo.findMatchableForUser(user, any(), ProjectMemberStatus.ACTIVE)
        } returns listOf(project)
        every { projectSkillRepo.findByProjectIn(any()) } returns emptyList()
        every { availabilityRepo.findByUser(user) } returns emptyList()

        // when
        val result = matchingService.findProjectsForUser(user, 0.0, 20)

        // then
        assertThat(result).isEmpty()
    }

    @Test
    fun `findProjectsForUser filters projects below minScore`() {
        // given
        val owner = UserBuilder().build(email = "pm@firma.de", firstName = "PM", lastName = "User")
        val user = UserBuilder().build()
        val project = ProjectBuilder().build(owner = owner)
        val kotlin = SkillBuilder().build(name = "kotlin")
        val docker = SkillBuilder().build(name = "docker")
        val usDocker = UserSkillBuilder().build(user = user, skill = docker, level = 2)
        val psKotlin = ProjectSkillBuilder().build(project = project, skill = kotlin, level = 4, priority = SkillPriority.MUST_HAVE)

        every { userSkillRepo.findByUser(user) } returns listOf(usDocker)
        every {
            projectRepo.findMatchableForUser(user, any(), ProjectMemberStatus.ACTIVE)
        } returns listOf(project)
        every { projectSkillRepo.findByProjectIn(any()) } returns listOf(psKotlin)
        every { availabilityRepo.findByUser(user) } returns emptyList()

        // when
        val result = matchingService.findProjectsForUser(user, 0.9, 20)

        // then
        assertThat(result).isEmpty()
    }

    @Test
    fun `findProjectsForUser respects limit parameter`() {
        // given
        val owner = UserBuilder().build(email = "pm@firma.de", firstName = "PM", lastName = "User")
        val user = UserBuilder().build()
        val project1 = ProjectBuilder().build(owner = owner)
        val project2 =
            ProjectBuilder().build(name = "Project 2", description = "Desc", status = ProjectStatus.ACTIVE, owner = owner)
        val kotlin = SkillBuilder().build(name = "kotlin")
        val usKotlin = UserSkillBuilder().build(user = user, skill = kotlin, level = 4)
        val ps1Kotlin = ProjectSkillBuilder().build(project = project1, skill = kotlin, level = 2, priority = SkillPriority.MUST_HAVE)
        val ps2Kotlin = ProjectSkillBuilder().build(project = project2, skill = kotlin, level = 2, priority = SkillPriority.MUST_HAVE)

        every { userSkillRepo.findByUser(user) } returns listOf(usKotlin)
        every {
            projectRepo.findMatchableForUser(user, any(), ProjectMemberStatus.ACTIVE)
        } returns listOf(project1, project2)
        every { projectSkillRepo.findByProjectIn(any()) } returns listOf(ps1Kotlin, ps2Kotlin)
        every { availabilityRepo.findByUser(user) } returns emptyList()

        // when
        val result = matchingService.findProjectsForUser(user, 0.0, 1)

        // then
        assertThat(result).hasSize(1)
    }

    @Test
    fun `findProjectsForUser returns correct project metadata in result`() {
        // given
        val owner = UserBuilder().build(email = "pm@firma.de", firstName = "PM", lastName = "User")
        val user = UserBuilder().build()
        val project = ProjectBuilder().build(name = "Test Project", description = "Test", owner = owner)
        val kotlin = SkillBuilder().build(name = "kotlin")
        val usKotlin = UserSkillBuilder().build(user = user, skill = kotlin, level = 3)
        val psKotlin = ProjectSkillBuilder().build(project = project, skill = kotlin, level = 2, priority = SkillPriority.MUST_HAVE)

        every { userSkillRepo.findByUser(user) } returns listOf(usKotlin)
        every {
            projectRepo.findMatchableForUser(user, any(), ProjectMemberStatus.ACTIVE)
        } returns listOf(project)
        every { projectSkillRepo.findByProjectIn(any()) } returns listOf(psKotlin)
        every { availabilityRepo.findByUser(user) } returns emptyList()

        // when
        val result = matchingService.findProjectsForUser(user, 0.0, 20)

        // then
        assertThat(result).hasSize(1)
        assertThat(result[0].projectId).isEqualTo(project.id)
        assertThat(result[0].projectName).isEqualTo("Test Project")
        assertThat(result[0].projectDescription).isEqualTo("Test")
        assertThat(result[0].status).isEqualTo("PLANNED")
        assertThat(result[0].ownerName).isEqualTo("PM User")
    }
}
