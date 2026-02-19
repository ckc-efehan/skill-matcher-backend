package org.efehan.skillmatcherbackend.core.projectskill

import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.media.Content
import io.swagger.v3.oas.annotations.media.ExampleObject
import io.swagger.v3.oas.annotations.media.Schema
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.responses.ApiResponses
import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.servlet.http.HttpServletResponse
import jakarta.validation.Valid
import org.efehan.skillmatcherbackend.core.auth.SecurityUser
import org.efehan.skillmatcherbackend.exception.GlobalErrorCodeResponse
import org.springframework.http.HttpStatus
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.ResponseStatus
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/projects/{projectId}/skills")
@Tag(name = "Project Skills", description = "Manage skills for projects")
class ProjectSkillController(
    private val service: ProjectSkillService,
) {
    @Operation(
        summary = "Add or update a project skill",
        description =
            "Adds a new skill to a project or updates the level and priority if the skill already exists. " +
                "Only the project owner can modify skills.",
    )
    @ApiResponses(
        value = [
            ApiResponse(
                responseCode = "201",
                description = "Skill created.",
                content = [
                    Content(
                        mediaType = "application/json",
                        schema = Schema(implementation = ProjectSkillDto::class),
                        examples = [
                            ExampleObject(
                                name = "Skill created",
                                value = """
                                {
                                    "id": "550e8400-e29b-41d4-a716-446655440000",
                                    "name": "kotlin",
                                    "level": 4,
                                    "priority": "MUST_HAVE"
                                }
                                """,
                            ),
                        ],
                    ),
                ],
            ),
            ApiResponse(
                responseCode = "200",
                description = "Skill level updated.",
                content = [
                    Content(
                        mediaType = "application/json",
                        schema = Schema(implementation = ProjectSkillDto::class),
                        examples = [
                            ExampleObject(
                                name = "Skill updated",
                                value = """
                                {
                                    "id": "550e8400-e29b-41d4-a716-446655440000",
                                    "name": "kotlin",
                                    "level": 5,
                                    "priority": "NICE_TO_HAVE"
                                }
                                """,
                            ),
                        ],
                    ),
                ],
            ),
            ApiResponse(
                responseCode = "400",
                description = "Validation error.",
                content = [
                    Content(
                        mediaType = "application/json",
                        schema = Schema(implementation = GlobalErrorCodeResponse::class),
                        examples = [
                            ExampleObject(
                                name = "Validation error",
                                value = """
                                {
                                    "errorCode": "VALIDATION_ERROR",
                                    "errorMessage": "Request validation failed.",
                                    "fieldErrors": [
                                        {
                                            "field": "level",
                                            "message": "must be greater than or equal to 1"
                                        }
                                    ]
                                }
                                """,
                            ),
                        ],
                    ),
                ],
            ),
            ApiResponse(
                responseCode = "401",
                description = "Not authenticated.",
                content = [
                    Content(
                        mediaType = "application/json",
                        schema = Schema(implementation = GlobalErrorCodeResponse::class),
                        examples = [
                            ExampleObject(
                                name = "Not authenticated",
                                value = """
                                {
                                    "errorCode": "UNAUTHORIZED",
                                    "errorMessage": "Not authenticated.",
                                    "fieldErrors": []
                                }
                                """,
                            ),
                        ],
                    ),
                ],
            ),
            ApiResponse(
                responseCode = "403",
                description = "Not the project owner.",
                content = [
                    Content(
                        mediaType = "application/json",
                        schema = Schema(implementation = GlobalErrorCodeResponse::class),
                        examples = [
                            ExampleObject(
                                name = "Forbidden",
                                value = """
                                {
                                    "errorCode": "PROJECT_ACCESS_DENIED",
                                    "errorMessage": "Not allowed to modify this project.",
                                    "fieldErrors": []
                                }
                                """,
                            ),
                        ],
                    ),
                ],
            ),
            ApiResponse(
                responseCode = "404",
                description = "Project not found.",
                content = [
                    Content(
                        mediaType = "application/json",
                        schema = Schema(implementation = GlobalErrorCodeResponse::class),
                        examples = [
                            ExampleObject(
                                name = "Project not found",
                                value = """
                                {
                                    "errorCode": "PROJECT_NOT_FOUND",
                                    "errorMessage": "Project with id 'some-id' could not be found.",
                                    "fieldErrors": []
                                }
                                """,
                            ),
                        ],
                    ),
                ],
            ),
        ],
    )
    @PostMapping
    @PreAuthorize("hasRole('PROJECTMANAGER')")
    fun addSkill(
        @AuthenticationPrincipal securityUser: SecurityUser,
        @PathVariable projectId: String,
        @Valid @RequestBody req: AddProjectSkillRequest,
        response: HttpServletResponse,
    ): ProjectSkillDto {
        val (model, created) = service.addOrUpdateSkill(securityUser.user, projectId, req.name, req.level, req.priority)
        response.status = if (created) HttpStatus.CREATED.value() else HttpStatus.OK.value()
        return model.toDTO()
    }

    @Operation(
        summary = "Get project skills",
        description = "Returns all skills of a project. Only the project owner can view skills.",
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
                content = [
                    Content(
                        mediaType = "application/json",
                        schema = Schema(implementation = GlobalErrorCodeResponse::class),
                        examples = [
                            ExampleObject(
                                name = "Not authenticated",
                                value = """
                                {
                                    "errorCode": "UNAUTHORIZED",
                                    "errorMessage": "Not authenticated.",
                                    "fieldErrors": []
                                }
                                """,
                            ),
                        ],
                    ),
                ],
            ),
            ApiResponse(
                responseCode = "403",
                description = "Not the project owner.",
                content = [
                    Content(
                        mediaType = "application/json",
                        schema = Schema(implementation = GlobalErrorCodeResponse::class),
                        examples = [
                            ExampleObject(
                                name = "Forbidden",
                                value = """
                                {
                                    "errorCode": "PROJECT_ACCESS_DENIED",
                                    "errorMessage": "Not allowed to modify Project.",
                                    "fieldErrors": []
                                }
                                """,
                            ),
                        ],
                    ),
                ],
            ),
            ApiResponse(
                responseCode = "404",
                description = "Project not found.",
                content = [
                    Content(
                        mediaType = "application/json",
                        schema = Schema(implementation = GlobalErrorCodeResponse::class),
                        examples = [
                            ExampleObject(
                                name = "Project not found",
                                value = """
                                {
                                    "errorCode": "PROJECT_NOT_FOUND",
                                    "errorMessage": "Project with id 'some-id' could not be found.",
                                    "fieldErrors": []
                                }
                                """,
                            ),
                        ],
                    ),
                ],
            ),
        ],
    )
    @GetMapping
    @PreAuthorize("hasRole('PROJECTMANAGER')")
    @ResponseStatus(HttpStatus.OK)
    fun getProjectSkills(
        @AuthenticationPrincipal securityUser: SecurityUser,
        @PathVariable projectId: String,
    ): List<ProjectSkillDto> = service.getProjectSkills(securityUser.user, projectId).map { it.toDTO() }

    @Operation(
        summary = "Delete a project skill",
        description = "Deletes a skill from a project. Only the project owner can delete skills.",
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
                content = [
                    Content(
                        mediaType = "application/json",
                        schema = Schema(implementation = GlobalErrorCodeResponse::class),
                        examples = [
                            ExampleObject(
                                name = "Not authenticated",
                                value = """
                                {
                                    "errorCode": "UNAUTHORIZED",
                                    "errorMessage": "Not authenticated.",
                                    "fieldErrors": []
                                }
                                """,
                            ),
                        ],
                    ),
                ],
            ),
            ApiResponse(
                responseCode = "403",
                description = "Not allowed to access the project or delete this skill.",
                content = [
                    Content(
                        mediaType = "application/json",
                        schema = Schema(implementation = GlobalErrorCodeResponse::class),
                        examples = [
                            ExampleObject(
                                name = "Project access denied",
                                value = """
                                {
                                    "errorCode": "PROJECT_ACCESS_DENIED",
                                    "errorMessage": "Not allowed to access Project.",
                                    "fieldErrors": []
                                }
                                """,
                            ),
                            ExampleObject(
                                name = "Not allowed",
                                value = """
                                {
                                    "errorCode": "PROJECT_SKILL_ACCESS_DENIED",
                                    "errorMessage": "Not allowed to modify ProjectSkill.",
                                    "fieldErrors": []
                                }
                                """,
                            ),
                        ],
                    ),
                ],
            ),
            ApiResponse(
                responseCode = "404",
                description = "Project skill not found.",
                content = [
                    Content(
                        mediaType = "application/json",
                        schema = Schema(implementation = GlobalErrorCodeResponse::class),
                        examples = [
                            ExampleObject(
                                name = "Skill not found",
                                value = """
                                {
                                    "errorCode": "PROJECT_SKILL_NOT_FOUND",
                                    "errorMessage": "Project skill could not be found.",
                                    "fieldErrors": []
                                }
                                """,
                            ),
                        ],
                    ),
                ],
            ),
        ],
    )
    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('PROJECTMANAGER')")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    fun delete(
        @AuthenticationPrincipal securityUser: SecurityUser,
        @PathVariable projectId: String,
        @PathVariable id: String,
    ) {
        service.deleteSkill(securityUser.user, projectId, id)
    }
}
