package org.efehan.skillmatcherbackend.core.auth

import jakarta.transaction.Transactional
import org.efehan.skillmatcherbackend.config.properties.JwtProperties
import org.efehan.skillmatcherbackend.exception.GlobalErrorCode
import org.efehan.skillmatcherbackend.persistence.RefreshTokenModel
import org.efehan.skillmatcherbackend.persistence.RefreshTokenRepository
import org.efehan.skillmatcherbackend.persistence.UserModel
import org.efehan.skillmatcherbackend.persistence.UserRepository
import org.efehan.skillmatcherbackend.shared.exceptions.AccountDisabledException
import org.efehan.skillmatcherbackend.shared.exceptions.InvalidCredentialsException
import org.springframework.http.HttpStatus
import org.springframework.security.authentication.AuthenticationManager
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.stereotype.Service
import java.time.Clock
import java.time.Instant

@Service
@Transactional
class AuthenticationService(
    private val userRepository: UserRepository,
    private val authenticationManager: AuthenticationManager,
    private val jwtService: JwtService,
    private val refreshTokenRepository: RefreshTokenRepository,
    private val jwtProperties: JwtProperties,
    private val clock: Clock = Clock.systemUTC(),
) {
    fun login(
        email: String,
        password: String,
    ): AuthResponse {
        val user =
            userRepository.findByEmail(email) ?: throw InvalidCredentialsException(
                errorCode = GlobalErrorCode.BAD_CREDENTIALS,
                status = HttpStatus.UNAUTHORIZED,
            )

        if (!user.isEnabled) {
            throw AccountDisabledException(
                errorCode = GlobalErrorCode.ACCOUNT_DISABLED,
                status = HttpStatus.FORBIDDEN,
            )
        }

        authenticationManager.authenticate(
            UsernamePasswordAuthenticationToken(email, password),
        )

        val accessToken = jwtService.generateAccessToken(user)

        val refreshToken = jwtService.generateOpaqueRefreshToken()

        val refreshTokenExpiration = Instant.now(clock).plusMillis(jwtProperties.refreshTokenExpiration)
        val accessTokenExpiration = jwtProperties.accessTokenExpiration

        val refreshTokenModel =
            RefreshTokenModel(
                token = refreshToken,
                user = user,
                expiresAt = refreshTokenExpiration,
                revoked = false,
            )

        refreshTokenRepository.save(refreshTokenModel)

        return AuthResponse(
            accessToken = accessToken,
            refreshToken = refreshToken,
            tokenType = "Bearer",
            expiresIn = accessTokenExpiration,
            user = user.toAuthUserResponse(),
        )
    }
}

fun UserModel.toAuthUserResponse() =
    AuthUserResponse(
        id = id,
        email = email,
        firstName = firstName,
        lastName = lastName,
        role = role.name,
    )
