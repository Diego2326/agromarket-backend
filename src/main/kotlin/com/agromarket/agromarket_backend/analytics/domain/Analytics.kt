package com.agromarket.agromarket_backend.analytics.domain

import java.math.BigDecimal
import java.util.UUID

data class UserAnalytics(
    val activePublications: Long,
    val reservationsMade: Long,
    val reservationsReceived: Long,
    val unitsCommitted: Long,
    val completedOperations: Long,
    val completedVolume: BigDecimal,
    val reservationsByStatus: Map<String, Long>,
)

interface AnalyticsRepository {
    fun getForUser(userId: UUID): UserAnalytics
}
