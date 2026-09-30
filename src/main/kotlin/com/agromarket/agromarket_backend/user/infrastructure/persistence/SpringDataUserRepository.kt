package com.agromarket.agromarket_backend.user.infrastructure.persistence

import org.springframework.data.jpa.repository.JpaRepository
import java.util.UUID

interface SpringDataUserRepository : JpaRepository<UserJpaEntity, UUID> {
    fun existsByEmail(email: String): Boolean
    fun findByEmail(email: String): UserJpaEntity?
}
