package org.efehan.skillmatcherbackend.core.projectmember

import io.swagger.v3.oas.annotations.media.Schema
import jakarta.validation.constraints.NotBlank
import java.time.Instant

data class AddProjectMemberRequest(
    @field:NotBlank
    @Schema(example = "550e8400-e29b-41d4-a716-446655440000")
    val userId: String,
)

data class ProjectMemberDto(
    @Schema(example = "550e8400-e29b-41d4-a716-446655440000")
    val id: String,
    @Schema(example = "550e8400-e29b-41d4-a716-446655440001")
    val userId: String,
    @Schema(example = "Max Mustermann")
    val userName: String,
    @Schema(example = "max@firma.de")
    val email: String,
    @Schema(example = "ACTIVE")
    val status: String,
    @Schema(example = "2026-03-01T10:00:00Z")
    val joinedDate: Instant,
)
