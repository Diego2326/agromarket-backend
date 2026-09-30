package com.agromarket.agromarket_backend.publication.application

import com.agromarket.agromarket_backend.publication.domain.CategoryWithProducts
import com.agromarket.agromarket_backend.publication.domain.Publication
import com.agromarket.agromarket_backend.publication.domain.PublicationCatalogRepository
import com.agromarket.agromarket_backend.publication.domain.PublicationPage
import com.agromarket.agromarket_backend.publication.domain.PublicationPageRequest
import com.agromarket.agromarket_backend.publication.domain.PublicationRepository
import com.agromarket.agromarket_backend.publication.domain.PublicationSearchCriteria
import com.agromarket.agromarket_backend.publication.domain.PublicationStatus
import com.agromarket.agromarket_backend.publication.domain.UnitType
import com.agromarket.agromarket_backend.shared.domain.ForbiddenOperationException
import com.agromarket.agromarket_backend.shared.domain.InvalidPublicationException
import com.agromarket.agromarket_backend.shared.domain.PublicationNotFoundException
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.math.BigDecimal
import java.time.Clock
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.UUID

@Service
class PublicationService(
    private val publications: PublicationRepository,
    private val catalogs: PublicationCatalogRepository,
    private val clock: Clock,
) {
    @Transactional
    fun create(ownerId: UUID, command: CreatePublicationCommand): Publication {
        val category = catalogs.findCategory(command.categoryId)
            ?: throw InvalidPublicationException("La categoría indicada no existe")
        val product = catalogs.findProduct(command.productId)
            ?: throw InvalidPublicationException("El producto indicado no existe")
        if (product.categoryId != category.id) {
            throw InvalidPublicationException("El producto no pertenece a la categoría indicada")
        }
        val unitType = catalogs.findUnitType(command.unitTypeId)
            ?: throw InvalidPublicationException("El tipo de unidad indicado no existe")
        validate(command.title, command.price, command.units, command.availableDate)

        val now = LocalDateTime.now(clock)
        return publications.save(
            Publication(
                id = UUID.randomUUID(),
                userOwnerId = ownerId,
                ownerName = null,
                category = category,
                product = product,
                title = command.title.trim(),
                description = command.description.cleanNullable(),
                price = command.price,
                currencyCode = "GTQ",
                units = command.units,
                unitsAvailable = command.units,
                unitType = unitType,
                location = command.location.cleanNullable(),
                availableDate = command.availableDate,
                status = PublicationStatus.ACTIVE,
                dateCreated = now,
                dateUpdated = now,
            ),
        )
    }

    @Transactional
    fun update(ownerId: UUID, publicationId: UUID, command: UpdatePublicationCommand): Publication {
        val current = ownedPublication(ownerId, publicationId)
        if (current.status == PublicationStatus.CLOSED) {
            throw InvalidPublicationException("Una publicación cerrada ya no puede editarse")
        }

        val category = catalogs.findCategory(command.categoryId)
            ?: throw InvalidPublicationException("La categoría indicada no existe")
        val product = catalogs.findProduct(command.productId)
            ?: throw InvalidPublicationException("El producto indicado no existe")
        if (product.categoryId != category.id) {
            throw InvalidPublicationException("El producto no pertenece a la categoría indicada")
        }
        val unitType = catalogs.findUnitType(command.unitTypeId)
            ?: throw InvalidPublicationException("El tipo de unidad indicado no existe")
        validate(command.title, command.price, command.units, command.availableDate)

        val committed = current.unitsCommitted
        if (command.units < committed) {
            throw InvalidPublicationException(
                "Las unidades totales no pueden ser menores que las $committed unidades ya comprometidas",
            )
        }
        val available = command.units - committed
        val status = when {
            available == 0 -> PublicationStatus.SOLD_OUT
            current.status == PublicationStatus.SOLD_OUT -> PublicationStatus.ACTIVE
            else -> current.status
        }

        return publications.save(
            current.copy(
                category = category,
                product = product,
                title = command.title.trim(),
                description = command.description.cleanNullable(),
                price = command.price,
                units = command.units,
                unitsAvailable = available,
                unitType = unitType,
                location = command.location.cleanNullable(),
                availableDate = command.availableDate,
                status = status,
                dateUpdated = LocalDateTime.now(clock),
            ),
        )
    }

    @Transactional
    fun changeStatus(ownerId: UUID, publicationId: UUID, requestedStatus: PublicationStatus): Publication {
        val current = ownedPublication(ownerId, publicationId)
        val allowed = when (current.status) {
            PublicationStatus.ACTIVE -> setOf(PublicationStatus.PAUSED, PublicationStatus.CLOSED)
            PublicationStatus.PAUSED -> setOf(PublicationStatus.ACTIVE, PublicationStatus.CLOSED)
            PublicationStatus.SOLD_OUT -> setOf(PublicationStatus.CLOSED)
            PublicationStatus.CLOSED -> emptySet()
        }
        if (requestedStatus !in allowed) {
            throw InvalidPublicationException(
                "No se puede cambiar una publicación ${current.status} a $requestedStatus",
            )
        }
        if (requestedStatus == PublicationStatus.ACTIVE && current.unitsAvailable == 0) {
            throw InvalidPublicationException("Una publicación sin disponibilidad no puede activarse")
        }
        return publications.save(
            current.copy(status = requestedStatus, dateUpdated = LocalDateTime.now(clock)),
        )
    }

    @Transactional(readOnly = true)
    fun getPublic(publicationId: UUID): Publication {
        val publication = publications.findById(publicationId) ?: throw PublicationNotFoundException()
        if (publication.status != PublicationStatus.ACTIVE) throw PublicationNotFoundException()
        return publication
    }

    @Transactional(readOnly = true)
    fun getOwned(ownerId: UUID, publicationId: UUID): Publication = ownedPublication(ownerId, publicationId)

    @Transactional(readOnly = true)
    fun search(criteria: PublicationSearchCriteria, page: PublicationPageRequest): PublicationPage {
        validateSearch(criteria, page)
        return publications.search(criteria.copy(status = PublicationStatus.ACTIVE, ownerId = null), page)
    }

    @Transactional(readOnly = true)
    fun searchOwned(ownerId: UUID, criteria: PublicationSearchCriteria, page: PublicationPageRequest): PublicationPage {
        validateSearch(criteria, page)
        return publications.search(criteria.copy(ownerId = ownerId, status = null), page)
    }

    @Transactional(readOnly = true)
    fun categories(): List<CategoryWithProducts> = catalogs.findAllCategories()

    @Transactional(readOnly = true)
    fun unitTypes(): List<UnitType> = catalogs.findAllUnitTypes()

    private fun ownedPublication(ownerId: UUID, publicationId: UUID): Publication {
        val publication = publications.findById(publicationId) ?: throw PublicationNotFoundException()
        if (publication.userOwnerId != ownerId) {
            throw ForbiddenOperationException("No puedes modificar una publicación que pertenece a otro usuario")
        }
        return publication
    }

    private fun validate(title: String, price: BigDecimal, units: Int, availableDate: LocalDate?) {
        if (title.isBlank()) throw InvalidPublicationException("El título no puede estar vacío")
        if (price <= BigDecimal.ZERO) throw InvalidPublicationException("El precio debe ser mayor que cero")
        if (units <= 0) throw InvalidPublicationException("Las unidades deben ser mayores que cero")
        if (availableDate != null && availableDate.isBefore(LocalDate.now(clock))) {
            throw InvalidPublicationException("La fecha de disponibilidad no puede estar en el pasado")
        }
    }

    private fun validateSearch(criteria: PublicationSearchCriteria, page: PublicationPageRequest) {
        if (page.page < 0) throw InvalidPublicationException("La página no puede ser negativa")
        if (page.size !in 1..100) throw InvalidPublicationException("El tamaño de página debe estar entre 1 y 100")
        if (criteria.minPrice != null && criteria.minPrice < BigDecimal.ZERO) {
            throw InvalidPublicationException("El precio mínimo no puede ser negativo")
        }
        if (criteria.maxPrice != null && criteria.maxPrice < BigDecimal.ZERO) {
            throw InvalidPublicationException("El precio máximo no puede ser negativo")
        }
        if (criteria.minPrice != null && criteria.maxPrice != null && criteria.minPrice > criteria.maxPrice) {
            throw InvalidPublicationException("El precio mínimo no puede superar al precio máximo")
        }
        if (criteria.availableFrom != null && criteria.availableTo != null &&
            criteria.availableFrom > criteria.availableTo
        ) {
            throw InvalidPublicationException("La fecha inicial no puede ser posterior a la fecha final")
        }
        if (criteria.minUnitsAvailable != null && criteria.minUnitsAvailable < 0) {
            throw InvalidPublicationException("La cantidad mínima no puede ser negativa")
        }
    }

    private fun String?.cleanNullable(): String? = this?.trim()?.takeIf { it.isNotEmpty() }
}

data class CreatePublicationCommand(
    val categoryId: Int,
    val productId: Int,
    val title: String,
    val description: String?,
    val price: BigDecimal,
    val units: Int,
    val unitTypeId: Int,
    val location: String?,
    val availableDate: LocalDate?,
)

typealias UpdatePublicationCommand = CreatePublicationCommand
