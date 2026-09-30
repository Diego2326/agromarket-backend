package com.agromarket.agromarket_backend.auth.application

import java.time.Instant
import java.util.UUID

interface PasswordHasher {
    fun hash(rawPassword: String): String
    fun matches(rawPassword: String, passwordHash: String): Boolean
}

interface TokenProvider {
    fun issue(userId: UUID): IssuedToken
}

data class IssuedToken(
    val value: String,
    val expiresAt: Instant,
)
