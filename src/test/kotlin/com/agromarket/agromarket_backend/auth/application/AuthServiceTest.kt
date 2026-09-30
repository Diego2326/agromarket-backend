package com.agromarket.agromarket_backend.auth.application

import com.agromarket.agromarket_backend.shared.domain.EmailAlreadyRegisteredException
import com.agromarket.agromarket_backend.shared.domain.InvalidCredentialsException
import com.agromarket.agromarket_backend.user.domain.User
import com.agromarket.agromarket_backend.user.domain.UserRepository
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import java.time.Clock
import java.time.Instant
import java.time.ZoneOffset
import java.util.UUID
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals

class AuthServiceTest {
    private val now = Instant.parse("2026-09-30T12:00:00Z")
    private val repository = InMemoryUserRepository()
    private val service = AuthService(
        users = repository,
        passwordHasher = FakePasswordHasher(),
        tokenProvider = FakeTokenProvider(now.plusSeconds(3600)),
        clock = Clock.fixed(now, ZoneOffset.UTC),
    )

    @Test
    fun `register normalizes email hashes password and returns a token`() {
        val result = service.register(
            RegisterCommand(
                email = "  PRODUCER@Example.COM  ",
                password = "secure-password",
                fullName = "  Ana Productora  ",
                phone = " 5555-1234 ",
                location = " Guatemala ",
            ),
        )

        assertEquals("producer@example.com", result.user.email)
        assertEquals("Ana Productora", result.user.fullName)
        assertEquals("hashed:secure-password", result.user.passwordHash)
        assertNotEquals("secure-password", result.user.passwordHash)
        assertEquals("token:${result.user.id}", result.accessToken)
        assertEquals(now.plusSeconds(3600), result.expiresAt)
    }

    @Test
    fun `register rejects a duplicated normalized email`() {
        service.register(RegisterCommand("user@example.com", "password1", "User", null, null))

        assertThrows<EmailAlreadyRegisteredException> {
            service.register(RegisterCommand("USER@example.com", "password2", "Other", null, null))
        }
    }

    @Test
    fun `login rejects an incorrect password without revealing whether email exists`() {
        service.register(RegisterCommand("user@example.com", "correct-password", "User", null, null))

        assertThrows<InvalidCredentialsException> {
            service.login(LoginCommand("user@example.com", "incorrect-password"))
        }
        assertThrows<InvalidCredentialsException> {
            service.login(LoginCommand("missing@example.com", "incorrect-password"))
        }
    }
}

private class FakePasswordHasher : PasswordHasher {
    override fun hash(rawPassword: String): String = "hashed:$rawPassword"
    override fun matches(rawPassword: String, passwordHash: String): Boolean = passwordHash == hash(rawPassword)
}

private class FakeTokenProvider(private val expiration: Instant) : TokenProvider {
    override fun issue(userId: UUID) = IssuedToken("token:$userId", expiration)
}

private class InMemoryUserRepository : UserRepository {
    private val values = mutableMapOf<UUID, User>()

    override fun existsByEmail(email: String): Boolean = values.values.any { it.email == email }
    override fun findByEmail(email: String): User? = values.values.find { it.email == email }
    override fun findById(id: UUID): User? = values[id]
    override fun save(user: User): User = user.also { values[it.id] = it }
}
