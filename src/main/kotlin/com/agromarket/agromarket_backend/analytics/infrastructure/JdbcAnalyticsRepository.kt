package com.agromarket.agromarket_backend.analytics.infrastructure

import com.agromarket.agromarket_backend.analytics.domain.AnalyticsRepository
import com.agromarket.agromarket_backend.analytics.domain.UserAnalytics
import org.springframework.jdbc.core.simple.JdbcClient
import org.springframework.stereotype.Repository
import java.math.BigDecimal
import java.util.UUID

@Repository
class JdbcAnalyticsRepository(
    private val jdbc: JdbcClient,
) : AnalyticsRepository {
    override fun getForUser(userId: UUID): UserAnalytics {
        val activePublications = jdbc.sql(
            """
            SELECT COUNT(*) FROM publication p
            JOIN status s ON s.id = p.status_id
            WHERE p.user_owner_id = :userId AND s.name = 'ACTIVE'
            """,
        ).param("userId", userId).query(Long::class.java).single()

        val unitsCommitted = jdbc.sql(
            """
            SELECT COALESCE(SUM(p.units - p.units_available), 0)
            FROM publication p WHERE p.user_owner_id = :userId
            """,
        ).param("userId", userId).query(Long::class.java).single()

        val reservationsMade = count("buyer_id", userId)
        val reservationsReceived = count("provider_id", userId)
        val completedOperations = jdbc.sql(
            """
            SELECT COUNT(*) FROM reservations
            WHERE (buyer_id = :userId OR provider_id = :userId) AND status = 'COMPLETED'
            """,
        ).param("userId", userId).query(Long::class.java).single()

        val completedVolume = jdbc.sql(
            """
            SELECT COALESCE(SUM(quantity * unit_price), 0)
            FROM reservations
            WHERE (buyer_id = :userId OR provider_id = :userId) AND status = 'COMPLETED'
            """,
        ).param("userId", userId).query(BigDecimal::class.java).single()

        val byStatus = jdbc.sql(
            """
            SELECT status, COUNT(*) AS total FROM reservations
            WHERE buyer_id = :userId OR provider_id = :userId
            GROUP BY status ORDER BY status
            """,
        ).param("userId", userId).query { rs, _ -> rs.getString("status") to rs.getLong("total") }.list().toMap()

        return UserAnalytics(
            activePublications,
            reservationsMade,
            reservationsReceived,
            unitsCommitted,
            completedOperations,
            completedVolume,
            byStatus,
        )
    }

    private fun count(column: String, userId: UUID): Long = jdbc.sql(
        "SELECT COUNT(*) FROM reservations WHERE $column = :userId",
    ).param("userId", userId).query(Long::class.java).single()
}
