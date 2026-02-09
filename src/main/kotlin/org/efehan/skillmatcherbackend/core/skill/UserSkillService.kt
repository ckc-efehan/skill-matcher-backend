package org.efehan.skillmatcherbackend.core.skill

import org.efehan.skillmatcherbackend.persistence.SkillModel
import org.efehan.skillmatcherbackend.persistence.SkillRepository
import org.efehan.skillmatcherbackend.persistence.UserModel
import org.efehan.skillmatcherbackend.persistence.UserSkillModel
import org.efehan.skillmatcherbackend.persistence.UserSkillRepository
import org.efehan.skillmatcherbackend.shared.exceptions.EntryNotFoundException
import org.springframework.security.access.AccessDeniedException
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
@Transactional
class UserSkillService(
    private val skillRepo: SkillRepository,
    private val userSkillRepo: UserSkillRepository,
) {
    fun addOrUpdateSkill(
        user: UserModel,
        name: String,
        level: Int,
    ): Pair<UserSkillDto, Boolean> {
        require(level in 1..5) { "Level must be between 1 and 5" }

        val skill = getOrCreateSkill(name)
        val existing = userSkillRepo.findByUserAndSkillId(user, skill.id)

        val created = existing == null
        val userSkill =
            if (existing != null) {
                existing.level = level
                userSkillRepo.save(existing)
            } else {
                userSkillRepo.save(UserSkillModel(user = user, skill = skill, level = level))
            }

        return userSkill.toDto() to created
    }

    fun getUserSkills(user: UserModel): List<UserSkillDto> = userSkillRepo.findByUser(user).map { it.toDto() }

    fun deleteSkill(
        user: UserModel,
        userSkillId: String,
    ) {
        val userSkill =
            userSkillRepo
                .findById(userSkillId)
                .orElseThrow { EntryNotFoundException(resource = "UserSkill", field = "id", value = userSkillId) }

        if (userSkill.user.id != user.id) {
            throw AccessDeniedException("Not allowed to delete this skill")
        }

        userSkillRepo.delete(userSkill)
    }

    private fun getOrCreateSkill(name: String): SkillModel {
        val normalized = name.trim().lowercase()
        return skillRepo.findByNameIgnoreCase(normalized)
            ?: skillRepo.save(SkillModel(name = normalized))
    }

    private fun UserSkillModel.toDto() =
        UserSkillDto(
            id = id,
            name = skill.name,
            level = level,
        )
}
