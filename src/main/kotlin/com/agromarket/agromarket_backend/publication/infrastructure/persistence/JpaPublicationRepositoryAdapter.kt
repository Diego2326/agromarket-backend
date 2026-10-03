package com.agromarket.agromarket_backend.publication.infrastructure.persistence

import com.agromarket.agromarket_backend.publication.domain.CategoryWithProducts
import com.agromarket.agromarket_backend.publication.domain.Publication
import com.agromarket.agromarket_backend.publication.domain.PublicationCatalogRepository
import com.agromarket.agromarket_backend.publication.domain.PublicationCategory
import com.agromarket.agromarket_backend.publication.domain.PublicationPage
import com.agromarket.agromarket_backend.publication.domain.PublicationPageRequest
import com.agromarket.agromarket_backend.publication.domain.PublicationProduct
import com.agromarket.agromarket_backend.publication.domain.PublicationRepository
import com.agromarket.agromarket_backend.publication.domain.PublicationSearchCriteria
import com.agromarket.agromarket_backend.publication.domain.PublicationSort
import com.agromarket.agromarket_backend.publication.domain.PublicationStatus
import com.agromarket.agromarket_backend.publication.domain.SortDirection
import com.agromarket.agromarket_backend.publication.domain.UnitType
import org.springframework.data.domain.PageRequest
import org.springframework.data.domain.Sort
import org.springframework.stereotype.Repository
import com.agromarket.agromarket_backend.user.infrastructure.persistence.SpringDataUserRepository
import java.util.UUID

@Repository
class JpaPublicationRepositoryAdapter(
    private val publicationRepository: SpringDataPublicationRepository,
    private val statusRepository: SpringDataPublicationStatusRepository,
    private val unitTypeRepository: SpringDataUnitTypeRepository,
    private val categoryRepository: SpringDataPublicationCategoryRepository,
    private val productRepository: SpringDataPublicationProductRepository,
    private val userRepository: SpringDataUserRepository,
) : PublicationRepository, PublicationCatalogRepository {
    override fun save(publication: Publication): Publication =
        publicationRepository.saveAndFlush(publication.toEntity()).toDomain()

    override fun findById(id: UUID): Publication? =
        publicationRepository.findById(id).orElse(null)?.toDomain()

    override fun findByIdForUpdate(id: UUID): Publication? =
        publicationRepository.findByIdForUpdate(id)?.toDomain()

    override fun search(criteria: PublicationSearchCriteria, pageRequest: PublicationPageRequest): PublicationPage {
        val sortProperty = when (pageRequest.sort) {
            PublicationSort.DATE_CREATED -> "dateCreated"
            PublicationSort.PRICE -> "price"
            PublicationSort.AVAILABLE_DATE -> "availableDate"
        }
        val direction = when (pageRequest.direction) {
            SortDirection.ASC -> Sort.Direction.ASC
            SortDirection.DESC -> Sort.Direction.DESC
        }
        val pageable = PageRequest.of(
            pageRequest.page,
            pageRequest.size,
            Sort.by(direction, sortProperty).and(Sort.by(Sort.Direction.ASC, "id")),
        )
        val result = publicationRepository.search(
            ownerId = criteria.ownerId,
            statusName = criteria.status?.name,
            query = criteria.query.cleanNullable().orEmpty(),
            categoryId = criteria.categoryId,
            productId = criteria.productId,
            location = criteria.location.cleanNullable().orEmpty(),
            minPrice = criteria.minPrice,
            maxPrice = criteria.maxPrice,
            availableFrom = criteria.availableFrom,
            availableTo = criteria.availableTo,
            minUnits = criteria.minUnitsAvailable,
            pageable = pageable,
        )
        return PublicationPage(
            items = result.content.map { it.toDomain() },
            page = result.number,
            size = result.size,
            totalElements = result.totalElements,
            totalPages = result.totalPages,
        )
    }

    override fun findCategory(id: Int): PublicationCategory? =
        categoryRepository.findById(id).orElse(null)?.toDomain()

    override fun findProduct(id: Int): PublicationProduct? =
        productRepository.findById(id).orElse(null)?.toDomain()

    override fun findUnitType(id: Int): UnitType? =
        unitTypeRepository.findById(id).orElse(null)?.toDomain()

    override fun findAllCategories(): List<CategoryWithProducts> =
        categoryRepository.findAllByOrderByNameAsc().map { category ->
            CategoryWithProducts(
                category = category.toDomain(),
                products = productRepository.findAllByCategoryIdOrderByNameAsc(category.id).map { it.toDomain() },
            )
        }

    override fun findAllUnitTypes(): List<UnitType> =
        unitTypeRepository.findAllByOrderByNameAsc().map { it.toDomain() }

    private fun Publication.toEntity() = PublicationJpaEntity(
        id = id,
        owner = userRepository.findById(userOwnerId).orElseThrow(),
        category = categoryRepository.findById(category.id).orElseThrow(),
        product = productRepository.findById(product.id).orElseThrow(),
        title = title,
        description = description,
        price = price,
        currencyCode = currencyCode,
        units = units,
        unitsAvailable = unitsAvailable,
        unitType = unitTypeRepository.findById(unitType.id).orElseThrow(),
        location = location,
        availableDate = availableDate,
        status = requireNotNull(statusRepository.findByName(status.name)),
        dateCreated = dateCreated,
        dateUpdated = dateUpdated,
        version = version,
    )

    private fun PublicationJpaEntity.toDomain() = Publication(
        id = id,
        userOwnerId = owner.id,
        ownerName = owner.fullName,
        category = category.toDomain(),
        product = product.toDomain(),
        title = title,
        description = description,
        price = price,
        currencyCode = currencyCode,
        units = units,
        unitsAvailable = unitsAvailable,
        unitType = unitType.toDomain(),
        location = location,
        availableDate = availableDate,
        status = PublicationStatus.valueOf(status.name),
        dateCreated = dateCreated,
        dateUpdated = dateUpdated,
        version = version,
    )

    private fun PublicationCategoryJpaEntity.toDomain() = PublicationCategory(id, code, name)

    private fun PublicationProductJpaEntity.toDomain() = PublicationProduct(id, category.id, code, name)

    private fun UnitTypeJpaEntity.toDomain() = UnitType(id, name, name.replace('_', ' '))

    private fun String?.cleanNullable(): String? = this?.trim()?.takeIf { it.isNotEmpty() }
}
