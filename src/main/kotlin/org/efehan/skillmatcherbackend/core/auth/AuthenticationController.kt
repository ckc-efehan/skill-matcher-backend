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
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/auth")
@Tag(name = "Authentication", description = "Authentication endpoints")
class AuthenticationController(
    private val authenticationService: AuthenticationService,
) {
    @Operation(
        summary = "Login user",
        method = "POST",
        description = "Authenticates a user and returns access and refresh tokens",
    )
    @ApiResponses(
        value = [
            ApiResponse(
                responseCode = "200",
                description = "Login successful.",
                content = [
                    Content(
                        mediaType = "application/json",
                        schema = Schema(implementation = AuthResponse::class),
                        examples = [
                            ExampleObject(
                                name = "Successful login",
                                value = """
                                {
                                    "accessToken": "eyJhbGciOiJSUzI1NiJ9...",
                                    "refreshToken": "a1b2c3d4-e5f6-7890-abcd-ef1234567890",
                                    "tokenType": "Bearer",
                                    "expiresIn": 900000,
                                    "user": {
                                        "id": "550e8400-e29b-41d4-a716-446655440000",
                                        "email": "user@example.com",
                                        "firstName": "John",
                                        "lastName": "Doe",
                                        "role": "ADMIN"
                                    }
                                }
                                """,
                            ),
                        ],
                    ),
                ],
            ),
            ApiResponse(
                responseCode = "401",
                description = "Invalid credentials.",
                content = [
                    Content(
                        mediaType = "application/json",
                        schema = Schema(implementation = GlobalErrorCodeResponse::class),
                        examples = [
                            ExampleObject(
                                name = "Bad credentials",
                                value = """
                                {
                                    "errorCode": "BAD_CREDENTIALS",
                                    "errorMessage": "Bad credentials.",
                                    "fieldErrors": []
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
            ApiResponse(
                responseCode = "403",
                description = "Account disabled.",
                content = [
                    Content(
                        mediaType = "application/json",
                        schema = Schema(implementation = GlobalErrorCodeResponse::class),
                        examples = [
                            ExampleObject(
                                name = "Account disabled",
                                value = """
                                {
                                    "errorCode": "ACCOUNT_DISABLED",
                                    "errorMessage": "Account is disabled.",
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
    @PostMapping("/login")
    fun login(
        @Valid
        @RequestBody
        request: LoginRequest,
    ): ResponseEntity<AuthResponse> =
        ResponseEntity.ok(
            authenticationService.login(
                request.email,
                request.password,
            ),
        )

    @Operation(
        summary = "Refresh access token",
        method = "POST",
        description =
            "Uses a valid refresh token to generate a new access token. " +
                "If the refresh token is close to expiration (< 2 days), it will be rotated.",
    )
    @ApiResponses(
        value = [
            ApiResponse(
                responseCode = "200",
                description = "Token refreshed successfully.",
                content = [
                    Content(
                        mediaType = "application/json",
                        schema = Schema(implementation = AuthResponse::class),
                        examples = [
                            ExampleObject(
                                name = "Successful refresh",
                                value = """
                                {
                                    "accessToken": "eyJhbGciOiJSUzI1NiJ9...",
                                    "refreshToken": "a1b2c3d4-e5f6-7890-abcd-ef1234567890",
                                    "tokenType": "Bearer",
                                    "expiresIn": 900000,
                                    "user": {
                                        "id": "550e8400-e29b-41d4-a716-446655440000",
                                        "email": "user@example.com",
                                        "firstName": "John",
                                        "lastName": "Doe",
                                        "role": "ADMIN"
                                    }
                                }
                                """,
                            ),
                        ],
                    ),
                ],
            ),
            ApiResponse(
                responseCode = "400",
                description = "Refresh token not found.",
                content = [
                    Content(
                        mediaType = "application/json",
                        schema = Schema(implementation = GlobalErrorCodeResponse::class),
                        examples = [
                            ExampleObject(
                                name = "Token not found",
                                value = """
                                {
                                    "errorCode": "REFRESH_TOKEN_NOT_FOUND",
                                    "errorMessage": "RefreshToken with token not found.",
                                    "fieldErrors": []
                                }
                                """,
                            ),
                        ],
                    ),
                ],
            ),
            ApiResponse(
                responseCode = "401",
                description = "Refresh token expired or invalid.",
                content = [
                    Content(
                        mediaType = "application/json",
                        schema = Schema(implementation = GlobalErrorCodeResponse::class),
                        examples = [
                            ExampleObject(
                                name = "Token expired or invalid",
                                value = """
                                {
                                    "errorCode": "INVALID_REFRESH_TOKEN",
                                    "errorMessage": "Refresh token is expired or invalid.",
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
    @PostMapping("/refresh")
    fun refreshToken(
        @RequestBody
        request: RefreshTokenRequest,
    ): ResponseEntity<AuthResponse> =
        ResponseEntity.ok(
            authenticationService.refreshToken(request.refreshToken),
        )

    @Operation(
        summary = "Logout user",
        method = "POST",
        description = "Revokes all refresh tokens for the authenticated user.",
    )
    @ApiResponses(
        value = [
            ApiResponse(
                responseCode = "204",
                description = "Logout successful.",
                content = [Content()],
            ),
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
        ],
    )
    @PostMapping("/logout")
    fun logout(
        @AuthenticationPrincipal securityUser: SecurityUser,
    ): ResponseEntity<Void> {
        authenticationService.logout(securityUser.user.id)
        return ResponseEntity.noContent().build()
    }
}
