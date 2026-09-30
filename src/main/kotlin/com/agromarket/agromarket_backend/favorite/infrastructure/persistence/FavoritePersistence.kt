package com.agromarket.agromarket_backend.favorite.infrastructure.persistence

import com.agromarket.agromarket_backend.favorite.domain.FavoriteRepository
import com.agromarket.agromarket_backend.publication.domain.Publication
import com.agromarket.agromarket_backend.publication.domain.PublicationRepository
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.Id
import jakarta.persistence.IdClass
import jakarta.persistence.Table
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository
import java.io.Serializable
import java.time.LocalDateTime
import java.util.UUID

data class FavoriteJpaId(
    var userId: UUID = UUID(0, 0),
    var publicationId: UUID = UUID(0, 0),
) : Serializable

@Entity
@IdClass(FavoriteJpaId::class)
@Table(name = "favorites")
class FavoriteJpaEntity(
    @Id
    @Column(name = "user_id", nullable = false)
    var userId: UUID,

    @Id
    @Column(name = "publication_id", nullable = false)
    var publicationId: UUID,

    @Column(name = "created_at", nullable = false)
    var createdAt: LocalDateTime,
)

interface SpringDataFavoriteRepository : JpaRepository<FavoriteJpaEntity, FavoriteJpaId> {
    fun existsByUserIdAndPublicationId(userId: UUID, publicationId: UUID): Boolean
    fun deleteByUserIdAndPublicationId(userId: UUID, publicationId: UUID)
    fun findAllByUserIdOrderByCreatedAtDesc(userId: UUID): List<FavoriteJpaEntity>
}

@Repository
class JpaFavoriteRepositoryAdapter(
    private val repository: SpringDataFavoriteRepository,
    private val publications: PublicationRepository,
) : FavoriteRepository {
    override fun exists(userId: UUID, publicationId: UUID): Boolean =
        repository.existsByUserIdAndPublicationId(userId, publicationId)

    override fun add(userId: UUID, publicationId: UUID, createdAt: LocalDateTime) {
        repository.save(FavoriteJpaEntity(userId, publicationId, createdAt))
    }

    override fun remove(userId: UUID, publicationId: UUID) =
        repository.deleteByUserIdAndPublicationId(userId, publicationId)

    override fun findPublications(userId: UUID): List<Publication> =
        repository.findAllByUserIdOrderByCreatedAtDesc(userId)
            .mapNotNull { publications.findById(it.publicationId) }
}
