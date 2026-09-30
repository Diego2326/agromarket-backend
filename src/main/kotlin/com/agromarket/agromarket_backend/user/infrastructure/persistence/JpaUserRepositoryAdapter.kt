package com.agromarket.agromarket_backend.user.infrastructure.persistence

import com.agromarket.agromarket_backend.user.domain.User
import com.agromarket.agromarket_backend.user.domain.UserRepository
import org.springframework.stereotype.Repository
import java.util.UUID

@Repository
class JpaUserRepositoryAdapter(
    private val repository: SpringDataUserRepository,
) : UserRepository {
    override fun existsByEmail(email: String): Boolean = repository.existsByEmail(email)

    override fun findByEmail(email: String): User? = repository.findByEmail(email)?.toDomain()

    override fun findById(id: UUID): User? = repository.findById(id).orElse(null)?.toDomain()

    override fun save(user: User): User = repository.saveAndFlush(user.toEntity()).toDomain()

    private fun UserJpaEntity.toDomain() = User(
        id = id,
        email = email,
        passwordHash = passwordHash,
        fullName = fullName,
        phone = phone,
        location = location,
        enabled = enabled,
        createdAt = createdAt,
        updatedAt = updatedAt,
    )

    private fun User.toEntity() = UserJpaEntity(
        id = id,
        email = email,
        passwordHash = passwordHash,
        fullName = fullName,
        phone = phone,
        location = location,
        enabled = enabled,
        createdAt = createdAt,
        updatedAt = updatedAt,
    )
}
