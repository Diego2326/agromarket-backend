package com.agromarket.agromarket_backend.auth.presentation

import com.agromarket.agromarket_backend.auth.application.AuthResult
import com.agromarket.agromarket_backend.user.presentation.UserResponse
import jakarta.validation.constraints.Email
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Size
import java.time.Instant

data class RegisterRequest(
    @field:NotBlank(message = "El correo electrónico es obligatorio")
    @field:Email(message = "El correo electrónico no es válido")
    @field:Size(max = 254, message = "El correo electrónico no puede superar 254 caracteres")
    val email: String,

    @field:NotBlank(message = "La contraseña es obligatoria")
    @field:Size(min = 8, max = 72, message = "La contraseña debe tener entre 8 y 72 caracteres")
    val password: String,

    @field:NotBlank(message = "El nombre es obligatorio")
    @field:Size(max = 120, message = "El nombre no puede superar 120 caracteres")
    val fullName: String,

    @field:Size(max = 30, message = "El teléfono no puede superar 30 caracteres")
    val phone: String? = null,

    @field:Size(max = 160, message = "La ubicación no puede superar 160 caracteres")
    val location: String? = null,
)

data class LoginRequest(
    @field:NotBlank(message = "El correo electrónico es obligatorio")
    @field:Email(message = "El correo electrónico no es válido")
    val email: String,

    @field:NotBlank(message = "La contraseña es obligatoria")
    val password: String,
)

data class AuthResponse(
    val accessToken: String,
    val tokenType: String = "Bearer",
    val expiresAt: Instant,
    val user: UserResponse,
) {
    companion object {
        fun from(result: AuthResult) = AuthResponse(
            accessToken = result.accessToken,
            expiresAt = result.expiresAt,
            user = UserResponse.from(result.user),
        )
    }
}
