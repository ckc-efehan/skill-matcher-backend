package org.efehan.skillmatcherbackend.persistence

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.Table
import org.efehan.skillmatcherbackend.core.skill.SkillDto
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository

@Entity
@Table(name = "skills")
class SkillModel(
    @Column(name = "name", nullable = false, unique = true)
    val name: String,
) : AuditingBaseEntity() {
    fun toDTO() =
        SkillDto(
            id = id,
            name = name,
        )
}

@Repository
interface SkillRepository : JpaRepository<SkillModel, String> {
    fun findByNameIgnoreCase(name: String): SkillModel?
}
