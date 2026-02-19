package org.efehan.skillmatcherbackend.fixtures.requests

import org.efehan.skillmatcherbackend.core.projectmember.AddProjectMemberRequest

object ProjectMemberFixtures {
    fun buildAddProjectMemberRequest(userId: String = "default-user-id"): AddProjectMemberRequest = AddProjectMemberRequest(userId = userId)
}
