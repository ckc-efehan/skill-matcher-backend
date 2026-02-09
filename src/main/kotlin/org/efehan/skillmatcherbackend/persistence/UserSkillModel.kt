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
    name = "user_skills",
    uniqueConstraints = [
        UniqueConstraint(columnNames = ["user_id", "skill_id"]),
    ],
)
class UserSkillModel(
    @ManyToOne
    @JoinColumn(name = "user_id")
    val user: UserModel,
    @ManyToOne
    @JoinColumn(name = "skill_id")
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
}
