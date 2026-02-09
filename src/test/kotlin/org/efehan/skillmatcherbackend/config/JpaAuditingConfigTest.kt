package org.efehan.skillmatcherbackend.config

import org.assertj.core.api.Assertions.assertThat
import org.efehan.skillmatcherbackend.core.auth.SecurityUser
import org.efehan.skillmatcherbackend.persistence.RoleModel
import org.efehan.skillmatcherbackend.persistence.UserModel
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.springframework.security.authentication.AnonymousAuthenticationToken
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.security.core.authority.SimpleGrantedAuthority
import org.springframework.security.core.context.SecurityContextHolder

@DisplayName("JpaAuditingConfig Unit Tests")
class JpaAuditingConfigTest {
    private val config = JpaAuditingConfig()

    @BeforeEach
    fun setUp() {
        SecurityContextHolder.clearContext()
    }

    @AfterEach
    fun tearDown() {
        SecurityContextHolder.clearContext()
    }

    @Test
    fun `auditorProvider returns user id when authenticated`() {
        // given
        val user = buildTestUser()
        val securityUser = SecurityUser(user)
        val authentication = UsernamePasswordAuthenticationToken(securityUser, null, securityUser.authorities)
        SecurityContextHolder.getContext().authentication = authentication

        // when
        val result = config.auditorProvider().currentAuditor

        // then
        assertThat(result).isPresent
        assertThat(result.get()).isEqualTo(user.id)
    }

    @Test
    fun `auditorProvider returns SYSTEM when no authentication`() {
        // when
        val result = config.auditorProvider().currentAuditor

        // then
        assertThat(result).isPresent
        assertThat(result.get()).isEqualTo("SYSTEM")
    }

    @Test
    fun `auditorProvider returns SYSTEM when anonymous authentication`() {
        // given
        val authentication =
            AnonymousAuthenticationToken(
                "key",
                "anonymous",
                listOf(SimpleGrantedAuthority("ROLE_ANONYMOUS")),
            )
        SecurityContextHolder.getContext().authentication = authentication

        // when
        val result = config.auditorProvider().currentAuditor

        // then
        assertThat(result).isPresent
        assertThat(result.get()).isEqualTo("SYSTEM")
    }

    private fun buildTestUser(): UserModel {
        val role = RoleModel(name = "EMPLOYER", description = null)
        val user =
            UserModel(
                username = "testuser",
                email = "test@example.com",
                passwordHash = "hashedPassword",
                firstName = "Test",
                lastName = "User",
                role = role,
            )
        user.isEnabled = true
        return user
    }
}
