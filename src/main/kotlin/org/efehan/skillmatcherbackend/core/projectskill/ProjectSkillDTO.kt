package org.efehan.skillmatcherbackend.core.projectskill

import io.swagger.v3.oas.annotations.media.Schema
import jakarta.validation.constraints.Max
import jakarta.validation.constraints.Min
import jakarta.validation.constraints.NotBlank

data class AddProjectSkillRequest(
    @field:NotBlank
    @Schema(example = "Kotlin")
    val name: String,
    @field:Min(1)
    @field:Max(5)
    @Schema(example = "4")
    val level: Int,
)

data class ProjectSkillDto(
    @Schema(example = "550e8400-e29b-41d4-a716-446655440000")
    val id: String,
    @Schema(example = "kotlin")
    val name: String,
    @Schema(example = "4")
    val level: Int,
)
