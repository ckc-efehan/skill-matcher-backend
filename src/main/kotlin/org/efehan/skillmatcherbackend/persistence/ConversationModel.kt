package org.efehan.skillmatcherbackend.persistence

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.JoinColumn
import jakarta.persistence.ManyToOne
import jakarta.persistence.Table
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.stereotype.Repository

@Entity
@Table(name = "conversations")
class ConversationModel(
    @ManyToOne(optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    val userOne: UserModel,
    @ManyToOne(optional = false)
    @JoinColumn(name = "user_two_id", nullable = false)
    val userTwo: UserModel,
) : AuditingBaseEntity()

@Repository
interface ConversationRepository : JpaRepository<ConversationModel, String> {
    @Query(
        """
          SELECT c FROM ConversationModel c
          WHERE c.userOne = :user OR c.userTwo = :user
          ORDER BY c.lastModifiedDate DESC
          """,
    )
    fun findByUser(user: UserModel): List<ConversationModel>

    @Query(
        """
          SELECT c FROM ConversationModel c
          WHERE (c.userOne = :userOne AND c.userTwo = :userTwo)
             OR (c.userOne = :userTwo AND c.userTwo = :userOne)
          """,
    )
    fun findByUsers(userOne: UserModel, userTwo: UserModel): ConversationModel?
}