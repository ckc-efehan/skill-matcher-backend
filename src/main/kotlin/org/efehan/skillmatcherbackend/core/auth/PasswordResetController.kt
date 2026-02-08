package org.efehan.skillmatcherbackend.core.auth

import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.media.Content
import io.swagger.v3.oas.annotations.media.ExampleObject
import io.swagger.v3.oas.annotations.media.Schema
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.responses.ApiResponses
import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.validation.Valid
import org.efehan.skillmatcherbackend.exception.GlobalErrorCodeResponse
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/auth/password-reset")
@Tag(name = "Password Reset", description = "Password reset endpoints")
class PasswordResetController(
    private val passwordResetService: PasswordResetService,
) {
    @Operation(
        summary = "Request password reset",
        method = "POST",
        description =
            "Sends a password reset email to the provided email address. " +
                "Always returns success to prevent email enumeration attacks.",
    )
    @ApiResponses(
        value = [
            ApiResponse(
                responseCode = "204",
                description = "Password reset email sent (if account exists).",
                content = [Content()],
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
                                            "field": "email",
                                            "message": "email must be a valid email address"
                                        }
                                    ]
                                }
                                """,
                            ),
                        ],
                    ),
                ],
            ),
        ],
    )
    @PostMapping("/request")
    fun requestPasswordReset(
        @Valid
        @RequestBody
        request: PasswordResetRequestDTO,
    ): ResponseEntity<Void> {
        passwordResetService.requestPasswordReset(request.email)
        return ResponseEntity.noContent().build()
    }

    @Operation(
        summary = "Validate password reset token",
        method = "POST",
        description =
            "Validates a password reset token without consuming it. " +
                "Returns whether the token is valid and a masked email address.",
    )
    @ApiResponses(
        value = [
            ApiResponse(
                responseCode = "200",
                description = "Token validation result.",
                content = [
                    Content(
                        mediaType = "application/json",
                        schema = Schema(implementation = PasswordResetTokenValidationResponse::class),
                        examples = [
                            ExampleObject(
                                name = "Valid token",
                                value = """
                                {
                                    "valid": true,
                                    "email": "t***t@example.com"
                                }
                                """,
                            ),
                            ExampleObject(
                                name = "Invalid token",
                                value = """
                                {
                                    "valid": false,
                                    "email": null
                                }
                                """,
                            ),
                        ],
                    ),
                ],
            ),
        ],
    )
    @PostMapping("/validate")
    fun validateToken(
        @Valid
        @RequestBody
        request: ValidatePasswordResetTokenDTO,
    ): ResponseEntity<PasswordResetTokenValidationResponse> = ResponseEntity.ok(passwordResetService.validateToken(request.token))

    @Operation(
        summary = "Reset password",
        method = "POST",
        description = "Resets the password using a valid reset token.",
    )
    @ApiResponses(
        value = [
            ApiResponse(
                responseCode = "204",
                description = "Password successfully reset.",
                content = [Content()],
            ),
            ApiResponse(
                responseCode = "400",
                description = "Invalid or expired token, or password validation failed.",
                content = [
                    Content(
                        mediaType = "application/json",
                        schema = Schema(implementation = GlobalErrorCodeResponse::class),
                        examples = [
                            ExampleObject(
                                name = "Invalid token",
                                value = """
                                {
                                    "errorCode": "INVALID_PASSWORD_RESET_TOKEN",
                                    "errorMessage": "Invalid password reset token.",
                                    "fieldErrors": []
                                }
                                """,
                            ),
                            ExampleObject(
                                name = "Token expired",
                                value = """
                                {
                                    "errorCode": "PASSWORD_RESET_TOKEN_EXPIRED",
                                    "errorMessage": "Password reset token has expired.",
                                    "fieldErrors": []
                                }
                                """,
                            ),
                            ExampleObject(
                                name = "Token already used",
                                value = """
                                {
                                    "errorCode": "PASSWORD_RESET_TOKEN_USED",
                                    "errorMessage": "Password reset token has already been used.",
                                    "fieldErrors": []
                                }
                                """,
                            ),
                            ExampleObject(
                                name = "Password validation error",
                                value = """
                                {
                                    "errorCode": "VALIDATION_ERROR",
                                    "errorMessage": "Password does not meet the required complexity.",
                                    "fieldErrors": [
                                        {
                                            "field": "password",
                                            "message": "Password must be at least 8 characters long"
                                        }
                                    ]
                                }
                                """,
                            ),
                        ],
                    ),
                ],
            ),
        ],
    )
    @PostMapping("/confirm")
    fun resetPassword(
        @Valid
        @RequestBody
        request: PasswordResetConfirmDTO,
    ): ResponseEntity<Void> {
        passwordResetService.resetPassword(request.token, request.newPassword)
        return ResponseEntity.noContent().build()
    }
}
