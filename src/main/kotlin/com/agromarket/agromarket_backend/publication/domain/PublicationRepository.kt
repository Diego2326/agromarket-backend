package com.agromarket.agromarket_backend.publication.domain

import java.math.BigDecimal
import java.time.LocalDate
import java.util.UUID

interface PublicationRepository {
    fun save(publication: Publication): Publication
    fun findById(id: UUID): Publication?
    fun findByIdForUpdate(id: UUID): Publication?
    fun search(criteria: PublicationSearchCriteria, pageRequest: PublicationPageRequest): PublicationPage
}

interface PublicationCatalogRepository {
    fun findCategory(id: Int): PublicationCategory?
    fun findProduct(id: Int): PublicationProduct?
    fun findUnitType(id: Int): UnitType?
    fun findAllCategories(): List<CategoryWithProducts>
    fun findAllUnitTypes(): List<UnitType>
}

data class PublicationSearchCriteria(
    val query: String? = null,
    val categoryId: Int? = null,
    val productId: Int? = null,
    val location: String? = null,
    val minPrice: BigDecimal? = null,
    val maxPrice: BigDecimal? = null,
    val availableFrom: LocalDate? = null,
    val availableTo: LocalDate? = null,
    val minUnitsAvailable: Int? = null,
    val ownerId: UUID? = null,
    val status: PublicationStatus? = PublicationStatus.ACTIVE,
)

data class PublicationPageRequest(
    val page: Int,
    val size: Int,
    val sort: PublicationSort,
    val direction: SortDirection,
)

data class PublicationPage(
    val items: List<Publication>,
    val page: Int,
    val size: Int,
    val totalElements: Long,
    val totalPages: Int,
)

data class CategoryWithProducts(
    val category: PublicationCategory,
    val products: List<PublicationProduct>,
)

enum class PublicationSort {
    DATE_CREATED,
    PRICE,
    AVAILABLE_DATE,
}

enum class SortDirection {
    ASC,
    DESC,
}
