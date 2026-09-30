package com.agromarket.agromarket_backend.review.domain

import java.util.UUID

interface ReviewRepository {
    fun saveSupplierReview(review: SupplierReview): SupplierReview
    fun saveProductReview(review: ProductReview): ProductReview
    fun existsSupplierReviewForConversation(conversationId: UUID): Boolean
    fun existsProductReviewForReservation(reservationId: UUID): Boolean
    fun findSupplierReviews(supplierId: UUID): List<SupplierReview>
    fun findProductReviews(productId: Int): List<ProductReview>
}
