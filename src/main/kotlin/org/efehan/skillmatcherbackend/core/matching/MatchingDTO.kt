package org.efehan.skillmatcherbackend.core.matching

import io.swagger.v3.oas.annotations.media.Schema

data class MatchScoreBreakdown(
    @Schema(example = "1.0", description = "Anteil der erfüllten MUST_HAVE Skills (0.0–1.0)")
    val mustHaveCoverage: Double,
    @Schema(example = "0.92", description = "Wie gut die Skill-Level passen (0.0–1.0)")
    val levelFitScore: Double,
    @Schema(example = "0.5", description = "Anteil der vorhandenen NICE_TO_HAVE Skills (0.0–1.0)")
    val niceToHaveCoverage: Double,
    @Schema(example = "1.0", description = "Zeitliche Verfügbarkeit im Projektzeitraum (0.0–1.0)")
    val availabilityScore: Double,
)

data class MatchedSkillDto(
    @Schema(example = "550e8400-e29b-41d4-a716-446655440000")
    val skillId: String,
    @Schema(example = "kotlin")
    val skillName: String,
    @Schema(example = "4")
    val userLevel: Int,
    @Schema(example = "3")
    val requiredLevel: Int,
    @Schema(example = "MUST_HAVE")
    val priority: String,
)

data class MissingSkillDto(
    @Schema(example = "550e8400-e29b-41d4-a716-446655440001")
    val skillId: String,
    @Schema(example = "docker")
    val skillName: String,
    @Schema(example = "2")
    val requiredLevel: Int,
    @Schema(example = "NICE_TO_HAVE")
    val priority: String,
)

data class UserMatchDto(
    val userId: String,
    @Schema(example = "Max Mustermann")
    val userName: String,
    @Schema(example = "max@firma.de")
    val email: String,
    @Schema(example = "0.87")
    val score: Double,
    val breakdown: MatchScoreBreakdown,
    val matchedSkills: List<MatchedSkillDto>,
    val missingSkills: List<MissingSkillDto>,
)

data class ProjectMatchDto(
    val projectId: String,
    @Schema(example = "Skill Matcher")
    val projectName: String,
    @Schema(example = "Internal tool to match employees to projects based on skills.")
    val projectDescription: String,
    @Schema(example = "PLANNED")
    val status: String,
    @Schema(example = "Max Mustermann")
    val ownerName: String,
    @Schema(example = "0.87")
    val score: Double,
    val breakdown: MatchScoreBreakdown,
    val matchedSkills: List<MatchedSkillDto>,
    val missingSkills: List<MissingSkillDto>,
)
