package org.efehan.skillmatcherbackend.core.projectmember

import jakarta.validation.constraints.NotBlank
import java.time.Instant

data class AddProjectMemberRequest(
    @field:NotBlank
    val userId: String,
)

data class ProjectMemberDto(
    val id: String,
    val userId: String,
    val userName: String,
    val email: String,
    val status: String,
    val joinedDate: Instant,
)
