package org.efehan.skillmatcherbackend.persistence

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.FetchType
import jakarta.persistence.JoinColumn
import jakarta.persistence.ManyToOne
import jakarta.persistence.Table
import org.springframework.data.jpa.repository.JpaRepository
import java.time.Instant

@Entity
@Table(name = "refresh_tokens")
class RefreshTokenModel(
    @Column(name = "token", nullable = false, unique = true)
    val token: String,
    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    val user: UserModel,
    @Column(name = "expires_at", nullable = false)
    val expiresAt: Instant,
    @Column(name = "revoked", nullable = false)
    val revoked: Boolean = false,
) : AuditingBaseEntity()

interface RefreshTokenRepository : JpaRepository<RefreshTokenModel, String> {
    fun findByToken(token: String): RefreshTokenModel?
}
