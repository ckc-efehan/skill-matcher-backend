package org.efehan.skillmatcherbackend.service

import io.mockk.every
import io.mockk.impl.annotations.MockK
import io.mockk.junit5.MockKExtension
import io.mockk.verify
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.efehan.skillmatcherbackend.core.skill.UserSkillService
import org.efehan.skillmatcherbackend.exception.GlobalErrorCode
import org.efehan.skillmatcherbackend.persistence.RoleModel
import org.efehan.skillmatcherbackend.persistence.SkillModel
import org.efehan.skillmatcherbackend.persistence.SkillRepository
import org.efehan.skillmatcherbackend.persistence.UserModel
import org.efehan.skillmatcherbackend.persistence.UserSkillModel
import org.efehan.skillmatcherbackend.persistence.UserSkillRepository
import org.efehan.skillmatcherbackend.shared.exceptions.EntryNotFoundException
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.springframework.security.access.AccessDeniedException
import java.util.Optional

@ExtendWith(MockKExtension::class)
@DisplayName("UserSkillService Unit Tests")
class UserSkillServiceTest {
    @MockK
    private lateinit var skillRepo: SkillRepository

    @MockK
    private lateinit var userSkillRepo: UserSkillRepository

    private lateinit var userSkillService: UserSkillService

    private val role = RoleModel("EMPLOYER", null)

    private val user =
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
        userSkillService = UserSkillService(skillRepo, userSkillRepo)
    }

    @Test
    fun `addOrUpdateSkill creates new skill and user skill`() {
        // given
        val skill = SkillModel(name = "kotlin")
        every { skillRepo.findByNameIgnoreCase("kotlin") } returns null
        every { skillRepo.save(any()) } returns skill
        every { userSkillRepo.findByUserAndSkillId(user, skill.id) } returns null
        every { userSkillRepo.save(any()) } returnsArgument 0

        // when
        val (dto, created) = userSkillService.addOrUpdateSkill(user, "Kotlin", 3)

        // then
        assertThat(created).isTrue()
        assertThat(dto.name).isEqualTo("kotlin")
        assertThat(dto.level).isEqualTo(3)
        verify(exactly = 1) { skillRepo.save(any()) }
        verify(exactly = 1) { userSkillRepo.save(any()) }
    }

    @Test
    fun `addOrUpdateSkill reuses existing skill`() {
        // given
        val skill = SkillModel(name = "java")
        every { skillRepo.findByNameIgnoreCase("java") } returns skill
        every { userSkillRepo.findByUserAndSkillId(user, skill.id) } returns null
        every { userSkillRepo.save(any()) } returnsArgument 0

        // when
        val (dto, created) = userSkillService.addOrUpdateSkill(user, "Java", 4)

        // then
        assertThat(created).isTrue()
        assertThat(dto.name).isEqualTo("java")
        assertThat(dto.level).isEqualTo(4)
        verify(exactly = 0) { skillRepo.save(any()) }
    }

    @Test
    fun `addOrUpdateSkill updates level when user already has the skill`() {
        // given
        val skill = SkillModel(name = "kotlin")
        val existingUserSkill = UserSkillModel(user = user, skill = skill, level = 2)
        every { skillRepo.findByNameIgnoreCase("kotlin") } returns skill
        every { userSkillRepo.findByUserAndSkillId(user, skill.id) } returns existingUserSkill
        every { userSkillRepo.save(any()) } returnsArgument 0

        // when
        val (dto, created) = userSkillService.addOrUpdateSkill(user, "Kotlin", 5)

        // then
        assertThat(created).isFalse()
        assertThat(dto.level).isEqualTo(5)
        verify(exactly = 0) { skillRepo.save(any()) }
    }

    @Test
    fun `addOrUpdateSkill trims and lowercases skill name`() {
        // given
        val skill = SkillModel(name = "spring boot")
        every { skillRepo.findByNameIgnoreCase("spring boot") } returns skill
        every { userSkillRepo.findByUserAndSkillId(user, skill.id) } returns null
        every { userSkillRepo.save(any()) } returnsArgument 0

        // when
        val (dto, _) = userSkillService.addOrUpdateSkill(user, "  Spring Boot  ", 3)

        // then
        assertThat(dto.name).isEqualTo("spring boot")
        verify { skillRepo.findByNameIgnoreCase("spring boot") }
    }

    @Test
    fun `addOrUpdateSkill throws when level is below 1`() {
        // then
        assertThatThrownBy {
            userSkillService.addOrUpdateSkill(user, "Kotlin", 0)
        }.isInstanceOf(IllegalArgumentException::class.java)

        verify(exactly = 0) { userSkillRepo.save(any()) }
    }

    @Test
    fun `addOrUpdateSkill throws when level is above 5`() {
        // then
        assertThatThrownBy {
            userSkillService.addOrUpdateSkill(user, "Kotlin", 6)
        }.isInstanceOf(IllegalArgumentException::class.java)

        verify(exactly = 0) { userSkillRepo.save(any()) }
    }

    @Test
    fun `getAllSkills returns all skills as DTOs`() {
        // given
        val skills =
            listOf(
                SkillModel(name = "kotlin"),
                SkillModel(name = "java"),
                SkillModel(name = "spring"),
            )
        every { skillRepo.findAll() } returns skills

        // when
        val result = userSkillService.getAllSkills()

        // then
        assertThat(result).hasSize(3)
        assertThat(result[0].name).isEqualTo("kotlin")
        assertThat(result[1].name).isEqualTo("java")
        assertThat(result[2].name).isEqualTo("spring")
    }

    @Test
    fun `getAllSkills returns empty list when no skills exist`() {
        // given
        every { skillRepo.findAll() } returns emptyList()

        // when
        val result = userSkillService.getAllSkills()

        // then
        assertThat(result).isEmpty()
    }

    @Test
    fun `getUserSkills returns mapped DTOs`() {
        // given
        val skill1 = SkillModel(name = "kotlin")
        val skill2 = SkillModel(name = "java")
        val userSkills =
            listOf(
                UserSkillModel(user = user, skill = skill1, level = 4),
                UserSkillModel(user = user, skill = skill2, level = 3),
            )
        every { userSkillRepo.findByUser(user) } returns userSkills

        // when
        val result = userSkillService.getUserSkills(user)

        // then
        assertThat(result).hasSize(2)
        assertThat(result[0].name).isEqualTo("kotlin")
        assertThat(result[0].level).isEqualTo(4)
        assertThat(result[1].name).isEqualTo("java")
        assertThat(result[1].level).isEqualTo(3)
    }

    @Test
    fun `getUserSkills returns empty list when user has no skills`() {
        // given
        every { userSkillRepo.findByUser(user) } returns emptyList()

        // when
        val result = userSkillService.getUserSkills(user)

        // then
        assertThat(result).isEmpty()
    }

    @Test
    fun `deleteSkill deletes user skill successfully`() {
        // given
        val skill = SkillModel(name = "kotlin")
        val userSkill = UserSkillModel(user = user, skill = skill, level = 3)
        every { userSkillRepo.findById(userSkill.id) } returns Optional.of(userSkill)
        every { userSkillRepo.delete(userSkill) } returns Unit

        // when
        userSkillService.deleteSkill(user, userSkill.id)

        // then
        verify(exactly = 1) { userSkillRepo.delete(userSkill) }
    }

    @Test
    fun `deleteSkill throws EntryNotFoundException when skill not found`() {
        // given
        every { userSkillRepo.findById("nonexistent") } returns Optional.empty()

        // then
        assertThatThrownBy {
            userSkillService.deleteSkill(user, "nonexistent")
        }.isInstanceOf(EntryNotFoundException::class.java)
            .satisfies({ ex ->
                val e = ex as EntryNotFoundException
                assertThat(e.errorCode).isEqualTo(GlobalErrorCode.NOT_FOUND)
            })

        verify(exactly = 0) { userSkillRepo.delete(any()) }
    }

    @Test
    fun `deleteSkill throws AccessDeniedException when user tries to delete another users skill`() {
        // given
        val skill = SkillModel(name = "kotlin")
        val otherUsersSkill = UserSkillModel(user = otherUser, skill = skill, level = 3)
        every { userSkillRepo.findById(otherUsersSkill.id) } returns Optional.of(otherUsersSkill)

        // then
        assertThatThrownBy {
            userSkillService.deleteSkill(user, otherUsersSkill.id)
        }.isInstanceOf(AccessDeniedException::class.java)

        verify(exactly = 0) { userSkillRepo.delete(any()) }
    }
}
