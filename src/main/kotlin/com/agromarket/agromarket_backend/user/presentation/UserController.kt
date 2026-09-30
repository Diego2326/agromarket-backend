package com.agromarket.agromarket_backend.user.presentation

import com.agromarket.agromarket_backend.auth.infrastructure.security.AuthenticatedUser
import com.agromarket.agromarket_backend.user.application.UpdateProfileCommand
import com.agromarket.agromarket_backend.user.application.UserService
import jakarta.validation.Valid
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PatchMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/users")
class UserController(
    private val userService: UserService,
) {
    @GetMapping("/me")
    fun current(@AuthenticationPrincipal principal: AuthenticatedUser): UserResponse =
        UserResponse.from(userService.getCurrent(principal.id))

    @PatchMapping("/me")
    fun updateCurrent(
        @AuthenticationPrincipal principal: AuthenticatedUser,
        @Valid @RequestBody request: UpdateProfileRequest,
    ): UserResponse = UserResponse.from(
        userService.updateCurrent(
            principal.id,
            UpdateProfileCommand(request.fullName, request.phone, request.location),
        ),
    )
}
