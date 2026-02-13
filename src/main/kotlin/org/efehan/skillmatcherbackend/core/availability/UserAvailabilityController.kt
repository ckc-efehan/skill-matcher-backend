package org.efehan.skillmatcherbackend.core.availability

import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.media.Content
import io.swagger.v3.oas.annotations.media.ExampleObject
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
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/me/availability")
@Tag(name = "My Availability", description = "Manage own availability periods")
class UserAvailabilityController(
    private val service: UserAvailabilityService,
) {
    @Operation(
        summary = "Add an availability period",
        description = "Creates a new availability period for the authenticated user. Periods must not overlap.",
    )
    @ApiResponses(
        value = [
            ApiResponse(responseCode = "201", description = "Availability period created."),
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
                                    "fieldErrors": [{"field": "availableFrom", "message": "must not be null"}]
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
                                value = """{"errorCode": "USER_MUST_LOGIN", "errorMessage": "User must be logged in."}""",
                            ),
                        ],
                    ),
                ],
            ),
            ApiResponse(
                responseCode = "409",
                description = "Availability period overlaps with an existing entry.",
                content = [
                    Content(
                        mediaType = "application/json",
                        schema = Schema(implementation = GlobalErrorCodeResponse::class),
                        examples = [
                            ExampleObject(
                                name = "Overlap",
                                value = """
                                {"errorCode": "USER_AVAILABILITY_OVERLAP",
                                "errorMessage": "Availability period overlaps with an existing entry."}
                                """,
                            ),
                        ],
                    ),
                ],
            ),
        ],
    )
    @PostMapping
    fun create(
        @AuthenticationPrincipal securityUser: SecurityUser,
        @Valid @RequestBody request: CreateAvailabilityRequest,
    ): ResponseEntity<UserAvailabilityDto> = ResponseEntity.status(HttpStatus.CREATED).body(service.create(securityUser.user, request))

    @Operation(
        summary = "Get my availability periods",
        description = "Returns all availability periods of the authenticated user, sorted by start date.",
    )
    @ApiResponses(
        value = [
            ApiResponse(responseCode = "200", description = "Availability periods retrieved."),
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
                                value = """{"errorCode": "USER_MUST_LOGIN", "errorMessage": "User must be logged in."}""",
                            ),
                        ],
                    ),
                ],
            ),
        ],
    )
    @GetMapping
    fun getAll(
        @AuthenticationPrincipal securityUser: SecurityUser,
    ): ResponseEntity<List<UserAvailabilityDto>> = ResponseEntity.ok(service.getAll(securityUser.user))

    @Operation(
        summary = "Update an availability period",
        description = "Updates an existing availability period. Periods must not overlap with other entries.",
    )
    @ApiResponses(
        value = [
            ApiResponse(responseCode = "200", description = "Availability period updated."),
            ApiResponse(
                responseCode = "401",
                description = "Not authenticated.",
                content = [
                    Content(
                        mediaType = "application/json",
                        schema = Schema(implementation = GlobalErrorCodeResponse::class),
                    ),
                ],
            ),
            ApiResponse(
                responseCode = "403",
                description = "Not allowed to modify this entry.",
                content = [
                    Content(
                        mediaType = "application/json",
                        schema = Schema(implementation = GlobalErrorCodeResponse::class),
                        examples = [
                            ExampleObject(
                                name = "Forbidden",
                                value = """
                                {"errorCode": "USER_AVAILABILITY_ACCESS_DENIED",
                                "errorMessage": "Not allowed to modify this availability entry."}
                                """,
                            ),
                        ],
                    ),
                ],
            ),
            ApiResponse(
                responseCode = "404",
                description = "Availability entry not found.",
                content = [
                    Content(
                        mediaType = "application/json",
                        schema = Schema(implementation = GlobalErrorCodeResponse::class),
                        examples = [
                            ExampleObject(
                                name = "Not found",
                                value = """
                                {"errorCode": "USER_AVAILABILITY_NOT_FOUND",
                                "errorMessage": "UserAvailability with id 'some-id' could not be found."}
                                """,
                            ),
                        ],
                    ),
                ],
            ),
            ApiResponse(
                responseCode = "409",
                description = "Overlap with an existing entry.",
                content = [
                    Content(
                        mediaType = "application/json",
                        schema = Schema(implementation = GlobalErrorCodeResponse::class),
                    ),
                ],
            ),
        ],
    )
    @PutMapping("/{id}")
    fun update(
        @AuthenticationPrincipal securityUser: SecurityUser,
        @PathVariable id: String,
        @Valid @RequestBody request: UpdateAvailabilityRequest,
    ): ResponseEntity<UserAvailabilityDto> = ResponseEntity.ok(service.update(securityUser.user, id, request))

    @Operation(
        summary = "Delete an availability period",
        description = "Deletes an availability period from the authenticated user's profile.",
    )
    @ApiResponses(
        value = [
            ApiResponse(responseCode = "204", description = "Availability period deleted.", content = [Content()]),
            ApiResponse(
                responseCode = "401",
                description = "Not authenticated.",
                content = [
                    Content(
                        mediaType = "application/json",
                        schema = Schema(implementation = GlobalErrorCodeResponse::class),
                    ),
                ],
            ),
            ApiResponse(
                responseCode = "403",
                description = "Not allowed to delete this entry.",
                content = [
                    Content(
                        mediaType = "application/json",
                        schema = Schema(implementation = GlobalErrorCodeResponse::class),
                    ),
                ],
            ),
            ApiResponse(
                responseCode = "404",
                description = "Availability entry not found.",
                content = [
                    Content(
                        mediaType = "application/json",
                        schema = Schema(implementation = GlobalErrorCodeResponse::class),
                    ),
                ],
            ),
        ],
    )
    @DeleteMapping("/{id}")
    fun delete(
        @AuthenticationPrincipal securityUser: SecurityUser,
        @PathVariable id: String,
    ): ResponseEntity<Void> {
        service.delete(securityUser.user, id)
        return ResponseEntity.noContent().build()
    }
}
