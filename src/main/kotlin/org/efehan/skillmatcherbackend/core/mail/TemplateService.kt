package org.efehan.skillmatcherbackend.core.mail

import org.springframework.stereotype.Service
import org.thymeleaf.context.Context
import org.thymeleaf.spring6.SpringTemplateEngine

@Service
class TemplateService(
    private val templateEngine: SpringTemplateEngine,
) {
    fun renderInvitation(
        firstName: String,
        invitationLink: String,
        expirationHours: Long,
    ): String {
        val context =
            Context().apply {
                setVariable("firstName", firstName)
                setVariable("invitationLink", invitationLink)
                setVariable("expirationHours", expirationHours)
            }
        return templateEngine.process("invitation", context)
    }

    fun renderWelcome(firstName: String): String {
        val context =
            Context().apply {
                setVariable("firstName", firstName)
            }
        return templateEngine.process("welcome", context)
    }
}
