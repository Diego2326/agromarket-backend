package com.agromarket.agromarket_backend.user.infrastructure.persistence

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.Id
import jakarta.persistence.Table
import java.time.Instant
import java.util.UUID

@Entity
@Table(name = "users")
class UserJpaEntity(
    @Id
    var id: UUID,

    @Column(nullable = false, unique = true, length = 254)
    var email: String,

    @Column(name = "password_hash", nullable = false, length = 100)
    var passwordHash: String,

    @Column(name = "full_name", nullable = false, length = 120)
    var fullName: String,

    @Column(length = 30)
    var phone: String?,

    @Column(length = 160)
    var location: String?,

    @Column(nullable = false)
    var enabled: Boolean,

    @Column(name = "created_at", nullable = false, updatable = false)
    var createdAt: Instant,

    @Column(name = "updated_at", nullable = false)
    var updatedAt: Instant,
)
