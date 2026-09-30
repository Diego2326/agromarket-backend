package com.agromarket.agromarket_backend.user.presentation

import com.agromarket.agromarket_backend.user.domain.User
import jakarta.validation.constraints.Pattern
import jakarta.validation.constraints.Size
import java.time.Instant
import java.util.UUID

data class UpdateProfileRequest(
    @field:Size(max = 120, message = "El nombre no puede superar 120 caracteres")
    @field:Pattern(regexp = ".*\\S.*", message = "El nombre no puede estar vacío")
    val fullName: String? = null,

    @field:Size(max = 30, message = "El teléfono no puede superar 30 caracteres")
    val phone: String? = null,

    @field:Size(max = 160, message = "La ubicación no puede superar 160 caracteres")
    val location: String? = null,
)

data class UserResponse(
    val id: UUID,
    val email: String,
    val fullName: String,
    val phone: String?,
    val location: String?,
    val enabled: Boolean,
    val createdAt: Instant,
    val updatedAt: Instant,
) {
    companion object {
        fun from(user: User) = UserResponse(
            id = user.id,
            email = user.email,
            fullName = user.fullName,
            phone = user.phone,
            location = user.location,
            enabled = user.enabled,
            createdAt = user.createdAt,
            updatedAt = user.updatedAt,
        )
    }
}
