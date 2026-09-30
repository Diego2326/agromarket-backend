package com.agromarket.agromarket_backend.user.domain

import java.util.UUID

interface UserRepository {
    fun existsByEmail(email: String): Boolean
    fun findByEmail(email: String): User?
    fun findById(id: UUID): User?
    fun save(user: User): User
}
