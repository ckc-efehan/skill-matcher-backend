package org.efehan.skillmatcherbackend.core.invitation

import jakarta.validation.constraints.NotBlank

data class AcceptInvitationRequest(
    @field:NotBlank(message = "token must not be blank")
    val token: String,
    @field:NotBlank(message = "password must not be blank")
    val password: String,
)

data class ValidateInvitationRequest(
    @field:NotBlank(message = "token must not be blank")
    val token: String,
)

data class ValidateInvitationResponse(
    val valid: Boolean,
    val email: String,
)
