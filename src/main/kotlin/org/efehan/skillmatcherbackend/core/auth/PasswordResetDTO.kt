package org.efehan.skillmatcherbackend.core.auth

import jakarta.validation.constraints.Email
import jakarta.validation.constraints.NotBlank

data class PasswordResetRequestDTO(
    @field:Email(message = "email must be a valid email address")
    @field:NotBlank(message = "email must not be blank")
    val email: String,
)

data class PasswordResetConfirmDTO(
    @field:NotBlank(message = "token must not be blank")
    val token: String,
    @field:NotBlank(message = "newPassword must not be blank")
    val newPassword: String,
)

data class ValidatePasswordResetTokenDTO(
    @field:NotBlank(message = "token must not be blank")
    val token: String,
)

data class PasswordResetTokenValidationResponse(
    val valid: Boolean,
    val email: String?,
)
