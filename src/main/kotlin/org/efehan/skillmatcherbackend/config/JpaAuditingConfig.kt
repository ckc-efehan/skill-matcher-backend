package org.efehan.skillmatcherbackend.config

import org.efehan.skillmatcherbackend.core.auth.SecurityUser
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.data.domain.AuditorAware
import org.springframework.data.jpa.repository.config.EnableJpaAuditing
import org.springframework.security.authentication.AnonymousAuthenticationToken
import org.springframework.security.core.context.SecurityContextHolder
import java.util.Optional

@Configuration
@EnableJpaAuditing
class JpaAuditingConfig {
    @Bean
    fun auditorProvider(): AuditorAware<String> =
        AuditorAware {
            val authentication = SecurityContextHolder.getContext().authentication

            if (authentication == null || !authentication.isAuthenticated || authentication is AnonymousAuthenticationToken) {
                Optional.of("SYSTEM")
            } else {
                val securityUser = authentication.principal as SecurityUser
                Optional.of(securityUser.user.id)
            }
        }
}
