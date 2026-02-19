package org.efehan.skillmatcherbackend.fixtures.builder

import org.efehan.skillmatcherbackend.persistence.ProjectModel
import org.efehan.skillmatcherbackend.persistence.ProjectSkillModel
import org.efehan.skillmatcherbackend.persistence.SkillModel
import org.efehan.skillmatcherbackend.persistence.SkillPriority

class ProjectSkillBuilder {
    fun build(
        project: ProjectModel = ProjectBuilder().build(),
        skill: SkillModel = SkillBuilder().build(),
        level: Int = 3,
        priority: SkillPriority = SkillPriority.MUST_HAVE,
    ): ProjectSkillModel =
        ProjectSkillModel(
            project = project,
            skill = skill,
            level = level,
            priority = priority,
        )
}
