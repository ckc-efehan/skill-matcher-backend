package org.efehan.skillmatcherbackend.service

import io.mockk.every
import io.mockk.impl.annotations.MockK
import io.mockk.junit5.MockKExtension
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.efehan.skillmatcherbackend.config.properties.JwtProperties
import org.efehan.skillmatcherbackend.core.auth.AuthenticationService
import org.efehan.skillmatcherbackend.core.auth.JwtService
import org.efehan.skillmatcherbackend.exception.GlobalErrorCode
import org.efehan.skillmatcherbackend.persistence.RefreshTokenModel
import org.efehan.skillmatcherbackend.persistence.RefreshTokenRepository
import org.efehan.skillmatcherbackend.persistence.RoleModel
import org.efehan.skillmatcherbackend.persistence.UserModel
import org.efehan.skillmatcherbackend.persistence.UserRepository
import org.efehan.skillmatcherbackend.shared.exceptions.AccountDisabledException
import org.efehan.skillmatcherbackend.shared.exceptions.InvalidCredentialsException
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.springframework.security.authentication.AuthenticationManager
import org.springframework.security.authentication.BadCredentialsException
import java.time.Clock
import java.time.Instant
import java.time.ZoneOffset

@ExtendWith(MockKExtension::class)
@DisplayName("Authentication Service Unit Tests")
class AuthenticationServiceTest {
    @MockK
    private lateinit var userRepository: UserRepository

    @MockK
    private lateinit var authenticationManager: AuthenticationManager

    @MockK
    private lateinit var jwtService: JwtService

    @MockK
    private lateinit var refreshTokenRepository: RefreshTokenRepository

    @MockK
    private lateinit var jwtProperties: JwtProperties

    private lateinit var authenticationService: AuthenticationService

    companion object {
        private const val EMAIL = "test@example.com"
        private const val PASSWORD = "Secret-password1"
        private const val ACCESS_TOKEN = "access-token-jwt"
        private const val REFRESH_TOKEN = "refresh-token-uuid"
        private const val ACCESS_TOKEN_EXPIRATION = 900_000L
        private const val REFRESH_TOKEN_EXPIRATION = 604_800_000L
        private val FIXED_INSTANT: Instant = Instant.parse("2025-01-01T12:00:00Z")
    }

    private val fixedClock = Clock.fixed(FIXED_INSTANT, ZoneOffset.UTC)

    @BeforeEach
    fun setUp() {
        every { jwtProperties.accessTokenExpiration } returns ACCESS_TOKEN_EXPIRATION
        every { jwtProperties.refreshTokenExpiration } returns REFRESH_TOKEN_EXPIRATION

        authenticationService =
            AuthenticationService(
                userRepository = userRepository,
                authenticationManager = authenticationManager,
                jwtService = jwtService,
                refreshTokenRepository = refreshTokenRepository,
                jwtProperties = jwtProperties,
                clock = fixedClock,
            )
    }

    private fun buildTestUser(
        email: String = EMAIL,
        isEnabled: Boolean = true,
    ): UserModel {
        val role = RoleModel("ADMIN", null)
        val user =
            UserModel(
                username = "testuser",
                email = email,
                passwordHash = "hashed",
                firstName = "Test",
                lastName = "User",
                role = role,
            )
        user.isEnabled = isEnabled
        return user
    }

    @Test
    fun `login successfully with correct credentials`() {
        // given
        val user = buildTestUser()

        every { userRepository.findByEmail(EMAIL) } returns user
        every { authenticationManager.authenticate(any()) } returns mockk()
        every { jwtService.generateAccessToken(user) } returns ACCESS_TOKEN
        every { jwtService.generateOpaqueRefreshToken() } returns REFRESH_TOKEN
        every { refreshTokenRepository.save(any()) } returnsArgument 0

        // when
        val result = authenticationService.login(EMAIL, PASSWORD)

        // then
        assertThat(result.accessToken).isEqualTo(ACCESS_TOKEN)
        assertThat(result.refreshToken).isEqualTo(REFRESH_TOKEN)
        assertThat(result.tokenType).isEqualTo("Bearer")
        assertThat(result.expiresIn).isEqualTo(ACCESS_TOKEN_EXPIRATION)
        assertThat(result.user.email).isEqualTo(EMAIL)
        assertThat(result.user.firstName).isEqualTo("Test")
        assertThat(result.user.lastName).isEqualTo("User")
        assertThat(result.user.role).isEqualTo("ADMIN")
    }

    @Test
    fun `login saves refresh token to database with correct values`() {
        // given
        val user = buildTestUser()
        val tokenSlot = slot<RefreshTokenModel>()

        every { userRepository.findByEmail(EMAIL) } returns user
        every { authenticationManager.authenticate(any()) } returns mockk()
        every { jwtService.generateAccessToken(user) } returns ACCESS_TOKEN
        every { jwtService.generateOpaqueRefreshToken() } returns REFRESH_TOKEN
        every { refreshTokenRepository.save(capture(tokenSlot)) } returnsArgument 0

        // when
        authenticationService.login(EMAIL, PASSWORD)

        // then
        val saved = tokenSlot.captured
        assertThat(saved.token).isEqualTo(REFRESH_TOKEN)
        assertThat(saved.user).isEqualTo(user)
        assertThat(saved.expiresAt).isEqualTo(FIXED_INSTANT.plusMillis(REFRESH_TOKEN_EXPIRATION))
        assertThat(saved.revoked).isFalse()
    }

    @Test
    fun `login calls authenticationManager with correct credentials`() {
        // given
        val user = buildTestUser()

        every { userRepository.findByEmail(EMAIL) } returns user
        every { authenticationManager.authenticate(any()) } returns mockk()
        every { jwtService.generateAccessToken(user) } returns ACCESS_TOKEN
        every { jwtService.generateOpaqueRefreshToken() } returns REFRESH_TOKEN
        every { refreshTokenRepository.save(any()) } returnsArgument 0

        // when
        authenticationService.login(EMAIL, PASSWORD)

        // then
        verify {
            authenticationManager.authenticate(
                withArg {
                    assertThat(it.principal).isEqualTo(EMAIL)
                    assertThat(it.credentials).isEqualTo(PASSWORD)
                },
            )
        }
    }

    @Test
    fun `login throws InvalidCredentialsException when user not found`() {
        // given
        every { userRepository.findByEmail(EMAIL) } returns null

        // then
        assertThatThrownBy {
            authenticationService.login(EMAIL, PASSWORD)
        }.isInstanceOf(InvalidCredentialsException::class.java)
            .satisfies({ ex ->
                val e = ex as InvalidCredentialsException
                assertThat(e.errorCode).isEqualTo(GlobalErrorCode.BAD_CREDENTIALS)
                assertThat(e.status).isEqualTo(org.springframework.http.HttpStatus.UNAUTHORIZED)
            })

        verify(exactly = 0) { authenticationManager.authenticate(any()) }
        verify(exactly = 0) { jwtService.generateAccessToken(any()) }
    }

    @Test
    fun `login throws AccountDisabledException when user is disabled`() {
        // given
        val user = buildTestUser(isEnabled = false)

        every { userRepository.findByEmail(EMAIL) } returns user

        // then
        assertThatThrownBy {
            authenticationService.login(EMAIL, PASSWORD)
        }.isInstanceOf(AccountDisabledException::class.java)
            .satisfies({ ex ->
                val e = ex as AccountDisabledException
                assertThat(e.errorCode).isEqualTo(GlobalErrorCode.ACCOUNT_DISABLED)
            })

        verify(exactly = 0) { authenticationManager.authenticate(any()) }
        verify(exactly = 0) { jwtService.generateAccessToken(any()) }
    }

    @Test
    fun `login throws when authenticationManager rejects credentials`() {
        // given
        val user = buildTestUser()

        every { userRepository.findByEmail(EMAIL) } returns user
        every { authenticationManager.authenticate(any()) } throws BadCredentialsException("Bad credentials")

        // then
        assertThatThrownBy {
            authenticationService.login(EMAIL, PASSWORD)
        }.isInstanceOf(BadCredentialsException::class.java)

        verify(exactly = 0) { jwtService.generateAccessToken(any()) }
    }
}
