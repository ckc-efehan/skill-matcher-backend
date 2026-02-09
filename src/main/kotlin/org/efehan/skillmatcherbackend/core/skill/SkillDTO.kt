package org.efehan.skillmatcherbackend.core.skill

import jakarta.validation.constraints.Max
import jakarta.validation.constraints.Min
import jakarta.validation.constraints.NotBlank

data class AddSkillRequest(
    @field:NotBlank
    val name: String,
    @field:Min(1)
    @field:Max(5)
    val level: Int,
)

data class UserSkillDto(
    val id: String,
    val name: String,
    val level: Int,
)
