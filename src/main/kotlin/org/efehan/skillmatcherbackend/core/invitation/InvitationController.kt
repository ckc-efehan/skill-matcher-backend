package org.efehan.skillmatcherbackend.core.invitation

import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.media.Content
import io.swagger.v3.oas.annotations.media.Schema
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.responses.ApiResponses
import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.validation.Valid
import org.efehan.skillmatcherbackend.core.auth.AuthResponse
import org.efehan.skillmatcherbackend.exception.GlobalErrorCodeResponse
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/auth/invitations")
@Tag(name = "Invitation", description = "Invitation endpoints")
class InvitationController(
    private val invitationService: InvitationService,
) {
    @Operation(
        summary = "Validate invitation token",
        method = "POST",
        description =
            "Validates an invitation token without accepting it. " +
                "Use this to check if a token is valid before showing the password form.",
    )
    @ApiResponses(
        value = [
            ApiResponse(
                responseCode = "200",
                description = "Token is valid.",
                content = [
                    Content(
                        mediaType = "application/json",
                        schema = Schema(implementation = ValidateInvitationResponse::class),
                    ),
                ],
            ),
            ApiResponse(
                responseCode = "400",
                description = "Invalid, expired, or already used invitation token.",
                content = [
                    Content(
                        mediaType = "application/json",
                        schema = Schema(implementation = GlobalErrorCodeResponse::class),
                    ),
                ],
            ),
        ],
    )
    @PostMapping("/validate")
    fun validateInvitation(
        @Valid
        @RequestBody
        request: ValidateInvitationRequest,
    ): ResponseEntity<ValidateInvitationResponse> =
        ResponseEntity.ok(
            invitationService.validateInvitation(request.token),
        )

    @Operation(
        summary = "Accept invitation",
        method = "POST",
        description = "Accepts an invitation token and sets the user's password. Returns access and refresh tokens.",
    )
    @ApiResponses(
        value = [
            ApiResponse(
                responseCode = "200",
                description = "Invitation accepted successfully.",
                content = [
                    Content(
                        mediaType = "application/json",
                        schema = Schema(implementation = AuthResponse::class),
                    ),
                ],
            ),
            ApiResponse(
                responseCode = "400",
                description = "Invalid, expired, or already used invitation token.",
                content = [
                    Content(
                        mediaType = "application/json",
                        schema = Schema(implementation = GlobalErrorCodeResponse::class),
                    ),
                ],
            ),
        ],
    )
    @PostMapping("/accept")
    fun acceptInvitation(
        @Valid
        @RequestBody
        request: AcceptInvitationRequest,
    ): ResponseEntity<AuthResponse> =
        ResponseEntity.ok(
            invitationService.acceptInvitation(request.token, request.password),
        )
}
