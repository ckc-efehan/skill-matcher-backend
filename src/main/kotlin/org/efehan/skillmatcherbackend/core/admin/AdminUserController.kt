package org.efehan.skillmatcherbackend.core.admin

import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.media.Content
import io.swagger.v3.oas.annotations.media.ExampleObject
import io.swagger.v3.oas.annotations.media.Schema
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.responses.ApiResponses
import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.validation.Valid
import org.efehan.skillmatcherbackend.core.invitation.InvitationService
import org.efehan.skillmatcherbackend.exception.GlobalErrorCodeResponse
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PatchMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/admin/users")
@Tag(name = "Admin", description = "Admin endpoints")
class AdminUserController(
    private val adminUserService: AdminUserService,
    private val invitationService: InvitationService,
) {
    @Operation(
        summary = "Create a new user",
        method = "POST",
        description = "Creates a new user with auto-generated username and sends an invitation email. Only accessible by admins.",
    )
    @ApiResponses(
        value = [
            ApiResponse(
                responseCode = "201",
                description = "User created successfully.",
                content = [
                    Content(
                        mediaType = "application/json",
                        schema = Schema(implementation = CreateUserResponse::class),
                        examples = [
                            ExampleObject(
                                name = "User created",
                                value = """
                                {
                                    "id": "550e8400-e29b-41d4-a716-446655440000",
                                    "username": "max.mustermann",
                                    "email": "max.mustermann@firma.de",
                                    "firstName": "Max",
                                    "lastName": "Mustermann",
                                    "role": "EMPLOYER"
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
                    ),
                ],
            ),
            ApiResponse(
                responseCode = "404",
                description = "Role not found.",
                content = [
                    Content(
                        mediaType = "application/json",
                        schema = Schema(implementation = GlobalErrorCodeResponse::class),
                        examples = [
                            ExampleObject(
                                name = "Role not found",
                                value = """
                                {
                                    "errorCode": "ROLE_NOT_FOUND",
                                    "errorMessage": "Role could not be found.",
                                    "fieldErrors": []
                                }
                                """,
                            ),
                        ],
                    ),
                ],
            ),
            ApiResponse(
                responseCode = "409",
                description = "User with this email already exists.",
                content = [
                    Content(
                        mediaType = "application/json",
                        schema = Schema(implementation = GlobalErrorCodeResponse::class),
                        examples = [
                            ExampleObject(
                                name = "Email already exists",
                                value = """
                                {
                                    "errorCode": "USER_ALREADY_EXISTS",
                                    "errorMessage": "User already exists.",
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
    fun createUser(
        @Valid
        @RequestBody
        request: CreateUserRequest,
    ): ResponseEntity<CreateUserResponse> =
        ResponseEntity.status(HttpStatus.CREATED).body(
            adminUserService.createUser(request),
        )

    @Operation(
        summary = "Resend invitation",
        method = "POST",
        description = "Resends an invitation email to a user. Only accessible by admins.",
    )
    @ApiResponses(
        value = [
            ApiResponse(
                responseCode = "204",
                description = "Invitation resent successfully.",
                content = [Content()],
            ),
            ApiResponse(
                responseCode = "404",
                description = "User not found.",
                content = [
                    Content(
                        mediaType = "application/json",
                        schema = Schema(implementation = GlobalErrorCodeResponse::class),
                    ),
                ],
            ),
        ],
    )
    @PostMapping("/{userId}/resend-invitation")
    fun resendInvitation(
        @PathVariable userId: String,
    ): ResponseEntity<Void> {
        invitationService.resendInvitation(userId)
        return ResponseEntity.noContent().build()
    }

    @PatchMapping("/{userId}/status")
    fun updateUserStatus(
        @PathVariable
        userId: String,
        @Valid
        @RequestBody
        request: UpdateUserStatusRequest,
    ): ResponseEntity<Unit> {
        adminUserService.updateUserStatus(userId, request.enabled)
        return ResponseEntity.noContent().build()
    }

    @Operation(
        summary = "List all users",
        method = "GET",
        description = "Returns a list of all users. Only accessible by admins.",
    )
    @ApiResponses(
        value = [
            ApiResponse(
                responseCode = "200",
                description = "Users retrieved successfully.",
            ),
        ],
    )
    @GetMapping
    fun listUsers(): ResponseEntity<List<AdminUserListResponse>> = ResponseEntity.ok(adminUserService.listUsers())

    @Operation(
        summary = "Update user role",
        method = "PATCH",
        description = "Changes the role of a user. Only accessible by admins.",
    )
    @ApiResponses(
        value = [
            ApiResponse(
                responseCode = "204",
                description = "Role updated successfully.",
                content = [Content()],
            ),
            ApiResponse(
                responseCode = "404",
                description = "User or role not found.",
                content = [
                    Content(
                        mediaType = "application/json",
                        schema = Schema(implementation = GlobalErrorCodeResponse::class),
                    ),
                ],
            ),
        ],
    )
    @PatchMapping("/{userId}/role")
    fun updateUserRole(
        @PathVariable userId: String,
        @Valid
        @RequestBody
        request: UpdateUserRoleRequest,
    ): ResponseEntity<Unit> {
        adminUserService.updateUserRole(userId, request.role)
        return ResponseEntity.noContent().build()
    }
}
