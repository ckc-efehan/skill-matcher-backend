package org.efehan.skillmatcherbackend.config.filter

import jakarta.servlet.FilterChain
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.efehan.skillmatcherbackend.core.auth.CustomUserDetailsService
import org.efehan.skillmatcherbackend.core.auth.JwtService
import org.efehan.skillmatcherbackend.shared.exceptions.InvalidTokenException
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource
import org.springframework.stereotype.Component
import org.springframework.web.filter.OncePerRequestFilter

@Component
class JwtAuthenticationFilter(
    private val jwtService: JwtService,
    private val userDetailsService: CustomUserDetailsService,
) : OncePerRequestFilter() {
    override fun doFilterInternal(
        request: HttpServletRequest,
        response: HttpServletResponse,
        filterChain: FilterChain,
    ) {
        val token = extractBearerToken(request)

        if (token != null && SecurityContextHolder.getContext().authentication == null) {
            try {
                val email = jwtService.getEmail(token)
                val userDetails = userDetailsService.loadUserByUsername(email)

                if (userDetails.isEnabled) {
                    val auth =
                        UsernamePasswordAuthenticationToken(
                            userDetails,
                            null,
                            userDetails.authorities,
                        )
                    auth.details = WebAuthenticationDetailsSource().buildDetails(request)
                    SecurityContextHolder.getContext().authentication = auth
                }
            } catch (_: InvalidTokenException) {
                // Token ungueltig — Request bleibt unauthentifiziert, Spring Security gibt 401
            }
        }

        filterChain.doFilter(request, response)
    }

    private fun extractBearerToken(request: HttpServletRequest): String? {
        val header = request.getHeader("Authorization")
        if (header != null && header.startsWith("Bearer ")) {
            return header.substring(7).ifBlank { null }
        }
        return null
    }
}
