package org.efehan.skillmatcherbackend.fixtures.requests

import org.efehan.skillmatcherbackend.core.projectskill.AddProjectSkillRequest

object ProjectSkillFixtures {
    fun buildAddProjectSkillRequest(
        name: String = "Kotlin",
        level: Int = 3,
        priority: String = "MUST_HAVE",
    ): AddProjectSkillRequest =
        AddProjectSkillRequest(
            name = name,
            level = level,
            priority = priority,
        )
}
