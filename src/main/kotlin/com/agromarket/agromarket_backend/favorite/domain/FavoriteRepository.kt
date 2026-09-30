package com.agromarket.agromarket_backend.favorite.domain

import com.agromarket.agromarket_backend.publication.domain.Publication
import java.time.LocalDateTime
import java.util.UUID

interface FavoriteRepository {
    fun exists(userId: UUID, publicationId: UUID): Boolean
    fun add(userId: UUID, publicationId: UUID, createdAt: LocalDateTime)
    fun remove(userId: UUID, publicationId: UUID)
    fun findPublications(userId: UUID): List<Publication>
}
