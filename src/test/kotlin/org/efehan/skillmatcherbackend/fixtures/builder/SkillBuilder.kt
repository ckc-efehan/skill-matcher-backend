package org.efehan.skillmatcherbackend.fixtures.builder

import org.efehan.skillmatcherbackend.persistence.SkillModel

class SkillBuilder {
    fun build(name: String = "Kotlin"): SkillModel =
        SkillModel(
            name = name,
        )
}
