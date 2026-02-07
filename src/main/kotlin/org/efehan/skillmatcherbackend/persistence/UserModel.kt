package org.efehan.skillmatcherbackend.persistence

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.FetchType
import jakarta.persistence.Index
import jakarta.persistence.JoinColumn
import jakarta.persistence.ManyToOne
import jakarta.persistence.Table
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository

@Entity
@Table(
    name = "users",
    indexes = [
        Index(
            name = "idx_users_username",
            columnList = "username",
        ),
        Index(
            name = "idx_users_email",
            columnList = "email",
        ),
    ],
)
class UserModel(
    @Column(name = "username", nullable = true)
    var username: String?,
    @Column(name = "email", nullable = false, unique = true)
    val email: String,
    @Column(name = "password_hash", nullable = true)
    var passwordHash: String?,
    @Column(name = "first_name", nullable = true)
    var firstName: String?,
    @Column(name = "last_name", nullable = true)
    var lastName: String?,
    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "role_id", nullable = false)
    var role: RoleModel,
) : AuditingBaseEntity() {
    @Column(name = "is_enabled", nullable = false)
    var isEnabled: Boolean = false
}

@Repository
interface UserRepository : JpaRepository<UserModel, String> {
    fun findByEmail(email: String): UserModel?

    fun existsByUsername(username: String): Boolean

    fun existsByEmail(email: String): Boolean
}
