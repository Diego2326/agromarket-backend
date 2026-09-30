package com.agromarket.agromarket_backend.analytics.presentation

import com.agromarket.agromarket_backend.analytics.application.AnalyticsService
import com.agromarket.agromarket_backend.analytics.domain.UserAnalytics
import com.agromarket.agromarket_backend.auth.infrastructure.security.AuthenticatedUser
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/analytics")
class AnalyticsController(
    private val analyticsService: AnalyticsService,
) {
    @GetMapping("/me")
    fun mine(@AuthenticationPrincipal principal: AuthenticatedUser): UserAnalytics =
        analyticsService.mine(principal.id)
}
