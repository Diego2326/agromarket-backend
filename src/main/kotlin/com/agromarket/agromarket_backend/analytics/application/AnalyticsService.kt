package com.agromarket.agromarket_backend.analytics.application

import com.agromarket.agromarket_backend.analytics.domain.AnalyticsRepository
import com.agromarket.agromarket_backend.analytics.domain.UserAnalytics
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.util.UUID

@Service
class AnalyticsService(
    private val analytics: AnalyticsRepository,
) {
    @Transactional(readOnly = true)
    fun mine(userId: UUID): UserAnalytics = analytics.getForUser(userId)
}
