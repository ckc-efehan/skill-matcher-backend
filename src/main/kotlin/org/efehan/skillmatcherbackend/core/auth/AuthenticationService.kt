package org.efehan.skillmatcherbackend.core.auth

import jakarta.transaction.Transactional
import org.efehan.skillmatcherbackend.config.properties.JwtProperties
import org.efehan.skillmatcherbackend.exception.GlobalErrorCode
import org.efehan.skillmatcherbackend.persistence.RefreshTokenModel
import org.efehan.skillmatcherbackend.persistence.RefreshTokenRepository
import org.efehan.skillmatcherbackend.persistence.UserModel
import org.efehan.skillmatcherbackend.persistence.UserRepository
import org.efehan.skillmatcherbackend.shared.exceptions.AccountDisabledException
import org.efehan.skillmatcherbackend.shared.exceptions.EntryNotFoundException
import org.efehan.skillmatcherbackend.shared.exceptions.InvalidCredentialsException
import org.efehan.skillmatcherbackend.shared.exceptions.InvalidTokenException
import org.springframework.http.HttpStatus
import org.springframework.security.authentication.AuthenticationManager
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.stereotype.Service
import java.time.Clock
import java.time.Instant
import java.time.temporal.ChronoUnit

private const val REFRESH_ROTATION_THRESHOLD_DAYS = 2L

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

    fun refreshToken(token: String): AuthResponse {
        val existingToken =
            refreshTokenRepository.findByToken(token) ?: throw EntryNotFoundException(
                resource = "RefreshToken",
                field = "token",
                value = token,
                errorCode = GlobalErrorCode.REFRESH_TOKEN_NOT_FOUND,
                status = HttpStatus.BAD_REQUEST,
            )

        if (existingToken.revoked || existingToken.expiresAt.isBefore(Instant.now(clock))) {
            throw InvalidTokenException(
                message = "Refresh token is expired or invalid",
                errorCode = GlobalErrorCode.INVALID_REFRESH_TOKEN,
            )
        }

        val user = existingToken.user
        val accessToken = jwtService.generateAccessToken(user)
        val daysRemaining = ChronoUnit.DAYS.between(Instant.now(clock), existingToken.expiresAt)

        val refreshToken =
            if (daysRemaining < REFRESH_ROTATION_THRESHOLD_DAYS) {
                rotateRefreshToken(existingToken)
            } else {
                existingToken.token
            }

        return AuthResponse(
            accessToken = accessToken,
            refreshToken = refreshToken,
            tokenType = "Bearer",
            expiresIn = jwtProperties.accessTokenExpiration,
            user = user.toAuthUserResponse(),
        )
    }

    private fun rotateRefreshToken(oldToken: RefreshTokenModel): String {
        oldToken.revoked = true

        val newToken = jwtService.generateOpaqueRefreshToken()
        refreshTokenRepository.save(
            RefreshTokenModel(
                token = newToken,
                user = oldToken.user,
                expiresAt = Instant.now(clock).plusMillis(jwtProperties.refreshTokenExpiration),
            ),
        )
        return newToken
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
