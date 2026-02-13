package org.efehan.skillmatcherbackend.core.matching

import org.efehan.skillmatcherbackend.exception.GlobalErrorCode
import org.efehan.skillmatcherbackend.persistence.ProjectMemberStatus
import org.efehan.skillmatcherbackend.persistence.ProjectModel
import org.efehan.skillmatcherbackend.persistence.ProjectRepository
import org.efehan.skillmatcherbackend.persistence.ProjectSkillModel
import org.efehan.skillmatcherbackend.persistence.ProjectSkillRepository
import org.efehan.skillmatcherbackend.persistence.ProjectStatus
import org.efehan.skillmatcherbackend.persistence.SkillPriority
import org.efehan.skillmatcherbackend.persistence.UserAvailabilityModel
import org.efehan.skillmatcherbackend.persistence.UserAvailabilityRepository
import org.efehan.skillmatcherbackend.persistence.UserModel
import org.efehan.skillmatcherbackend.persistence.UserSkillModel
import org.efehan.skillmatcherbackend.persistence.UserSkillRepository
import org.efehan.skillmatcherbackend.shared.exceptions.EntryNotFoundException
import org.springframework.http.HttpStatus
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.temporal.ChronoUnit
import kotlin.math.min
import kotlin.math.roundToInt

@Service
@Transactional(readOnly = true)
class MatchingService(
    private val projectRepo: ProjectRepository,
    private val projectSkillRepo: ProjectSkillRepository,
    private val userSkillRepo: UserSkillRepository,
    private val availabilityRepo: UserAvailabilityRepository,
) {
    companion object {
        const val WEIGHT_MUST_HAVE = 0.40
        const val WEIGHT_LEVEL_FIT = 0.25
        const val WEIGHT_NICE_TO_HAVE = 0.15
        const val WEIGHT_AVAILABILITY = 0.20
        const val LEVEL_OVERFIT_CAP = 1.2
    }

    fun findCandidatesForProject(
        projectId: String,
        minScore: Double,
        limit: Int,
    ): List<UserMatchDto> {
        val project = findProjectOrThrow(projectId)
        val projectSkills = projectSkillRepo.findByProject(project)

        if (projectSkills.isEmpty()) return emptyList()

        // alle user laden die mind. einen projekt skill haben
        val relevantSkills = projectSkills.map { it.skill }
        val allUserSkills =
            userSkillRepo.findMatchableBySkillsForProject(
                skills = relevantSkills,
                project = project,
                activeStatus = ProjectMemberStatus.ACTIVE,
            )

        val userSkillMap = allUserSkills.groupBy { it.user.id }
        if (userSkillMap.isEmpty()) return emptyList()

        // Batch-Query: alle Availabilities für Kandidaten in einem Aufruf laden
        val candidateUsers = userSkillMap.values.map { it.first().user }
        val availabilityMap =
            availabilityRepo
                .findByUserIn(candidateUsers)
                .groupBy { it.user.id }

        return userSkillMap
            .map { (userId, skills) ->
                val user = skills.first().user
                val userAvailabilities = availabilityMap[userId].orEmpty()
                computeUserMatch(user, skills, projectSkills, project, userAvailabilities)
            }.filter { it.score >= minScore }
            .sortedByDescending { it.score }
            .take(limit)
    }

    fun findProjectsForUser(
        user: UserModel,
        minScore: Double,
        limit: Int,
    ): List<ProjectMatchDto> {
        val userSkills = userSkillRepo.findByUser(user)
        if (userSkills.isEmpty()) return emptyList()

        // Nur relevante Projekte direkt aus der DB laden
        val projects =
            projectRepo.findMatchableForUser(
                user = user,
                statuses = listOf(ProjectStatus.PLANNED, ProjectStatus.ACTIVE),
                activeStatus = ProjectMemberStatus.ACTIVE,
            )

        if (projects.isEmpty()) return emptyList()

        // batch: alle ProjectSkills für relevante Projekte in einem Aufruf
        val projectSkillMap =
            projectSkillRepo
                .findByProjectIn(projects)
                .groupBy { it.project.id }

        // Availability einmal laden (selber User für alle Projekte)
        val userAvailabilities = availabilityRepo.findByUser(user)

        return projects
            .asSequence()
            .mapNotNull { project ->
                val projectSkills = projectSkillMap[project.id].orEmpty()
                if (projectSkills.isEmpty()) return@mapNotNull null
                computeProjectMatch(user, userSkills, project, projectSkills, userAvailabilities)
            }.filter { it.score >= minScore }
            .sortedByDescending { it.score }
            .take(limit)
            .toList()
    }

    private fun computeUserMatch(
        user: UserModel,
        userSkills: List<UserSkillModel>,
        projectSkills: List<ProjectSkillModel>,
        project: ProjectModel,
        availabilities: List<UserAvailabilityModel>,
    ): UserMatchDto {
        val result = computeScore(userSkills, projectSkills, project, availabilities)

        return UserMatchDto(
            userId = user.id,
            userName = "${user.firstName} ${user.lastName}",
            email = user.email,
            score = result.score,
            breakdown = result.breakdown,
            matchedSkills = result.matchedSkills,
            missingSkills = result.missingSkills,
        )
    }

    private fun computeProjectMatch(
        user: UserModel,
        userSkills: List<UserSkillModel>,
        project: ProjectModel,
        projectSkills: List<ProjectSkillModel>,
        availabilities: List<UserAvailabilityModel>,
    ): ProjectMatchDto {
        val result = computeScore(userSkills, projectSkills, project, availabilities)

        return ProjectMatchDto(
            projectId = project.id,
            projectName = project.name,
            projectDescription = project.description,
            status = project.status.name,
            ownerName = "${project.owner.firstName} ${project.owner.lastName}",
            score = result.score,
            breakdown = result.breakdown,
            matchedSkills = result.matchedSkills,
            missingSkills = result.missingSkills,
        )
    }

    private data class ScoreResult(
        val score: Double,
        val breakdown: MatchScoreBreakdown,
        val matchedSkills: List<MatchedSkillDto>,
        val missingSkills: List<MissingSkillDto>,
    )

    private fun computeScore(
        userSkills: List<UserSkillModel>,
        projectSkills: List<ProjectSkillModel>,
        project: ProjectModel,
        availabilities: List<UserAvailabilityModel>,
    ): ScoreResult {
        val userSkillMap = userSkills.associateBy { it.skill.id }

        val mustHaveSkills = projectSkills.filter { it.priority == SkillPriority.MUST_HAVE }
        val niceToHaveSkills = projectSkills.filter { it.priority == SkillPriority.NICE_TO_HAVE }

        val matchedSkills = mutableListOf<MatchedSkillDto>()
        val missingSkills = mutableListOf<MissingSkillDto>()

        // must have coverage
        var mustHaveFulfilled = 0
        mustHaveSkills.forEach { ps ->
            val us = userSkillMap[ps.skill.id]
            if (us != null && us.level >= ps.level) {
                mustHaveFulfilled++
                matchedSkills.add(
                    MatchedSkillDto(
                        skillId = ps.skill.id,
                        skillName = ps.skill.name,
                        userLevel = us.level,
                        requiredLevel = ps.level,
                        priority = ps.priority.name,
                    ),
                )
            } else if (us != null) {
                // user hat skill aber nicht level
                matchedSkills.add(
                    MatchedSkillDto(
                        skillId = ps.skill.id,
                        skillName = ps.skill.name,
                        userLevel = us.level,
                        requiredLevel = ps.level,
                        priority = ps.priority.name,
                    ),
                )
            } else {
                missingSkills.add(
                    MissingSkillDto(
                        skillId = ps.skill.id,
                        skillName = ps.skill.name,
                        requiredLevel = ps.level,
                        priority = ps.priority.name,
                    ),
                )
            }
        }
        val mustHaveCoverage =
            if (mustHaveSkills.isEmpty()) 1.0 else mustHaveFulfilled.toDouble() / mustHaveSkills.size

        // nice to have coverage
        var niceToHaveFulfilled = 0
        niceToHaveSkills.forEach { ps ->
            val us = userSkillMap[ps.skill.id]
            if (us != null) {
                niceToHaveFulfilled++
                matchedSkills.add(
                    MatchedSkillDto(
                        skillId = ps.skill.id,
                        skillName = ps.skill.name,
                        userLevel = us.level,
                        requiredLevel = ps.level,
                        priority = ps.priority.name,
                    ),
                )
            } else {
                missingSkills.add(
                    MissingSkillDto(
                        skillId = ps.skill.id,
                        skillName = ps.skill.name,
                        requiredLevel = ps.level,
                        priority = ps.priority.name,
                    ),
                )
            }
        }
        val niceToHaveCoverage =
            if (niceToHaveSkills.isEmpty()) 1.0 else niceToHaveFulfilled.toDouble() / niceToHaveSkills.size

        // level fit score
        // berechnet über alle gematchten skill

        val allMatchedWithUserLevel =
            projectSkills.mapNotNull { ps ->
                val us = userSkillMap[ps.skill.id]
                if (us != null) us.level to ps.level else null
            }
        val levelFitScore =
            if (allMatchedWithUserLevel.isEmpty()) {
                0.0
            } else {
                allMatchedWithUserLevel.sumOf { (userLevel, reqLevel) ->
                    min(userLevel.toDouble() / reqLevel.toDouble(), LEVEL_OVERFIT_CAP)
                } / allMatchedWithUserLevel.size / LEVEL_OVERFIT_CAP
            }

        val availabilityScore = computeAvailabilityScore(availabilities, project)

        val rawScore =
            WEIGHT_MUST_HAVE * mustHaveCoverage +
                WEIGHT_LEVEL_FIT * levelFitScore +
                WEIGHT_NICE_TO_HAVE * niceToHaveCoverage +
                WEIGHT_AVAILABILITY * availabilityScore

        val score = roundToTwoDecimals(rawScore)

        return ScoreResult(
            score = score,
            breakdown =
                MatchScoreBreakdown(
                    mustHaveCoverage = roundToTwoDecimals(mustHaveCoverage),
                    levelFitScore = roundToTwoDecimals(levelFitScore),
                    niceToHaveCoverage = roundToTwoDecimals(niceToHaveCoverage),
                    availabilityScore = roundToTwoDecimals(availabilityScore),
                ),
            matchedSkills = matchedSkills,
            missingSkills = missingSkills,
        )
    }

    private fun computeAvailabilityScore(
        availabilities: List<UserAvailabilityModel>,
        project: ProjectModel,
    ): Double {
        if (availabilities.isEmpty()) return 1.0

        val projectStart = project.startDate
        val projectEnd = project.endDate
        val projectDays = ChronoUnit.DAYS.between(projectStart, projectEnd)
        if (projectDays <= 0) return 1.0

        // annahme: availability einträge überlappen nicht (wird bei erstellung validiert)
        // berechne wie viele tage des projektzeitraums durch verfügbarkeit abgedeckt sind
        val coveredDays =
            availabilities.sumOf { avail ->
                val overlapStart = maxOf(projectStart, avail.availableFrom)
                val overlapEnd = minOf(projectEnd, avail.availableTo)
                val overlap = ChronoUnit.DAYS.between(overlapStart, overlapEnd)
                if (overlap > 0) overlap else 0L
            }

        return min(coveredDays.toDouble() / projectDays.toDouble(), 1.0)
    }

    private fun findProjectOrThrow(projectId: String): ProjectModel =
        projectRepo
            .findById(projectId)
            .orElseThrow {
                EntryNotFoundException(
                    resource = "Project",
                    field = "id",
                    value = projectId,
                    errorCode = GlobalErrorCode.PROJECT_NOT_FOUND,
                    status = HttpStatus.NOT_FOUND,
                )
            }

    private fun roundToTwoDecimals(value: Double): Double = (value * 100.0).roundToInt() / 100.0
}
