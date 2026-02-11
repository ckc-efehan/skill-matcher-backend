package org.efehan.skillmatcherbackend.persistence

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.JoinColumn
import jakarta.persistence.ManyToOne
import jakarta.persistence.Table
import jakarta.persistence.UniqueConstraint
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository

@Entity
@Table(
    name = "project_skills",
    uniqueConstraints = [
        UniqueConstraint(columnNames = ["project_id", "skill_id"]),
    ],
)
class ProjectSkillModel(
    @ManyToOne(optional = false)
    @JoinColumn(name = "project_id", nullable = false)
    val project: ProjectModel,
    @ManyToOne(optional = false)
    @JoinColumn(name = "skill_id", nullable = false)
    val skill: SkillModel,
    @Column(nullable = false)
    var level: Int,
) : AuditingBaseEntity()

@Repository
interface ProjectSkillRepository : JpaRepository<ProjectSkillModel, String> {
    fun findByProject(project: ProjectModel): List<ProjectSkillModel>

    fun findByProjectAndSkillId(
        project: ProjectModel,
        skillId: String,
    ): ProjectSkillModel?
}
