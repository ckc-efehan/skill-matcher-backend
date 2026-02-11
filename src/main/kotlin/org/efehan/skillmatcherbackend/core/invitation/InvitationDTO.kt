package org.efehan.skillmatcherbackend.core.invitation

import jakarta.validation.constraints.NotBlank

data class AcceptInvitationRequest(
    @field:NotBlank(message = "token must not be blank")
    val token: String,
    @field:NotBlank(message = "password must not be blank")
    val password: String,
    @field:NotBlank(message = "firstName must not be blank")
    val firstName: String,
    @field:NotBlank(message = "lastName must not be blank")
    val lastName: String,
)

data class ValidateInvitationRequest(
    @field:NotBlank(message = "token must not be blank")
    val token: String,
)

data class ValidateInvitationResponse(
    val valid: Boolean,
    val email: String,
)
