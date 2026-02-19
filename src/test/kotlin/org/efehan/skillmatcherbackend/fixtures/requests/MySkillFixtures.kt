package org.efehan.skillmatcherbackend.fixtures.requests

import org.efehan.skillmatcherbackend.core.skill.AddSkillRequest

object MySkillFixtures {
    fun buildAddSkillRequest(
        name: String = "Kotlin",
        level: Int = 4,
    ): AddSkillRequest =
        AddSkillRequest(
            name = name,
            level = level,
        )
}
