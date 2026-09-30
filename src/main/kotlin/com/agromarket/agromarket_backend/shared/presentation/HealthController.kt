package com.agromarket.agromarket_backend.shared.presentation

import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RestController
import java.time.Clock
import java.time.Instant

@RestController
class HealthController(
    private val clock: Clock,
) {
    @GetMapping("/api/health")
    fun health() = HealthResponse(status = "UP", timestamp = Instant.now(clock))
}

data class HealthResponse(
    val status: String,
    val timestamp: Instant,
)
