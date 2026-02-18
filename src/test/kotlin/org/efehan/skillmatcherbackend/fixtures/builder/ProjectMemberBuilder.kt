package org.efehan.skillmatcherbackend.fixtures.builder

import org.efehan.skillmatcherbackend.persistence.ProjectMemberModel
import org.efehan.skillmatcherbackend.persistence.ProjectMemberStatus
import org.efehan.skillmatcherbackend.persistence.ProjectModel
import org.efehan.skillmatcherbackend.persistence.UserModel
import java.time.Instant

class ProjectMemberBuilder {
    fun build(
        project: ProjectModel = ProjectBuilder().build(),
        user: UserModel = UserBuilder().build(),
        status: ProjectMemberStatus = ProjectMemberStatus.ACTIVE,
        joinedDate: Instant = Instant.now(),
    ): ProjectMemberModel =
        ProjectMemberModel(
            project = project,
            user = user,
            status = status,
            joinedDate = joinedDate,
        )
}
