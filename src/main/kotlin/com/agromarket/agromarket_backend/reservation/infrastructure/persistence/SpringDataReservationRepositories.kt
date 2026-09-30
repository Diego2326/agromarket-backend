package com.agromarket.agromarket_backend.reservation.infrastructure.persistence

import jakarta.persistence.LockModeType
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Lock
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import java.util.UUID

interface SpringDataReservationRepository : JpaRepository<ReservationJpaEntity, UUID> {
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT r FROM ReservationJpaEntity r WHERE r.id = :id")
    fun findByIdForUpdate(@Param("id") id: UUID): ReservationJpaEntity?

    fun findAllByBuyerIdOrderByCreatedAtDesc(buyerId: UUID): List<ReservationJpaEntity>
    fun findAllByProviderIdOrderByCreatedAtDesc(providerId: UUID): List<ReservationJpaEntity>
}

interface SpringDataReservationHistoryRepository : JpaRepository<ReservationHistoryJpaEntity, Long>
