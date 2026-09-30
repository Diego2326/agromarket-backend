package com.agromarket.agromarket_backend.reservation.infrastructure.persistence

import com.agromarket.agromarket_backend.publication.infrastructure.persistence.PublicationJpaEntity
import com.agromarket.agromarket_backend.reservation.domain.ReservationStatus
import com.agromarket.agromarket_backend.user.infrastructure.persistence.UserJpaEntity
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.FetchType
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.JoinColumn
import jakarta.persistence.ManyToOne
import jakarta.persistence.Table
import jakarta.persistence.Version
import java.math.BigDecimal
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.UUID

@Entity
@Table(name = "reservations")
class ReservationJpaEntity(
    @Id
    var id: UUID,

    @ManyToOne(fetch = FetchType.EAGER, optional = false)
    @JoinColumn(name = "publication_id", nullable = false)
    var publication: PublicationJpaEntity,

    @ManyToOne(fetch = FetchType.EAGER, optional = false)
    @JoinColumn(name = "buyer_id", nullable = false)
    var buyer: UserJpaEntity,

    @ManyToOne(fetch = FetchType.EAGER, optional = false)
    @JoinColumn(name = "provider_id", nullable = false)
    var provider: UserJpaEntity,

    @Column(nullable = false)
    var quantity: Int,

    @Column(name = "unit_price", nullable = false, precision = 12, scale = 2)
    var unitPrice: BigDecimal,

    @Column(name = "currency_code", nullable = false, length = 3)
    var currencyCode: String,

    @Column(name = "expected_available_date")
    var expectedAvailableDate: LocalDate?,

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 40)
    var status: ReservationStatus,

    @Column(name = "stock_released", nullable = false)
    var stockReleased: Boolean,

    @Column(name = "created_at", nullable = false, updatable = false)
    var createdAt: LocalDateTime,

    @Column(name = "updated_at", nullable = false)
    var updatedAt: LocalDateTime,

    @Version
    @Column(nullable = false)
    var version: Long = 0,
)

@Entity
@Table(name = "reservation_history")
class ReservationHistoryJpaEntity(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    var id: Long = 0,

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "reservation_id", nullable = false)
    var reservation: ReservationJpaEntity,

    @Enumerated(EnumType.STRING)
    @Column(name = "from_status", length = 40)
    var fromStatus: ReservationStatus?,

    @Enumerated(EnumType.STRING)
    @Column(name = "to_status", nullable = false, length = 40)
    var toStatus: ReservationStatus,

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "actor_id", nullable = false)
    var actor: UserJpaEntity,

    @Column(name = "created_at", nullable = false)
    var createdAt: LocalDateTime,
)
