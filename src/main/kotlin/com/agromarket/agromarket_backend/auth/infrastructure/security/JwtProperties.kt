package com.agromarket.agromarket_backend.auth.infrastructure.security

import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Positive
import jakarta.validation.constraints.Size
import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.validation.annotation.Validated

@ConfigurationProperties("app.security.jwt")
@Validated
data class JwtProperties(
    @field:Size(min = 32, message = "JWT_SECRET debe contener al menos 32 caracteres")
    val secret: String,

    @field:NotBlank(message = "JWT_ISSUER es obligatorio")
    val issuer: String,

    @field:Positive(message = "JWT_EXPIRATION_SECONDS debe ser positivo")
    val expirationSeconds: Long,
)
