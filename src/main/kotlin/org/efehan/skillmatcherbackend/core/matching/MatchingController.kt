package org.efehan.skillmatcherbackend.core.matching

import io.swagger.v3.oas.annotations.tags.Tag
import org.efehan.skillmatcherbackend.core.auth.SecurityUser
import org.springframework.http.ResponseEntity
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/matching")
@Tag(name = "Matching", description = "Skill-based matching between users and projects")
class MatchingController(
    private val matchingService: MatchingService,
) {
    @GetMapping("/projects/{projectId}/candidates")
    @PreAuthorize("hasRole('PROJECTMANAGER')")
    fun findCandidates(
        @PathVariable
        projectId: String,
        @RequestParam(defaultValue = "0.0")
        minScore: Double,
        @RequestParam(defaultValue = "20")
        limit: Int,
    ): ResponseEntity<List<UserMatchDto>> =
        ResponseEntity.ok(matchingService.findCandidatesForProject(projectId, minScore, limit))

    @GetMapping("/me/projects")
    fun findProjectsForMe(
        @AuthenticationPrincipal
        securityUser: SecurityUser,
        @RequestParam(defaultValue = "0.0")
        minScore: Double,
        @RequestParam(defaultValue = "20")
        limit: Int,
    ): ResponseEntity<List<ProjectMatchDto>> =
        ResponseEntity.ok(matchingService.findProjectsForUser(securityUser.user, minScore, limit))
}