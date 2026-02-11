package org.efehan.skillmatcherbackend.core.project

import io.swagger.v3.oas.annotations.media.Schema
import jakarta.validation.constraints.Min
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.NotNull
import jakarta.validation.constraints.Pattern
import java.time.Instant
import java.time.LocalDate

data class CreateProjectRequest(
    @field:NotBlank
    @Schema(example = "Skill Matcher")
    val name: String,
    @field:NotBlank
    @Schema(example = "Internal tool to match employees to projects based on skills.")
    val description: String,
    @field:NotNull
    @Schema(example = "2026-03-01")
    val startDate: LocalDate,
    @field:NotNull
    @Schema(example = "2026-09-01")
    val endDate: LocalDate,
    @field:Min(1)
    @Schema(example = "5")
    val maxMembers: Int,
)

data class UpdateProjectRequest(
    @field:NotBlank
    @Schema(example = "Skill Matcher v2")
    val name: String,
    @field:NotBlank
    @Schema(example = "Updated description for the project.")
    val description: String,
    @field:NotNull
    @field:Pattern(regexp = "PLANNED|ACTIVE|PAUSED|COMPLETED", message = "Status must be one of: PLANNED, ACTIVE, PAUSED, COMPLETED")
    @Schema(example = "ACTIVE")
    val status: String,
    @field:NotNull
    @Schema(example = "2026-03-01")
    val startDate: LocalDate,
    @field:NotNull
    @Schema(example = "2026-12-01")
    val endDate: LocalDate,
    @field:Min(1)
    @Schema(example = "8")
    val maxMembers: Int,
)

data class ProjectDto(
    @Schema(example = "550e8400-e29b-41d4-a716-446655440000")
    val id: String,
    @Schema(example = "Skill Matcher")
    val name: String,
    @Schema(example = "Internal tool to match employees to projects based on skills.")
    val description: String,
    @Schema(example = "PLANNED")
    val status: String,
    @Schema(example = "2026-03-01")
    val startDate: LocalDate,
    @Schema(example = "2026-09-01")
    val endDate: LocalDate,
    @Schema(example = "5")
    val maxMembers: Int,
    @Schema(example = "Max Mustermann")
    val ownerName: String,
    @Schema(example = "2026-02-10T12:00:00Z")
    val createdDate: Instant?,
)
