package org.efehan.skillmatcherbackend.persistence

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.JoinColumn
import jakarta.persistence.ManyToOne
import jakarta.persistence.Table
import jakarta.persistence.UniqueConstraint
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.stereotype.Repository

@Entity
@Table(
    name = "user_skills",
    uniqueConstraints = [
        UniqueConstraint(columnNames = ["user_id", "skill_id"]),
    ],
)
class UserSkillModel(
    @ManyToOne(optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    val user: UserModel,
    @ManyToOne(optional = false)
    @JoinColumn(name = "skill_id", nullable = false)
    val skill: SkillModel,
    @Column(nullable = false)
    var level: Int,
) : AuditingBaseEntity()

@Repository
interface UserSkillRepository : JpaRepository<UserSkillModel, String> {
    fun findByUser(user: UserModel): List<UserSkillModel>

    fun findByUserAndSkillId(
        user: UserModel,
        skillId: String,
    ): UserSkillModel?

    fun findBySkillIn(skills: List<SkillModel>): List<UserSkillModel>

    @Query(
        """
        SELECT us
        FROM UserSkillModel us
        WHERE us.skill IN :skills
          AND us.user.isEnabled = true
          AND us.user.id NOT IN (
              SELECT pm.user.id
              FROM ProjectMemberModel pm
              WHERE pm.project = :project
                AND pm.status = :activeStatus
          )
        """,
    )
    fun findMatchableBySkillsForProject(
        skills: Collection<SkillModel>,
        project: ProjectModel,
        activeStatus: ProjectMemberStatus,
    ): List<UserSkillModel>
}
