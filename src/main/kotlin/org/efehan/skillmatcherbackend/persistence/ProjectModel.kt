package org.efehan.skillmatcherbackend.persistence

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.JoinColumn
import jakarta.persistence.ManyToOne
import jakarta.persistence.Table
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.stereotype.Repository
import java.time.LocalDate

@Entity
@Table(name = "projects")
class ProjectModel(
    @Column(name = "name", nullable = false)
    var name: String,
    @Column(name = "description", nullable = false)
    var description: String,
    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    var status: ProjectStatus,
    @Column(name = "start_date", nullable = false)
    var startDate: LocalDate,
    @Column(name = "end_date", nullable = false)
    var endDate: LocalDate,
    @Column(name = "max_members", nullable = false)
    var maxMembers: Int,
    @ManyToOne(optional = false)
    @JoinColumn(name = "owner_id", nullable = false)
    val owner: UserModel,
) : AuditingBaseEntity()

@Repository
interface ProjectRepository : JpaRepository<ProjectModel, String> {
    fun findByStatusIn(statuses: Collection<ProjectStatus>): List<ProjectModel>

    @Query(
        """
        SELECT p
        FROM ProjectModel p
        WHERE p.status IN :statuses
          AND p.id NOT IN (
              SELECT pm.project.id
              FROM ProjectMemberModel pm
              WHERE pm.user = :user
                AND pm.status = :activeStatus
          )
        """,
    )
    fun findMatchableForUser(
        user: UserModel,
        statuses: Collection<ProjectStatus>,
        activeStatus: ProjectMemberStatus,
    ): List<ProjectModel>
}

enum class ProjectStatus {
    PLANNED,
    ACTIVE,
    PAUSED,
    COMPLETED,
}
