package com.agromarket.agromarket_backend.reservation.domain

import java.math.BigDecimal
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.UUID

data class Reservation(
    val id: UUID,
    val publicationId: UUID,
    val publicationTitle: String,
    val productId: Int,
    val productName: String,
    val buyerId: UUID,
    val buyerName: String,
    val providerId: UUID,
    val providerName: String,
    val quantity: Int,
    val unitPrice: BigDecimal,
    val currencyCode: String,
    val expectedAvailableDate: LocalDate?,
    val status: ReservationStatus,
    val stockReleased: Boolean,
    val createdAt: LocalDateTime,
    val updatedAt: LocalDateTime,
    val version: Long = 0,
) {
    val total: BigDecimal
        get() = unitPrice.multiply(quantity.toBigDecimal())
}

data class ReservationHistory(
    val id: Long = 0,
    val reservationId: UUID,
    val fromStatus: ReservationStatus?,
    val toStatus: ReservationStatus,
    val actorId: UUID,
    val createdAt: LocalDateTime,
)

enum class ReservationStatus {
    PENDING_PROVIDER,
    ACCEPTED_PENDING_PAYMENT,
    CONFIRMED,
    READY_FOR_DELIVERY,
    DELIVERED,
    COMPLETED,
    REJECTED,
    CANCELLED,
}
