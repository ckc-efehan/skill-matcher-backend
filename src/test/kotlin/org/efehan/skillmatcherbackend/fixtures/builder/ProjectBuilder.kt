package org.efehan.skillmatcherbackend.fixtures.builder

import org.efehan.skillmatcherbackend.persistence.ProjectModel
import org.efehan.skillmatcherbackend.persistence.ProjectStatus
import org.efehan.skillmatcherbackend.persistence.UserModel
import java.time.LocalDate

class ProjectBuilder {
    fun build(
        name: String = "Skill Matcher",
        description: String = "Internal tool",
        status: ProjectStatus = ProjectStatus.PLANNED,
        startDate: LocalDate = LocalDate.of(2026, 3, 1),
        endDate: LocalDate = LocalDate.of(2026, 9, 1),
        maxMembers: Int = 5,
        owner: UserModel = UserBuilder().build(),
    ): ProjectModel =
        ProjectModel(
            name = name,
            description = description,
            status = status,
            startDate = startDate,
            endDate = endDate,
            maxMembers = maxMembers,
            owner = owner,
        )
}
