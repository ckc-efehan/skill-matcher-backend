package org.efehan.skillmatcherbackend.persistence

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.JoinColumn
import jakarta.persistence.ManyToOne
import jakarta.persistence.Table
import jakarta.persistence.UniqueConstraint
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository
import java.time.Instant

@Entity
@Table(
    name = "project_members",
    uniqueConstraints = [
        UniqueConstraint(columnNames = ["project_id", "user_id"]),
    ],
)
class ProjectMemberModel(
    @ManyToOne(optional = false)
    @JoinColumn(name = "project_id", nullable = false)
    val project: ProjectModel,
    @ManyToOne(optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    val user: UserModel,
    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    var status: ProjectMemberStatus,
    @Column(name = "joined_date", nullable = false)
    val joinedDate: Instant,
): AuditingBaseEntity()

@Repository
interface ProjectMemberRepository : JpaRepository<ProjectMemberModel, String> {
    fun findByProject(project: ProjectModel): List<ProjectMemberModel>

    fun findByProjectAndUser(
        project: ProjectModel,
        user: UserModel,
    ): ProjectMemberModel?

    fun countByProjectAndStatus(
        project: ProjectModel,
        status: ProjectMemberStatus,
    ): Long
}

enum class ProjectMemberStatus {
    ACTIVE,
    LEFT,
}