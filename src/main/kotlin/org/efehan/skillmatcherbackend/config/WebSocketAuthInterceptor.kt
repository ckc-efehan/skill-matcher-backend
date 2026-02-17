package org.efehan.skillmatcherbackend.config

import org.efehan.skillmatcherbackend.core.auth.CustomUserDetailsService
import org.efehan.skillmatcherbackend.core.auth.JwtService
import org.efehan.skillmatcherbackend.shared.exceptions.InvalidTokenException
import org.springframework.context.annotation.Configuration
import org.springframework.messaging.Message
import org.springframework.messaging.MessageChannel
import org.springframework.messaging.simp.stomp.StompCommand
import org.springframework.messaging.simp.stomp.StompHeaderAccessor
import org.springframework.messaging.support.ChannelInterceptor
import org.springframework.messaging.support.MessageHeaderAccessor
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken

@Configuration
class WebSocketAuthInterceptor(
    private val jwtService: JwtService,
    private val userDetailsService: CustomUserDetailsService,
) : ChannelInterceptor {
    override fun preSend(
        message: Message<*>,
        channel: MessageChannel,
    ): Message<*> {
        val accessor = MessageHeaderAccessor.getAccessor(message, StompHeaderAccessor::class.java)

        if (accessor != null && accessor.command == StompCommand.CONNECT) {
            val token =
                accessor
                    .getFirstNativeHeader("Authorization")
                    ?.removePrefix("Bearer ")
                    ?.ifBlank { null }

            if (token != null) {
                try {
                    val email = jwtService.getEmail(token)
                    val userDetails = userDetailsService.loadUserByUsername(email)

                    if (userDetails.isEnabled) {
                        accessor.user =
                            UsernamePasswordAuthenticationToken(
                                userDetails,
                                null,
                                userDetails.authorities,
                            )
                    }
                } catch (_: InvalidTokenException) {
                }
            }
        }
        return message
    }
}
