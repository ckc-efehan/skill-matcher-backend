package org.efehan.skillmatcherbackend.core.availability

import io.swagger.v3.oas.annotations.media.Schema
import jakarta.validation.constraints.NotNull
import java.time.LocalDate

data class CreateAvailabilityRequest(
    @field:NotNull
    @Schema(example = "2026-03-01")
    var availableFrom: LocalDate,
    @field:NotNull
    @Schema(example = "2026-09-01")
    var availableTo: LocalDate,
)

data class UpdateAvailabilityRequest(
    @field:NotNull
    @Schema(example = "2026-04-01")
    var availableFrom: LocalDate,
    @field:NotNull
    @Schema(example = "2026-10-01")
    var availableTo: LocalDate,
)

data class UserAvailabilityDto(
    @Schema(example = "550e8400-e29b-41d4-a716-446655440000")
    val id: String,
    @Schema(example = "2026-03-01")
    val availableFrom: LocalDate,
    @Schema(example = "2026-09-01")
    val availableTo: LocalDate,
)
