package com.agromarket.agromarket_backend.publication.domain

import java.math.BigDecimal
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.UUID

data class Publication(
    val id: UUID,
    val userOwnerId: UUID,
    val ownerName: String?,
    val category: PublicationCategory,
    val product: PublicationProduct,
    val title: String,
    val description: String?,
    val price: BigDecimal,
    val currencyCode: String,
    val units: Int,
    val unitsAvailable: Int,
    val unitType: UnitType,
    val location: String?,
    val availableDate: LocalDate?,
    val status: PublicationStatus,
    val dateCreated: LocalDateTime,
    val dateUpdated: LocalDateTime,
    val version: Long = 0,
) {
    val unitsCommitted: Int
        get() = units - unitsAvailable
}

data class PublicationCategory(
    val id: Int,
    val code: String,
    val name: String,
)

data class PublicationProduct(
    val id: Int,
    val categoryId: Int,
    val code: String,
    val name: String,
)

data class UnitType(
    val id: Int,
    val code: String,
    val name: String,
)

enum class PublicationStatus {
    ACTIVE,
    PAUSED,
    SOLD_OUT,
    CLOSED,
}
