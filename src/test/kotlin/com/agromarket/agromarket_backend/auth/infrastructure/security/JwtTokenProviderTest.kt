package com.agromarket.agromarket_backend.auth.infrastructure.security

import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import java.time.Clock
import java.time.Instant
import java.time.ZoneOffset
import java.util.UUID
import kotlin.test.assertEquals

class JwtTokenProviderTest {
    private val userId = UUID.fromString("e1ac107a-f95e-4bb4-a9d6-4407872ed6ae")
    private val issuedAt = Instant.parse("2026-09-30T12:00:00Z")
    private val properties = JwtProperties(
        secret = "a-secure-test-secret-with-more-than-32-bytes",
        issuer = "agromarket-test",
        expirationSeconds = 3600,
    )

    @Test
    fun `issued token can be verified and returns its user id`() {
        val provider = JwtTokenProvider(properties, Clock.fixed(issuedAt, ZoneOffset.UTC))

        val issued = provider.issue(userId)

        assertEquals(userId, provider.verifyAndGetUserId(issued.value))
        assertEquals(issuedAt.plusSeconds(3600), issued.expiresAt)
    }

    @Test
    fun `expired token is rejected`() {
        val issuer = JwtTokenProvider(properties, Clock.fixed(issuedAt, ZoneOffset.UTC))
        val token = issuer.issue(userId).value
        val verifier = JwtTokenProvider(properties, Clock.fixed(issuedAt.plusSeconds(3601), ZoneOffset.UTC))

        assertThrows<InvalidJwtException> { verifier.verifyAndGetUserId(token) }
    }

    @Test
    fun `tampered token is rejected`() {
        val provider = JwtTokenProvider(properties, Clock.fixed(issuedAt, ZoneOffset.UTC))
        val token = provider.issue(userId).value
        val tampered = token.dropLast(1) + if (token.last() == 'a') 'b' else 'a'

        assertThrows<InvalidJwtException> { provider.verifyAndGetUserId(tampered) }
    }
}
