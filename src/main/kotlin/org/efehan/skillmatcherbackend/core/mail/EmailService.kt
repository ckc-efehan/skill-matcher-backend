package org.efehan.skillmatcherbackend.core.mail

import org.efehan.skillmatcherbackend.persistence.UserModel

interface EmailService {
    fun sendInvitationEmail(
        user: UserModel,
        invitationToken: String,
        expirationHours: Long,
    )

    fun sendWelcomeEmail(user: UserModel)
}
