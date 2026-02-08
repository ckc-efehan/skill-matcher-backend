package org.efehan.skillmatcherbackend.core.auth

import jakarta.validation.constraints.Email
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Size

data class LoginRequest(
    @field:Email(message = "email must be a valid email address")
    val email: String,
    @field:NotBlank(message = "password must be required")
    val password: String,
)

data class AuthResponse(
    val accessToken: String,
    val refreshToken: String,
    val tokenType: String = "Bearer",
    val expiresIn: Long = 900000, // 15 min
    val user: AuthUserResponse,
)

data class AuthUserResponse(
    val id: String,
    val email: String,
    val firstName: String?,
    val lastName: String?,
    val role: String,
)

data class RefreshTokenRequest(
    val refreshToken: String,
)

data class ChangePasswordRequest(
    @field:NotBlank val oldPassword: String,
    @field:Size(min = 8) val newPassword: String,
)
