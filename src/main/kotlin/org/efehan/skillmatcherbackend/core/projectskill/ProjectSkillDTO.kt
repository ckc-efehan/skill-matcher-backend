package org.efehan.skillmatcherbackend.core.projectskill

import jakarta.validation.constraints.Max
import jakarta.validation.constraints.Min
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Pattern

data class AddProjectSkillRequest(
    @field:NotBlank
    val name: String,
    @field:Min(1)
    @field:Max(5)
    val level: Int,
    @field:Pattern(
        regexp = "(?i)MUST_HAVE|NICE_TO_HAVE",
        message = "Priority must be MUST_HAVE or NICE_TO_HAVE",
    )
    val priority: String = "MUST_HAVE",
)

data class ProjectSkillDto(
    val id: String,
    val name: String,
    val level: Int,
    val priority: String,
)
