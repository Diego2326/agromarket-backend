package com.agromarket.agromarket_backend.auth.presentation

import com.agromarket.agromarket_backend.auth.application.AuthService
import com.agromarket.agromarket_backend.auth.application.LoginCommand
import com.agromarket.agromarket_backend.auth.application.RegisterCommand
import jakarta.validation.Valid
import org.springframework.http.HttpStatus
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.ResponseStatus
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/auth")
class AuthController(
    private val authService: AuthService,
) {
    @PostMapping("/register")
    @ResponseStatus(HttpStatus.CREATED)
    fun register(@Valid @RequestBody request: RegisterRequest): AuthResponse = AuthResponse.from(
        authService.register(
            RegisterCommand(
                email = request.email,
                password = request.password,
                fullName = request.fullName,
                phone = request.phone,
                location = request.location,
            ),
        ),
    )

    @PostMapping("/login")
    fun login(@Valid @RequestBody request: LoginRequest): AuthResponse = AuthResponse.from(
        authService.login(LoginCommand(request.email, request.password)),
    )
}
