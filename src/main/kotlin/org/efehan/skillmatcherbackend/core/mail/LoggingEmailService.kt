package org.efehan.skillmatcherbackend.core.mail

import org.efehan.skillmatcherbackend.persistence.UserModel
import org.slf4j.LoggerFactory
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.stereotype.Service

@Service
@ConditionalOnProperty(name = ["mail.smtp.enabled"], havingValue = "false", matchIfMissing = true)
class LoggingEmailService : EmailService {
    private val logger = LoggerFactory.getLogger(LoggingEmailService::class.java)

    override fun sendInvitationEmail(
        user: UserModel,
        invitationToken: String,
        expirationHours: Long,
    ) {
        logger.info("Mock sending invitation email to {} with token {} (expires in {}h)", user.email, invitationToken, expirationHours)
    }

    override fun sendWelcomeEmail(user: UserModel) {
        logger.info("Mock sending welcome email to {}", user.email)
    }
}
