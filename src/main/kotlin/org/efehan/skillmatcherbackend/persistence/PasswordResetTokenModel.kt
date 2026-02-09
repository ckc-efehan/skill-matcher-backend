package org.efehan.skillmatcherbackend.persistence

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.FetchType
import jakarta.persistence.Index
import jakarta.persistence.JoinColumn
import jakarta.persistence.ManyToOne
import jakarta.persistence.Table
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Modifying
import org.springframework.data.jpa.repository.Query
import org.springframework.stereotype.Repository
import java.time.Instant

@Entity
@Table(
    name = "password_reset_tokens",
    indexes = [
        Index(
            name = "idx_password_reset_tokens_token_hash",
            columnList = "token_hash",
        ),
    ],
)
class PasswordResetTokenModel(
    @Column(name = "token_hash", nullable = false, unique = true)
    val tokenHash: String,
    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    val user: UserModel,
    @Column(name = "expires_at", nullable = false)
    val expiresAt: Instant,
    @Column(name = "used", nullable = false)
    var used: Boolean = false,
) : AuditingBaseEntity()

@Repository
interface PasswordResetTokenRepository : JpaRepository<PasswordResetTokenModel, String> {
    fun findByTokenHash(tokenHash: String): PasswordResetTokenModel?

    @Modifying
    @Query("UPDATE PasswordResetTokenModel p SET p.used = true WHERE p.user.id = :userId AND p.used = false")
    fun invalidateAllUserTokens(userId: String)
}
