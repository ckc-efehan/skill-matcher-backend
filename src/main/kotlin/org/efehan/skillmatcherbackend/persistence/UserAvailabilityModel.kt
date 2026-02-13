package org.efehan.skillmatcherbackend.persistence

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.Index
import jakarta.persistence.JoinColumn
import jakarta.persistence.ManyToOne
import jakarta.persistence.Table
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository
import java.time.Instant

@Entity
@Table(
    name = "user_availibility",
    indexes = [
        Index(name = "idx_user_availibility_user_id", columnList = "user_id"),
    ],
)
class UserAvailabilityModel(
    @ManyToOne(optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    val user: UserModel,
    @Column(name = "available_from", nullable = false)
    var availableFrom: Instant,
    @Column(name = "available_to", nullable = false)
    var availableTo: Instant,
): AuditingBaseEntity()

@Repository
interface UserAvailibilityRepository : JpaRepository<UserAvailabilityModel, String> {
    fun findByUser(user: UserModel): List<UserAvailabilityModel>
}