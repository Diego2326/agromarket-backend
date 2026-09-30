package com.agromarket.agromarket_backend.auth.infrastructure.security

import com.agromarket.agromarket_backend.auth.application.IssuedToken
import com.agromarket.agromarket_backend.auth.application.TokenProvider
import io.jsonwebtoken.JwtException
import io.jsonwebtoken.Jwts
import io.jsonwebtoken.security.Keys
import org.springframework.stereotype.Component
import java.nio.charset.StandardCharsets
import java.time.Clock
import java.time.Instant
import java.util.Date
import java.util.UUID
import javax.crypto.SecretKey

@Component
class JwtTokenProvider(
    private val properties: JwtProperties,
    private val clock: Clock,
) : TokenProvider {
    private val key: SecretKey by lazy {
        require(properties.secret.toByteArray(StandardCharsets.UTF_8).size >= 32) {
            "JWT_SECRET debe contener al menos 32 bytes"
        }
        Keys.hmacShaKeyFor(properties.secret.toByteArray(StandardCharsets.UTF_8))
    }

    override fun issue(userId: UUID): IssuedToken {
        val issuedAt = Instant.now(clock)
        val expiresAt = issuedAt.plusSeconds(properties.expirationSeconds)
        val token = Jwts.builder()
            .subject(userId.toString())
            .issuer(properties.issuer)
            .issuedAt(Date.from(issuedAt))
            .expiration(Date.from(expiresAt))
            .signWith(key)
            .compact()
        return IssuedToken(token, expiresAt)
    }

    fun verifyAndGetUserId(token: String): UUID = try {
        val claims = Jwts.parser()
            .verifyWith(key)
            .requireIssuer(properties.issuer)
            .clock { Date.from(Instant.now(clock)) }
            .build()
            .parseSignedClaims(token)
            .payload
        UUID.fromString(claims.subject)
    } catch (exception: IllegalArgumentException) {
        throw InvalidJwtException(exception)
    } catch (exception: JwtException) {
        throw InvalidJwtException(exception)
    }
}

class InvalidJwtException(cause: Throwable) : RuntimeException("Token inválido o vencido", cause)
