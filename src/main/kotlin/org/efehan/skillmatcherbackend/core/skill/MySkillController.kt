package org.efehan.skillmatcherbackend.core.skill

import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.media.Content
import io.swagger.v3.oas.annotations.media.Schema
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.responses.ApiResponses
import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.validation.Valid
import org.efehan.skillmatcherbackend.core.auth.SecurityUser
import org.efehan.skillmatcherbackend.exception.GlobalErrorCodeResponse
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/me/skills")
@Tag(name = "My Skills", description = "Manage own skills")
class MySkillController(
    private val service: UserSkillService,
) {
    @Operation(
        summary = "Add or update a skill",
        description = "Adds a new skill or updates the level if the skill already exists.",
    )
    @ApiResponses(
        value = [
            ApiResponse(
                responseCode = "201",
                description = "Skill created.",
                content = [Content(mediaType = "application/json", schema = Schema(implementation = UserSkillDto::class))],
            ),
            ApiResponse(
                responseCode = "200",
                description = "Skill level updated.",
                content = [Content(mediaType = "application/json", schema = Schema(implementation = UserSkillDto::class))],
            ),
            ApiResponse(
                responseCode = "400",
                description = "Validation error.",
                content = [Content(mediaType = "application/json", schema = Schema(implementation = GlobalErrorCodeResponse::class))],
            ),
            ApiResponse(
                responseCode = "401",
                description = "Not authenticated.",
                content = [Content(mediaType = "application/json", schema = Schema(implementation = GlobalErrorCodeResponse::class))],
            ),
        ],
    )
    @PostMapping
    fun addSkill(
        @AuthenticationPrincipal securityUser: SecurityUser,
        @Valid @RequestBody req: AddSkillRequest,
    ): ResponseEntity<UserSkillDto> {
        val (dto, created) = service.addOrUpdateSkill(securityUser.user, req.name, req.level)
        val status = if (created) HttpStatus.CREATED else HttpStatus.OK
        return ResponseEntity.status(status).body(dto)
    }

    @Operation(
        summary = "Get my skills",
        description = "Returns all skills of the authenticated user.",
    )
    @ApiResponses(
        value = [
            ApiResponse(
                responseCode = "200",
                description = "Skills retrieved successfully.",
            ),
            ApiResponse(
                responseCode = "401",
                description = "Not authenticated.",
                content = [Content(mediaType = "application/json", schema = Schema(implementation = GlobalErrorCodeResponse::class))],
            ),
        ],
    )
    @GetMapping
    fun getMySkills(
        @AuthenticationPrincipal securityUser: SecurityUser,
    ): ResponseEntity<List<UserSkillDto>> = ResponseEntity.ok(service.getUserSkills(securityUser.user))

    @Operation(
        summary = "Delete a skill",
        description = "Deletes a skill from the authenticated user's profile.",
    )
    @ApiResponses(
        value = [
            ApiResponse(
                responseCode = "204",
                description = "Skill deleted.",
                content = [Content()],
            ),
            ApiResponse(
                responseCode = "401",
                description = "Not authenticated.",
                content = [Content(mediaType = "application/json", schema = Schema(implementation = GlobalErrorCodeResponse::class))],
            ),
            ApiResponse(
                responseCode = "403",
                description = "Not allowed to delete this skill.",
                content = [Content(mediaType = "application/json", schema = Schema(implementation = GlobalErrorCodeResponse::class))],
            ),
            ApiResponse(
                responseCode = "404",
                description = "Skill not found.",
                content = [Content(mediaType = "application/json", schema = Schema(implementation = GlobalErrorCodeResponse::class))],
            ),
        ],
    )
    @DeleteMapping("/{id}")
    fun delete(
        @AuthenticationPrincipal securityUser: SecurityUser,
        @PathVariable id: String,
    ): ResponseEntity<Void> {
        service.deleteSkill(securityUser.user, id)
        return ResponseEntity.noContent().build()
    }
}
