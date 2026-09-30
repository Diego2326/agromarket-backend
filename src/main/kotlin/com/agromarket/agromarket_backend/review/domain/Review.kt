package com.agromarket.agromarket_backend.review.domain

import java.time.LocalDateTime
import java.util.UUID

data class SupplierReview(
    val id: UUID,
    val reviewerId: UUID,
    val reviewerName: String,
    val supplierId: UUID,
    val conversationId: UUID,
    val purchased: Boolean,
    val rating: Int,
    val comment: String?,
    val createdAt: LocalDateTime,
)

data class ProductReview(
    val id: UUID,
    val reviewerId: UUID,
    val reviewerName: String,
    val productId: Int,
    val productName: String,
    val reservationId: UUID,
    val rating: Int,
    val comment: String?,
    val createdAt: LocalDateTime,
)

data class ReviewSummary<T>(
    val average: Double?,
    val count: Int,
    val reviews: List<T>,
)
