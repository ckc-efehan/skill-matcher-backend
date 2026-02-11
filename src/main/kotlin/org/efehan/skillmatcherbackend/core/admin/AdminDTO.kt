package org.efehan.skillmatcherbackend.core.admin

import jakarta.validation.constraints.Email
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.NotNull
import java.time.Instant

data class CreateUserRequest(
    @field:Email(message = "email must be a valid email address")
    @field:NotBlank(message = "email must not be blank")
    val email: String,
    @field:NotBlank(message = "role must not be blank")
    val role: String,
)

data class CreateUserResponse(
    val id: String,
    val email: String,
    val role: String,
)

data class UpdateUserStatusRequest(
    @field:NotNull(message = "isEnabled must not be null")
    var enabled: Boolean,
)

data class AdminUserListResponse(
    val id: String,
    val email: String,
    val firstName: String?,
    val lastName: String?,
    val role: String,
    val isEnabled: Boolean,
    val createdDate: Instant?,
)

data class UpdateUserRoleRequest(
    @field:NotBlank(message = "role must not be blank")
    val role: String,
)
