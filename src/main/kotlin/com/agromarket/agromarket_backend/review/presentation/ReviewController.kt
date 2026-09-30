package com.agromarket.agromarket_backend.review.presentation

import com.agromarket.agromarket_backend.auth.infrastructure.security.AuthenticatedUser
import com.agromarket.agromarket_backend.review.application.ReviewService
import com.agromarket.agromarket_backend.review.domain.ProductReview
import com.agromarket.agromarket_backend.review.domain.ReviewSummary
import com.agromarket.agromarket_backend.review.domain.SupplierReview
import jakarta.validation.Valid
import jakarta.validation.constraints.Max
import jakarta.validation.constraints.Min
import jakarta.validation.constraints.Size
import org.springframework.http.HttpStatus
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.ResponseStatus
import org.springframework.web.bind.annotation.RestController
import java.time.LocalDateTime
import java.util.UUID

@RestController
class ReviewController(
    private val reviewService: ReviewService,
) {
    @PostMapping("/api/reviews/providers")
    @ResponseStatus(HttpStatus.CREATED)
    fun reviewSupplier(
        @AuthenticationPrincipal principal: AuthenticatedUser,
        @Valid @RequestBody request: SupplierReviewRequest,
    ): SupplierReviewResponse = SupplierReviewResponse.from(
        reviewService.reviewSupplier(
            principal.id,
            request.conversationId,
            request.purchased,
            request.rating,
            request.comment,
        ),
    )

    @PostMapping("/api/reviews/products")
    @ResponseStatus(HttpStatus.CREATED)
    fun reviewProduct(
        @AuthenticationPrincipal principal: AuthenticatedUser,
        @Valid @RequestBody request: ProductReviewRequest,
    ): ProductReviewResponse = ProductReviewResponse.from(
        reviewService.reviewProduct(principal.id, request.reservationId, request.rating, request.comment),
    )

    @GetMapping("/api/reputation/providers/{supplierId}")
    fun supplierReputation(@PathVariable supplierId: UUID): ReviewSummaryResponse<SupplierReviewResponse> =
        reviewService.supplierSummary(supplierId).toResponse(SupplierReviewResponse::from)

    @GetMapping("/api/reputation/products/{productId}")
    fun productReputation(@PathVariable productId: Int): ReviewSummaryResponse<ProductReviewResponse> =
        reviewService.productSummary(productId).toResponse(ProductReviewResponse::from)
}

data class SupplierReviewRequest(
    val conversationId: UUID,
    val purchased: Boolean,
    @field:Min(1) @field:Max(5) val rating: Int,
    @field:Size(max = 1000) val comment: String? = null,
)

data class ProductReviewRequest(
    val reservationId: UUID,
    @field:Min(1) @field:Max(5) val rating: Int,
    @field:Size(max = 1000) val comment: String? = null,
)

data class SupplierReviewResponse(
    val id: UUID,
    val reviewerId: UUID,
    val reviewerName: String,
    val purchased: Boolean,
    val rating: Int,
    val comment: String?,
    val createdAt: LocalDateTime,
) {
    companion object {
        fun from(value: SupplierReview) = SupplierReviewResponse(
            value.id,
            value.reviewerId,
            value.reviewerName,
            value.purchased,
            value.rating,
            value.comment,
            value.createdAt,
        )
    }
}

data class ProductReviewResponse(
    val id: UUID,
    val reviewerId: UUID,
    val reviewerName: String,
    val productId: Int,
    val productName: String,
    val rating: Int,
    val comment: String?,
    val createdAt: LocalDateTime,
) {
    companion object {
        fun from(value: ProductReview) = ProductReviewResponse(
            value.id,
            value.reviewerId,
            value.reviewerName,
            value.productId,
            value.productName,
            value.rating,
            value.comment,
            value.createdAt,
        )
    }
}

data class ReviewSummaryResponse<T>(
    val average: Double?,
    val count: Int,
    val reviews: List<T>,
)

private fun <T, R> ReviewSummary<T>.toResponse(mapper: (T) -> R) = ReviewSummaryResponse(
    average = average,
    count = count,
    reviews = reviews.map(mapper),
)
