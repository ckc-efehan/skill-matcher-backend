package org.efehan.skillmatcherbackend.config.properties

import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.core.io.Resource

@ConfigurationProperties(prefix = "jwt")
data class JwtProperties(
    val publicKey: Resource,
    val privateKey: Resource,
    val accessTokenExpiration: Long,
    val refreshTokenExpiration: Long,
    val issuer: String,
)
