package com.agromarket.agromarket_backend.auth.application

import com.agromarket.agromarket_backend.shared.domain.DisabledUserException
import com.agromarket.agromarket_backend.shared.domain.EmailAlreadyRegisteredException
import com.agromarket.agromarket_backend.shared.domain.InvalidCredentialsException
import com.agromarket.agromarket_backend.user.domain.User
import com.agromarket.agromarket_backend.user.domain.UserRepository
import org.springframework.dao.DataIntegrityViolationException
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.Clock
import java.time.Instant
import java.util.UUID

@Service
class AuthService(
    private val users: UserRepository,
    private val passwordHasher: PasswordHasher,
    private val tokenProvider: TokenProvider,
    private val clock: Clock,
) {
    @Transactional
    fun register(command: RegisterCommand): AuthResult {
        val normalizedEmail = command.email.trim().lowercase()
        if (users.existsByEmail(normalizedEmail)) throw EmailAlreadyRegisteredException()

        val now = Instant.now(clock)
        val user = User(
            id = UUID.randomUUID(),
            email = normalizedEmail,
            passwordHash = passwordHasher.hash(command.password),
            fullName = command.fullName.trim(),
            phone = command.phone.cleanNullable(),
            location = command.location.cleanNullable(),
            enabled = true,
            createdAt = now,
            updatedAt = now,
        )

        val saved = try {
            users.save(user)
        } catch (_: DataIntegrityViolationException) {
            throw EmailAlreadyRegisteredException()
        }
        return authenticated(saved)
    }

    @Transactional(readOnly = true)
    fun login(command: LoginCommand): AuthResult {
        val user = users.findByEmail(command.email.trim().lowercase())
            ?: throw InvalidCredentialsException()
        if (!passwordHasher.matches(command.password, user.passwordHash)) throw InvalidCredentialsException()
        if (!user.enabled) throw DisabledUserException()
        return authenticated(user)
    }

    private fun authenticated(user: User): AuthResult {
        val token = tokenProvider.issue(user.id)
        return AuthResult(user, token.value, token.expiresAt)
    }

    private fun String?.cleanNullable(): String? = this?.trim()?.takeIf { it.isNotEmpty() }
}

data class RegisterCommand(
    val email: String,
    val password: String,
    val fullName: String,
    val phone: String?,
    val location: String?,
)

data class LoginCommand(val email: String, val password: String)

data class AuthResult(val user: User, val accessToken: String, val expiresAt: Instant)
