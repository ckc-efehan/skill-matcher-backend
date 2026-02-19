package org.efehan.skillmatcherbackend.fixtures.requests

import org.efehan.skillmatcherbackend.core.project.CreateProjectRequest
import org.efehan.skillmatcherbackend.core.project.UpdateProjectRequest
import java.time.LocalDate

object ProjectFixtures {
    fun buildCreateProjectRequest(
        name: String = "Skill Matcher",
        description: String = "Internal tool",
        startDate: LocalDate = LocalDate.of(2026, 3, 1),
        endDate: LocalDate = LocalDate.of(2026, 9, 1),
        maxMembers: Int = 5,
    ): CreateProjectRequest =
        CreateProjectRequest(
            name = name,
            description = description,
            startDate = startDate,
            endDate = endDate,
            maxMembers = maxMembers,
        )

    fun buildUpdateProjectRequest(
        name: String = "Updated Skill Matcher",
        description: String = "Updated description",
        status: String = "ACTIVE",
        startDate: LocalDate = LocalDate.of(2026, 4, 1),
        endDate: LocalDate = LocalDate.of(2026, 12, 1),
        maxMembers: Int = 8,
    ): UpdateProjectRequest =
        UpdateProjectRequest(
            name = name,
            description = description,
            status = status,
            startDate = startDate,
            endDate = endDate,
            maxMembers = maxMembers,
        )
}
