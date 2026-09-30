package com.agromarket.agromarket_backend.publication.presentation

import com.agromarket.agromarket_backend.publication.application.CreatePublicationCommand
import com.agromarket.agromarket_backend.publication.domain.CategoryWithProducts
import com.agromarket.agromarket_backend.publication.domain.Publication
import com.agromarket.agromarket_backend.publication.domain.PublicationPage
import com.agromarket.agromarket_backend.publication.domain.PublicationStatus
import com.agromarket.agromarket_backend.publication.domain.UnitType
import jakarta.validation.constraints.DecimalMin
import jakarta.validation.constraints.Digits
import jakarta.validation.constraints.FutureOrPresent
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Positive
import jakarta.validation.constraints.Size
import java.math.BigDecimal
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.UUID

data class PublicationRequest(
    @field:Positive(message = "La categoría es obligatoria")
    val categoryId: Int,

    @field:Positive(message = "El producto es obligatorio")
    val productId: Int,

    @field:NotBlank(message = "El título es obligatorio")
    @field:Size(max = 100, message = "El título no puede superar 100 caracteres")
    val title: String,

    @field:Size(max = 5000, message = "La descripción no puede superar 5000 caracteres")
    val description: String? = null,

    @field:DecimalMin(value = "0.01", message = "El precio debe ser mayor que cero")
    @field:Digits(integer = 10, fraction = 2, message = "El precio admite hasta 10 enteros y 2 decimales")
    val price: BigDecimal,

    @field:Positive(message = "Las unidades deben ser mayores que cero")
    val units: Int,

    @field:Positive(message = "El tipo de unidad es obligatorio")
    val unitTypeId: Int,

    @field:Size(max = 100, message = "La ubicación no puede superar 100 caracteres")
    val location: String? = null,

    @field:FutureOrPresent(message = "La fecha de disponibilidad no puede estar en el pasado")
    val availableDate: LocalDate? = null,
) {
    fun toCommand() = CreatePublicationCommand(
        categoryId = categoryId,
        productId = productId,
        title = title,
        description = description,
        price = price,
        units = units,
        unitTypeId = unitTypeId,
        location = location,
        availableDate = availableDate,
    )
}

data class ChangePublicationStatusRequest(
    val status: PublicationStatus,
)

data class PublicationResponse(
    val id: UUID,
    val ownerId: UUID,
    val ownerName: String?,
    val category: CatalogItemResponse,
    val product: CatalogItemResponse,
    val title: String,
    val description: String?,
    val price: BigDecimal,
    val currencyCode: String,
    val units: Int,
    val unitsAvailable: Int,
    val unitsCommitted: Int,
    val unitType: CatalogItemResponse,
    val location: String?,
    val availableDate: LocalDate?,
    val status: PublicationStatus,
    val createdAt: LocalDateTime,
    val updatedAt: LocalDateTime,
) {
    companion object {
        fun from(publication: Publication) = PublicationResponse(
            id = publication.id,
            ownerId = publication.userOwnerId,
            ownerName = publication.ownerName,
            category = CatalogItemResponse(
                publication.category.id,
                publication.category.code,
                publication.category.name,
            ),
            product = CatalogItemResponse(
                publication.product.id,
                publication.product.code,
                publication.product.name,
            ),
            title = publication.title,
            description = publication.description,
            price = publication.price,
            currencyCode = publication.currencyCode,
            units = publication.units,
            unitsAvailable = publication.unitsAvailable,
            unitsCommitted = publication.unitsCommitted,
            unitType = CatalogItemResponse(
                publication.unitType.id,
                publication.unitType.code,
                publication.unitType.name,
            ),
            location = publication.location,
            availableDate = publication.availableDate,
            status = publication.status,
            createdAt = publication.dateCreated,
            updatedAt = publication.dateUpdated,
        )
    }
}

data class PublicationPageResponse(
    val content: List<PublicationResponse>,
    val page: Int,
    val size: Int,
    val totalElements: Long,
    val totalPages: Int,
) {
    companion object {
        fun from(result: PublicationPage) = PublicationPageResponse(
            content = result.items.map(PublicationResponse::from),
            page = result.page,
            size = result.size,
            totalElements = result.totalElements,
            totalPages = result.totalPages,
        )
    }
}

data class CatalogItemResponse(
    val id: Int,
    val code: String,
    val name: String,
)

data class CategoryResponse(
    val id: Int,
    val code: String,
    val name: String,
    val products: List<CatalogItemResponse>,
) {
    companion object {
        fun from(value: CategoryWithProducts) = CategoryResponse(
            id = value.category.id,
            code = value.category.code,
            name = value.category.name,
            products = value.products.map { CatalogItemResponse(it.id, it.code, it.name) },
        )
    }
}

fun UnitType.toResponse() = CatalogItemResponse(id, code, name)
