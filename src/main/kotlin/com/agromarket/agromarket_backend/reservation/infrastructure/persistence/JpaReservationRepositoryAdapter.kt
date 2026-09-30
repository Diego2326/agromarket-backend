package com.agromarket.agromarket_backend.reservation.infrastructure.persistence

import com.agromarket.agromarket_backend.publication.infrastructure.persistence.SpringDataPublicationRepository
import com.agromarket.agromarket_backend.reservation.domain.Reservation
import com.agromarket.agromarket_backend.reservation.domain.ReservationHistory
import com.agromarket.agromarket_backend.reservation.domain.ReservationRepository
import com.agromarket.agromarket_backend.user.infrastructure.persistence.SpringDataUserRepository
import org.springframework.stereotype.Repository
import java.util.UUID

@Repository
class JpaReservationRepositoryAdapter(
    private val reservationRepository: SpringDataReservationRepository,
    private val historyRepository: SpringDataReservationHistoryRepository,
    private val publicationRepository: SpringDataPublicationRepository,
    private val userRepository: SpringDataUserRepository,
) : ReservationRepository {
    override fun save(reservation: Reservation): Reservation =
        reservationRepository.saveAndFlush(reservation.toEntity()).toDomain()

    override fun saveHistory(history: ReservationHistory) {
        historyRepository.save(
            ReservationHistoryJpaEntity(
                id = history.id,
                reservation = reservationRepository.getReferenceById(history.reservationId),
                fromStatus = history.fromStatus,
                toStatus = history.toStatus,
                actor = userRepository.getReferenceById(history.actorId),
                createdAt = history.createdAt,
            ),
        )
    }

    override fun findByIdForUpdate(id: UUID): Reservation? =
        reservationRepository.findByIdForUpdate(id)?.toDomain()

    override fun findById(id: UUID): Reservation? =
        reservationRepository.findById(id).orElse(null)?.toDomain()

    override fun findByBuyerId(buyerId: UUID): List<Reservation> =
        reservationRepository.findAllByBuyerIdOrderByCreatedAtDesc(buyerId).map { it.toDomain() }

    override fun findByProviderId(providerId: UUID): List<Reservation> =
        reservationRepository.findAllByProviderIdOrderByCreatedAtDesc(providerId).map { it.toDomain() }

    private fun Reservation.toEntity() = ReservationJpaEntity(
        id = id,
        publication = publicationRepository.getReferenceById(publicationId),
        buyer = userRepository.getReferenceById(buyerId),
        provider = userRepository.getReferenceById(providerId),
        quantity = quantity,
        unitPrice = unitPrice,
        currencyCode = currencyCode,
        expectedAvailableDate = expectedAvailableDate,
        status = status,
        stockReleased = stockReleased,
        createdAt = createdAt,
        updatedAt = updatedAt,
        version = version,
    )

    private fun ReservationJpaEntity.toDomain() = Reservation(
        id = id,
        publicationId = publication.id,
        publicationTitle = publication.title,
        productId = publication.product.id,
        productName = publication.product.name,
        buyerId = buyer.id,
        buyerName = buyer.fullName,
        providerId = provider.id,
        providerName = provider.fullName,
        quantity = quantity,
        unitPrice = unitPrice,
        currencyCode = currencyCode,
        expectedAvailableDate = expectedAvailableDate,
        status = status,
        stockReleased = stockReleased,
        createdAt = createdAt,
        updatedAt = updatedAt,
        version = version,
    )
}
