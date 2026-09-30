package com.agromarket.agromarket_backend.review.infrastructure.persistence

import com.agromarket.agromarket_backend.conversation.infrastructure.persistence.ConversationJpaEntity
import com.agromarket.agromarket_backend.conversation.infrastructure.persistence.SpringDataConversationRepository
import com.agromarket.agromarket_backend.publication.infrastructure.persistence.PublicationProductJpaEntity
import com.agromarket.agromarket_backend.publication.infrastructure.persistence.SpringDataPublicationProductRepository
import com.agromarket.agromarket_backend.reservation.infrastructure.persistence.ReservationJpaEntity
import com.agromarket.agromarket_backend.reservation.infrastructure.persistence.SpringDataReservationRepository
import com.agromarket.agromarket_backend.review.domain.ProductReview
import com.agromarket.agromarket_backend.review.domain.ReviewRepository
import com.agromarket.agromarket_backend.review.domain.SupplierReview
import com.agromarket.agromarket_backend.user.infrastructure.persistence.SpringDataUserRepository
import com.agromarket.agromarket_backend.user.infrastructure.persistence.UserJpaEntity
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.FetchType
import jakarta.persistence.Id
import jakarta.persistence.JoinColumn
import jakarta.persistence.ManyToOne
import jakarta.persistence.Table
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository
import java.time.LocalDateTime
import java.util.UUID

@Entity
@Table(name = "supplier_reviews")
class SupplierReviewJpaEntity(
    @Id
    var id: UUID,

    @ManyToOne(fetch = FetchType.EAGER, optional = false)
    @JoinColumn(name = "reviewer_id", nullable = false)
    var reviewer: UserJpaEntity,

    @ManyToOne(fetch = FetchType.EAGER, optional = false)
    @JoinColumn(name = "supplier_id", nullable = false)
    var supplier: UserJpaEntity,

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "conversation_id", nullable = false)
    var conversation: ConversationJpaEntity,

    @Column(nullable = false)
    var purchased: Boolean,

    @Column(nullable = false)
    var rating: Int,

    @Column(length = 1000)
    var comment: String?,

    @Column(name = "created_at", nullable = false)
    var createdAt: LocalDateTime,
)

@Entity
@Table(name = "product_reviews")
class ProductReviewJpaEntity(
    @Id
    var id: UUID,

    @ManyToOne(fetch = FetchType.EAGER, optional = false)
    @JoinColumn(name = "reviewer_id", nullable = false)
    var reviewer: UserJpaEntity,

    @ManyToOne(fetch = FetchType.EAGER, optional = false)
    @JoinColumn(name = "product_id", nullable = false)
    var product: PublicationProductJpaEntity,

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "reservation_id", nullable = false)
    var reservation: ReservationJpaEntity,

    @Column(nullable = false)
    var rating: Int,

    @Column(length = 1000)
    var comment: String?,

    @Column(name = "created_at", nullable = false)
    var createdAt: LocalDateTime,
)

interface SpringDataSupplierReviewRepository : JpaRepository<SupplierReviewJpaEntity, UUID> {
    fun existsByConversationId(conversationId: UUID): Boolean
    fun findAllBySupplierIdOrderByCreatedAtDesc(supplierId: UUID): List<SupplierReviewJpaEntity>
}

interface SpringDataProductReviewRepository : JpaRepository<ProductReviewJpaEntity, UUID> {
    fun existsByReservationId(reservationId: UUID): Boolean
    fun findAllByProductIdOrderByCreatedAtDesc(productId: Int): List<ProductReviewJpaEntity>
}

@Repository
class JpaReviewRepositoryAdapter(
    private val supplierRepository: SpringDataSupplierReviewRepository,
    private val productReviewRepository: SpringDataProductReviewRepository,
    private val userRepository: SpringDataUserRepository,
    private val conversationRepository: SpringDataConversationRepository,
    private val reservationRepository: SpringDataReservationRepository,
    private val productRepository: SpringDataPublicationProductRepository,
) : ReviewRepository {
    override fun saveSupplierReview(review: SupplierReview): SupplierReview =
        supplierRepository.saveAndFlush(
            SupplierReviewJpaEntity(
                id = review.id,
                reviewer = userRepository.getReferenceById(review.reviewerId),
                supplier = userRepository.getReferenceById(review.supplierId),
                conversation = conversationRepository.getReferenceById(review.conversationId),
                purchased = review.purchased,
                rating = review.rating,
                comment = review.comment,
                createdAt = review.createdAt,
            ),
        ).toDomain()

    override fun saveProductReview(review: ProductReview): ProductReview =
        productReviewRepository.saveAndFlush(
            ProductReviewJpaEntity(
                id = review.id,
                reviewer = userRepository.getReferenceById(review.reviewerId),
                product = productRepository.getReferenceById(review.productId),
                reservation = reservationRepository.getReferenceById(review.reservationId),
                rating = review.rating,
                comment = review.comment,
                createdAt = review.createdAt,
            ),
        ).toDomain()

    override fun existsSupplierReviewForConversation(conversationId: UUID): Boolean =
        supplierRepository.existsByConversationId(conversationId)

    override fun existsProductReviewForReservation(reservationId: UUID): Boolean =
        productReviewRepository.existsByReservationId(reservationId)

    override fun findSupplierReviews(supplierId: UUID): List<SupplierReview> =
        supplierRepository.findAllBySupplierIdOrderByCreatedAtDesc(supplierId).map { it.toDomain() }

    override fun findProductReviews(productId: Int): List<ProductReview> =
        productReviewRepository.findAllByProductIdOrderByCreatedAtDesc(productId).map { it.toDomain() }

    private fun SupplierReviewJpaEntity.toDomain() = SupplierReview(
        id = id,
        reviewerId = reviewer.id,
        reviewerName = reviewer.fullName,
        supplierId = supplier.id,
        conversationId = conversation.id,
        purchased = purchased,
        rating = rating,
        comment = comment,
        createdAt = createdAt,
    )

    private fun ProductReviewJpaEntity.toDomain() = ProductReview(
        id = id,
        reviewerId = reviewer.id,
        reviewerName = reviewer.fullName,
        productId = product.id,
        productName = product.name,
        reservationId = reservation.id,
        rating = rating,
        comment = comment,
        createdAt = createdAt,
    )
}
