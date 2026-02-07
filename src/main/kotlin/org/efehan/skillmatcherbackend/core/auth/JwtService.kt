package org.efehan.skillmatcherbackend.core.auth

import io.jsonwebtoken.Claims
import io.jsonwebtoken.JwtException
import io.jsonwebtoken.JwtParser
import io.jsonwebtoken.Jwts
import org.efehan.skillmatcherbackend.config.properties.JwtProperties
import org.efehan.skillmatcherbackend.exception.InvalidTokenException
import org.efehan.skillmatcherbackend.persistence.UserModel
import org.springframework.stereotype.Service
import java.security.KeyFactory
import java.security.interfaces.RSAPrivateKey
import java.security.interfaces.RSAPublicKey
import java.security.spec.PKCS8EncodedKeySpec
import java.security.spec.X509EncodedKeySpec
import java.time.Clock
import java.util.Base64
import java.util.Date
import java.util.UUID

@Service
class JwtService(
    private val jwtProperties: JwtProperties,
    private val clock: Clock = Clock.systemUTC(),
) {
    private val privateKey: RSAPrivateKey by lazy { loadPrivateKey() }
    private val publicKey: RSAPublicKey by lazy { loadPublicKey() }

    private val jwtParser: JwtParser by lazy {
        Jwts
            .parser()
            .verifyWith(publicKey)
            .requireIssuer(jwtProperties.issuer)
            .clock { Date.from(clock.instant()) }
            .build()
    }

    fun generateAccessToken(user: UserModel): String {
        val now = clock.instant()
        return Jwts
            .builder()
            .subject(user.email)
            .claim("userId", user.id)
            .claim("role", user.role.name)
            .issuer(jwtProperties.issuer)
            .issuedAt(Date.from(now))
            .expiration(Date.from(now.plusMillis(jwtProperties.accessTokenExpiration)))
            .signWith(privateKey, Jwts.SIG.RS256)
            .compact()
    }

    fun generateOpaqueRefreshToken(): String = UUID.randomUUID().toString()

    fun validateToken(token: String): Claims =
        try {
            jwtParser.parseSignedClaims(token).payload
        } catch (ex: JwtException) {
            throw InvalidTokenException("Invalid JWT", ex)
        }

    fun getEmail(token: String): String = validateToken(token).subject

    private fun loadPrivateKey(): RSAPrivateKey {
        val keyBytes =
            jwtProperties.privateKey.inputStream.use { stream ->
                String(stream.readAllBytes())
                    .replace("-----BEGIN PRIVATE KEY-----", "")
                    .replace("-----END PRIVATE KEY-----", "")
                    .replace("\\s".toRegex(), "")
                    .let { Base64.getDecoder().decode(it) }
            }
        val keySpec = PKCS8EncodedKeySpec(keyBytes)
        return KeyFactory.getInstance("RSA").generatePrivate(keySpec) as RSAPrivateKey
    }

    private fun loadPublicKey(): RSAPublicKey {
        val keyBytes =
            jwtProperties.publicKey.inputStream.use { stream ->
                String(stream.readAllBytes())
                    .replace("-----BEGIN PUBLIC KEY-----", "")
                    .replace("-----END PUBLIC KEY-----", "")
                    .replace("\\s".toRegex(), "")
                    .let { Base64.getDecoder().decode(it) }
            }
        val keySpec = X509EncodedKeySpec(keyBytes)
        return KeyFactory.getInstance("RSA").generatePublic(keySpec) as RSAPublicKey
    }
}
