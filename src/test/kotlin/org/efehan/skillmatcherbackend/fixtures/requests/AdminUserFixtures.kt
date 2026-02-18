package org.efehan.skillmatcherbackend.fixtures.requests

import org.efehan.skillmatcherbackend.core.admin.CreateUserRequest
import org.efehan.skillmatcherbackend.core.admin.UpdateUserRoleRequest
import org.efehan.skillmatcherbackend.core.admin.UpdateUserStatusRequest

object AdminUserFixtures {
    fun buildCreateUserRequest(
        email: String = "max.mustermann@firma.de",
        role: String = "EMPLOYER",
    ): CreateUserRequest =
        CreateUserRequest(
            email = email,
            role = role,
        )

    fun buildUpdateUserStatusRequest(enabled: Boolean = false): UpdateUserStatusRequest = UpdateUserStatusRequest(enabled = enabled)

    fun buildUpdateUserRoleRequest(role: String = "PROJECTMANAGER"): UpdateUserRoleRequest = UpdateUserRoleRequest(role = role)
}
