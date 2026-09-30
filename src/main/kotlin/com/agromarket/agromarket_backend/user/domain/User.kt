package com.agromarket.agromarket_backend.user.domain

import java.time.Instant
import java.util.UUID

data class User(
    val id: UUID,
    val email: String,
    val passwordHash: String,
    val fullName: String,
    val phone: String?,
    val location: String?,
    val enabled: Boolean,
    val createdAt: Instant,
    val updatedAt: Instant,
)
