package com.agromarket.agromarket_backend.reservation.application

import com.agromarket.agromarket_backend.publication.domain.Publication
import com.agromarket.agromarket_backend.publication.domain.PublicationRepository
import com.agromarket.agromarket_backend.publication.domain.PublicationStatus
import com.agromarket.agromarket_backend.reservation.domain.Reservation
import com.agromarket.agromarket_backend.reservation.domain.ReservationHistory
import com.agromarket.agromarket_backend.reservation.domain.ReservationRepository
import com.agromarket.agromarket_backend.reservation.domain.ReservationStatus
import com.agromarket.agromarket_backend.shared.domain.ForbiddenOperationException
import com.agromarket.agromarket_backend.shared.domain.InvalidReservationException
import com.agromarket.agromarket_backend.shared.domain.PublicationNotFoundException
import com.agromarket.agromarket_backend.shared.domain.ReservationNotFoundException
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.Clock
import java.time.LocalDateTime
import java.util.UUID

@Service
class ReservationService(
    private val reservations: ReservationRepository,
    private val publications: PublicationRepository,
    private val clock: Clock,
) {
    @Transactional
    fun create(buyerId: UUID, publicationId: UUID, quantity: Int): Reservation {
        if (quantity <= 0) throw InvalidReservationException("La cantidad debe ser mayor que cero")
        val publication = publications.findByIdForUpdate(publicationId) ?: throw PublicationNotFoundException()
        if (publication.status != PublicationStatus.ACTIVE) {
            throw InvalidReservationException("La publicación no está disponible para nuevas reservas")
        }
        if (publication.userOwnerId == buyerId) {
            throw InvalidReservationException("No puedes reservar tu propia publicación")
        }
        if (quantity > publication.unitsAvailable) {
            throw InvalidReservationException("La cantidad solicitada supera la disponibilidad actual")
        }

        val remaining = publication.unitsAvailable - quantity
        val now = LocalDateTime.now(clock)
        publications.save(
            publication.copy(
                unitsAvailable = remaining,
                status = if (remaining == 0) PublicationStatus.SOLD_OUT else publication.status,
                dateUpdated = now,
            ),
        )

        val reservation = reservations.save(
            Reservation(
                id = UUID.randomUUID(),
                publicationId = publication.id,
                publicationTitle = publication.title,
                productId = publication.product.id,
                productName = publication.product.name,
                buyerId = buyerId,
                buyerName = "",
                providerId = publication.userOwnerId,
                providerName = publication.ownerName.orEmpty(),
                quantity = quantity,
                unitPrice = publication.price,
                currencyCode = publication.currencyCode,
                expectedAvailableDate = publication.availableDate,
                status = ReservationStatus.PENDING_PROVIDER,
                stockReleased = false,
                createdAt = now,
                updatedAt = now,
            ),
        )
        recordTransition(reservation, null, buyerId, now)
        return reservation
    }

    @Transactional(readOnly = true)
    fun mine(buyerId: UUID): List<Reservation> = reservations.findByBuyerId(buyerId)

    @Transactional(readOnly = true)
    fun received(providerId: UUID): List<Reservation> = reservations.findByProviderId(providerId)

    @Transactional
    fun accept(providerId: UUID, reservationId: UUID): Reservation = transition(
        actorId = providerId,
        reservationId = reservationId,
        expected = ReservationStatus.PENDING_PROVIDER,
        target = ReservationStatus.ACCEPTED_PENDING_PAYMENT,
        requiredActor = RequiredActor.PROVIDER,
    )

    @Transactional
    fun reject(providerId: UUID, reservationId: UUID): Reservation = transition(
        actorId = providerId,
        reservationId = reservationId,
        expected = ReservationStatus.PENDING_PROVIDER,
        target = ReservationStatus.REJECTED,
        requiredActor = RequiredActor.PROVIDER,
        releaseStock = true,
    )

    @Transactional
    fun cancel(actorId: UUID, reservationId: UUID): Reservation {
        val reservation = locked(reservationId)
        requireParticipant(reservation, actorId)
        if (reservation.status !in setOf(
                ReservationStatus.PENDING_PROVIDER,
                ReservationStatus.ACCEPTED_PENDING_PAYMENT,
                ReservationStatus.CONFIRMED,
            )
        ) {
            throw InvalidReservationException("La reserva ya no puede cancelarse en su estado actual")
        }
        return persistTransition(reservation, ReservationStatus.CANCELLED, actorId, releaseStock = true)
    }

    @Transactional
    fun confirm(buyerId: UUID, reservationId: UUID): Reservation = transition(
        buyerId,
        reservationId,
        ReservationStatus.ACCEPTED_PENDING_PAYMENT,
        ReservationStatus.CONFIRMED,
        RequiredActor.BUYER,
    )

    @Transactional
    fun markReady(providerId: UUID, reservationId: UUID): Reservation = transition(
        providerId,
        reservationId,
        ReservationStatus.CONFIRMED,
        ReservationStatus.READY_FOR_DELIVERY,
        RequiredActor.PROVIDER,
    )

    @Transactional
    fun markDelivered(providerId: UUID, reservationId: UUID): Reservation = transition(
        providerId,
        reservationId,
        ReservationStatus.READY_FOR_DELIVERY,
        ReservationStatus.DELIVERED,
        RequiredActor.PROVIDER,
    )

    @Transactional
    fun complete(buyerId: UUID, reservationId: UUID): Reservation = transition(
        buyerId,
        reservationId,
        ReservationStatus.DELIVERED,
        ReservationStatus.COMPLETED,
        RequiredActor.BUYER,
    )

    private fun transition(
        actorId: UUID,
        reservationId: UUID,
        expected: ReservationStatus,
        target: ReservationStatus,
        requiredActor: RequiredActor,
        releaseStock: Boolean = false,
    ): Reservation {
        val reservation = locked(reservationId)
        when (requiredActor) {
            RequiredActor.BUYER -> if (reservation.buyerId != actorId) forbidden()
            RequiredActor.PROVIDER -> if (reservation.providerId != actorId) forbidden()
        }
        if (reservation.status != expected) {
            throw InvalidReservationException("La reserva debe estar en estado $expected para pasar a $target")
        }
        return persistTransition(reservation, target, actorId, releaseStock)
    }

    private fun persistTransition(
        current: Reservation,
        target: ReservationStatus,
        actorId: UUID,
        releaseStock: Boolean,
    ): Reservation {
        val now = LocalDateTime.now(clock)
        val released = if (releaseStock) releaseStock(current, now) else current.stockReleased
        val updated = reservations.save(
            current.copy(status = target, stockReleased = released, updatedAt = now),
        )
        recordTransition(updated, current.status, actorId, now)
        return updated
    }

    private fun releaseStock(reservation: Reservation, now: LocalDateTime): Boolean {
        if (reservation.stockReleased) return true
        val publication = publications.findByIdForUpdate(reservation.publicationId) ?: throw PublicationNotFoundException()
        val restored = publication.unitsAvailable + reservation.quantity
        if (restored > publication.units) {
            throw InvalidReservationException("La restitución produciría una disponibilidad inválida")
        }
        val restoredStatus = if (publication.status == PublicationStatus.SOLD_OUT) {
            PublicationStatus.ACTIVE
        } else {
            publication.status
        }
        publications.save(
            publication.copy(unitsAvailable = restored, status = restoredStatus, dateUpdated = now),
        )
        return true
    }

    private fun recordTransition(
        reservation: Reservation,
        fromStatus: ReservationStatus?,
        actorId: UUID,
        now: LocalDateTime,
    ) {
        reservations.saveHistory(
            ReservationHistory(
                reservationId = reservation.id,
                fromStatus = fromStatus,
                toStatus = reservation.status,
                actorId = actorId,
                createdAt = now,
            ),
        )
    }

    private fun locked(id: UUID): Reservation =
        reservations.findByIdForUpdate(id) ?: throw ReservationNotFoundException()

    private fun requireParticipant(reservation: Reservation, actorId: UUID) {
        if (actorId != reservation.buyerId && actorId != reservation.providerId) forbidden()
    }

    private fun forbidden(): Nothing =
        throw ForbiddenOperationException("No tienes permisos para modificar esta reserva")

    private enum class RequiredActor { BUYER, PROVIDER }
}
