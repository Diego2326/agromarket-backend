package com.agromarket.agromarket_backend.favorite.application

import com.agromarket.agromarket_backend.favorite.domain.FavoriteRepository
import com.agromarket.agromarket_backend.publication.domain.Publication
import com.agromarket.agromarket_backend.publication.domain.PublicationRepository
import com.agromarket.agromarket_backend.publication.domain.PublicationStatus
import com.agromarket.agromarket_backend.shared.domain.InvalidPublicationException
import com.agromarket.agromarket_backend.shared.domain.PublicationNotFoundException
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.Clock
import java.time.LocalDateTime
import java.util.UUID

@Service
class FavoriteService(
    private val favorites: FavoriteRepository,
    private val publications: PublicationRepository,
    private val clock: Clock,
) {
    @Transactional
    fun add(userId: UUID, publicationId: UUID) {
        val publication = publications.findById(publicationId) ?: throw PublicationNotFoundException()
        if (publication.status != PublicationStatus.ACTIVE) {
            throw InvalidPublicationException("Solo puedes agregar publicaciones activas a favoritos")
        }
        if (!favorites.exists(userId, publicationId)) {
            favorites.add(userId, publicationId, LocalDateTime.now(clock))
        }
    }

    @Transactional
    fun remove(userId: UUID, publicationId: UUID) = favorites.remove(userId, publicationId)

    @Transactional(readOnly = true)
    fun list(userId: UUID): List<Publication> =
        favorites.findPublications(userId).filter { it.status == PublicationStatus.ACTIVE }
}
