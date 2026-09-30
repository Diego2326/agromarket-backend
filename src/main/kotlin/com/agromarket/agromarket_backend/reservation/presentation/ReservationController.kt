package com.agromarket.agromarket_backend.reservation.presentation

import com.agromarket.agromarket_backend.auth.infrastructure.security.AuthenticatedUser
import com.agromarket.agromarket_backend.reservation.application.ReservationService
import com.agromarket.agromarket_backend.reservation.domain.Reservation
import com.agromarket.agromarket_backend.reservation.domain.ReservationStatus
import jakarta.validation.Valid
import jakarta.validation.constraints.Positive
import org.springframework.http.HttpStatus
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PatchMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.ResponseStatus
import org.springframework.web.bind.annotation.RestController
import java.math.BigDecimal
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.UUID

@RestController
@RequestMapping("/api/reservations")
class ReservationController(
    private val reservationService: ReservationService,
) {
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    fun create(
        @AuthenticationPrincipal principal: AuthenticatedUser,
        @Valid @RequestBody request: CreateReservationRequest,
    ): ReservationResponse = ReservationResponse.from(
        reservationService.create(principal.id, request.publicationId, request.quantity),
    )

    @GetMapping("/mine")
    fun mine(@AuthenticationPrincipal principal: AuthenticatedUser): List<ReservationResponse> =
        reservationService.mine(principal.id).map(ReservationResponse::from)

    @GetMapping("/received")
    fun received(@AuthenticationPrincipal principal: AuthenticatedUser): List<ReservationResponse> =
        reservationService.received(principal.id).map(ReservationResponse::from)

    @PatchMapping("/{id}/accept")
    fun accept(@AuthenticationPrincipal principal: AuthenticatedUser, @PathVariable id: UUID) =
        ReservationResponse.from(reservationService.accept(principal.id, id))

    @PatchMapping("/{id}/reject")
    fun reject(@AuthenticationPrincipal principal: AuthenticatedUser, @PathVariable id: UUID) =
        ReservationResponse.from(reservationService.reject(principal.id, id))

    @PatchMapping("/{id}/cancel")
    fun cancel(@AuthenticationPrincipal principal: AuthenticatedUser, @PathVariable id: UUID) =
        ReservationResponse.from(reservationService.cancel(principal.id, id))

    @PatchMapping("/{id}/confirm")
    fun confirm(@AuthenticationPrincipal principal: AuthenticatedUser, @PathVariable id: UUID) =
        ReservationResponse.from(reservationService.confirm(principal.id, id))

    @PatchMapping("/{id}/ready")
    fun ready(@AuthenticationPrincipal principal: AuthenticatedUser, @PathVariable id: UUID) =
        ReservationResponse.from(reservationService.markReady(principal.id, id))

    @PatchMapping("/{id}/delivered")
    fun delivered(@AuthenticationPrincipal principal: AuthenticatedUser, @PathVariable id: UUID) =
        ReservationResponse.from(reservationService.markDelivered(principal.id, id))

    @PatchMapping("/{id}/complete")
    fun complete(@AuthenticationPrincipal principal: AuthenticatedUser, @PathVariable id: UUID) =
        ReservationResponse.from(reservationService.complete(principal.id, id))
}

data class CreateReservationRequest(
    val publicationId: UUID,

    @field:Positive(message = "La cantidad debe ser mayor que cero")
    val quantity: Int,
)

data class ReservationResponse(
    val id: UUID,
    val publicationId: UUID,
    val publicationTitle: String,
    val productId: Int,
    val productName: String,
    val buyerId: UUID,
    val buyerName: String,
    val providerId: UUID,
    val providerName: String,
    val quantity: Int,
    val unitPrice: BigDecimal,
    val total: BigDecimal,
    val currencyCode: String,
    val expectedAvailableDate: LocalDate?,
    val status: ReservationStatus,
    val createdAt: LocalDateTime,
    val updatedAt: LocalDateTime,
) {
    companion object {
        fun from(value: Reservation) = ReservationResponse(
            id = value.id,
            publicationId = value.publicationId,
            publicationTitle = value.publicationTitle,
            productId = value.productId,
            productName = value.productName,
            buyerId = value.buyerId,
            buyerName = value.buyerName,
            providerId = value.providerId,
            providerName = value.providerName,
            quantity = value.quantity,
            unitPrice = value.unitPrice,
            total = value.total,
            currencyCode = value.currencyCode,
            expectedAvailableDate = value.expectedAvailableDate,
            status = value.status,
            createdAt = value.createdAt,
            updatedAt = value.updatedAt,
        )
    }
}
