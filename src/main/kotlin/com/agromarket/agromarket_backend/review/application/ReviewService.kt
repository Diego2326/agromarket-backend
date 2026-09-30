package com.agromarket.agromarket_backend.review.application

import com.agromarket.agromarket_backend.conversation.domain.ConversationRepository
import com.agromarket.agromarket_backend.reservation.domain.ReservationRepository
import com.agromarket.agromarket_backend.reservation.domain.ReservationStatus
import com.agromarket.agromarket_backend.review.domain.ProductReview
import com.agromarket.agromarket_backend.review.domain.ReviewRepository
import com.agromarket.agromarket_backend.review.domain.ReviewSummary
import com.agromarket.agromarket_backend.review.domain.SupplierReview
import com.agromarket.agromarket_backend.shared.domain.ConversationNotFoundException
import com.agromarket.agromarket_backend.shared.domain.ForbiddenOperationException
import com.agromarket.agromarket_backend.shared.domain.InvalidReviewException
import com.agromarket.agromarket_backend.shared.domain.ReservationNotFoundException
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.Clock
import java.time.LocalDateTime
import java.util.UUID

@Service
class ReviewService(
    private val reviews: ReviewRepository,
    private val conversations: ConversationRepository,
    private val reservations: ReservationRepository,
    private val clock: Clock,
) {
    @Transactional
    fun reviewSupplier(
        reviewerId: UUID,
        conversationId: UUID,
        purchased: Boolean,
        rating: Int,
        comment: String?,
    ): SupplierReview {
        validate(rating, comment)
        val conversation = conversations.findById(conversationId) ?: throw ConversationNotFoundException()
        if (conversation.buyerId != reviewerId) {
            throw ForbiddenOperationException("Solo el comprador de la conversación puede calificar al proveedor")
        }
        if (!conversations.hasMessagesFromBothParticipants(
                conversation.id,
                conversation.buyerId,
                conversation.providerId,
            )
        ) {
            throw InvalidReviewException("La conversación aún no constituye una interacción elegible")
        }
        if (reviews.existsSupplierReviewForConversation(conversationId)) {
            throw InvalidReviewException("Esta interacción ya tiene una reseña de proveedor")
        }
        return reviews.saveSupplierReview(
            SupplierReview(
                id = UUID.randomUUID(),
                reviewerId = reviewerId,
                reviewerName = conversation.buyerName,
                supplierId = conversation.providerId,
                conversationId = conversation.id,
                purchased = purchased,
                rating = rating,
                comment = comment.cleanNullable(),
                createdAt = LocalDateTime.now(clock),
            ),
        )
    }

    @Transactional
    fun reviewProduct(
        reviewerId: UUID,
        reservationId: UUID,
        rating: Int,
        comment: String?,
    ): ProductReview {
        validate(rating, comment)
        val reservation = reservations.findById(reservationId) ?: throw ReservationNotFoundException()
        if (reservation.buyerId != reviewerId) {
            throw ForbiddenOperationException("Solo el comprador de la reserva puede calificar el producto")
        }
        if (reservation.status != ReservationStatus.COMPLETED) {
            throw InvalidReviewException("El producto solo puede calificarse después de completar la operación")
        }
        if (reviews.existsProductReviewForReservation(reservationId)) {
            throw InvalidReviewException("Esta operación ya tiene una reseña de producto")
        }
        return reviews.saveProductReview(
            ProductReview(
                id = UUID.randomUUID(),
                reviewerId = reviewerId,
                reviewerName = reservation.buyerName,
                productId = reservation.productId,
                productName = reservation.productName,
                reservationId = reservation.id,
                rating = rating,
                comment = comment.cleanNullable(),
                createdAt = LocalDateTime.now(clock),
            ),
        )
    }

    @Transactional(readOnly = true)
    fun supplierSummary(supplierId: UUID): ReviewSummary<SupplierReview> =
        summary(reviews.findSupplierReviews(supplierId)) { it.rating }

    @Transactional(readOnly = true)
    fun productSummary(productId: Int): ReviewSummary<ProductReview> =
        summary(reviews.findProductReviews(productId)) { it.rating }

    private fun validate(rating: Int, comment: String?) {
        if (rating !in 1..5) throw InvalidReviewException("La puntuación debe estar entre 1 y 5")
        if (comment != null && comment.trim().length > 1000) {
            throw InvalidReviewException("El comentario no puede superar 1000 caracteres")
        }
    }

    private fun <T> summary(values: List<T>, rating: (T) -> Int): ReviewSummary<T> = ReviewSummary(
        average = values.takeIf { it.isNotEmpty() }?.map(rating)?.average(),
        count = values.size,
        reviews = values,
    )

    private fun String?.cleanNullable(): String? = this?.trim()?.takeIf { it.isNotEmpty() }
}
