package org.efehan.skillmatcherbackend.fixtures.builder

import org.efehan.skillmatcherbackend.persistence.SkillModel
import org.efehan.skillmatcherbackend.persistence.UserModel
import org.efehan.skillmatcherbackend.persistence.UserSkillModel

class UserSkillBuilder {
    fun build(
        user: UserModel = UserBuilder().build(),
        skill: SkillModel = SkillBuilder().build(),
        level: Int = 3,
    ): UserSkillModel =
        UserSkillModel(
            user = user,
            skill = skill,
            level = level,
        )
}
