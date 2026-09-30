package com.agromarket.agromarket_backend.auth.infrastructure.security

import com.agromarket.agromarket_backend.auth.application.PasswordHasher
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.stereotype.Component

@Component
class BCryptPasswordHasher(
    private val encoder: PasswordEncoder,
) : PasswordHasher {
    override fun hash(rawPassword: String): String = requireNotNull(encoder.encode(rawPassword))

    override fun matches(rawPassword: String, passwordHash: String): Boolean =
        encoder.matches(rawPassword, passwordHash)
}
