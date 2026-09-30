package com.agromarket.agromarket_backend.reservation.domain

import java.util.UUID

interface ReservationRepository {
    fun save(reservation: Reservation): Reservation
    fun saveHistory(history: ReservationHistory)
    fun findById(id: UUID): Reservation?
    fun findByIdForUpdate(id: UUID): Reservation?
    fun findByBuyerId(buyerId: UUID): List<Reservation>
    fun findByProviderId(providerId: UUID): List<Reservation>
}
