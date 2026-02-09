package org.efehan.skillmatcherbackend.core.admin

import jakarta.validation.constraints.Email
import jakarta.validation.constraints.NotBlank

data class CreateUserRequest(
    @field:NotBlank(message = "firstName must not be blank")
    val firstName: String,
    @field:NotBlank(message = "lastName must not be blank")
    val lastName: String,
    @field:Email(message = "email must be a valid email address")
    @field:NotBlank(message = "email must not be blank")
    val email: String,
    @field:NotBlank(message = "role must not be blank")
    val role: String,
)

data class CreateUserResponse(
    val id: String,
    val username: String,
    val email: String,
    val firstName: String,
    val lastName: String,
    val role: String,
)
