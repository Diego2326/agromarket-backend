package com.agromarket.agromarket_backend.publication.presentation

import com.agromarket.agromarket_backend.auth.infrastructure.security.AuthenticatedUser
import com.agromarket.agromarket_backend.publication.application.PublicationService
import com.agromarket.agromarket_backend.publication.domain.PublicationPageRequest
import com.agromarket.agromarket_backend.publication.domain.PublicationSearchCriteria
import com.agromarket.agromarket_backend.publication.domain.PublicationSort
import com.agromarket.agromarket_backend.publication.domain.SortDirection
import jakarta.validation.Valid
import org.springframework.http.HttpStatus
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PatchMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.ResponseStatus
import org.springframework.web.bind.annotation.RestController
import java.math.BigDecimal
import java.time.LocalDate
import java.util.UUID

@RestController
@RequestMapping("/api/publications")
class PublicationController(
    private val publicationService: PublicationService,
) {
    @GetMapping
    fun search(
        @RequestParam(required = false) query: String?,
        @RequestParam(required = false) categoryId: Int?,
        @RequestParam(required = false) productId: Int?,
        @RequestParam(required = false) location: String?,
        @RequestParam(required = false) minPrice: BigDecimal?,
        @RequestParam(required = false) maxPrice: BigDecimal?,
        @RequestParam(required = false) availableFrom: LocalDate?,
        @RequestParam(required = false) availableTo: LocalDate?,
        @RequestParam(required = false) minUnitsAvailable: Int?,
        @RequestParam(defaultValue = "0") page: Int,
        @RequestParam(defaultValue = "20") size: Int,
        @RequestParam(defaultValue = "DATE_CREATED") sort: PublicationSort,
        @RequestParam(defaultValue = "DESC") direction: SortDirection,
    ): PublicationPageResponse = PublicationPageResponse.from(
        publicationService.search(
            criteria(query, categoryId, productId, location, minPrice, maxPrice, availableFrom, availableTo, minUnitsAvailable),
            PublicationPageRequest(page, size, sort, direction),
        ),
    )

    @GetMapping("/mine")
    fun mine(
        @AuthenticationPrincipal principal: AuthenticatedUser,
        @RequestParam(required = false) query: String?,
        @RequestParam(required = false) categoryId: Int?,
        @RequestParam(required = false) productId: Int?,
        @RequestParam(required = false) location: String?,
        @RequestParam(required = false) minPrice: BigDecimal?,
        @RequestParam(required = false) maxPrice: BigDecimal?,
        @RequestParam(required = false) availableFrom: LocalDate?,
        @RequestParam(required = false) availableTo: LocalDate?,
        @RequestParam(required = false) minUnitsAvailable: Int?,
        @RequestParam(defaultValue = "0") page: Int,
        @RequestParam(defaultValue = "20") size: Int,
        @RequestParam(defaultValue = "DATE_CREATED") sort: PublicationSort,
        @RequestParam(defaultValue = "DESC") direction: SortDirection,
    ): PublicationPageResponse = PublicationPageResponse.from(
        publicationService.searchOwned(
            principal.id,
            criteria(query, categoryId, productId, location, minPrice, maxPrice, availableFrom, availableTo, minUnitsAvailable),
            PublicationPageRequest(page, size, sort, direction),
        ),
    )

    @GetMapping("/{id}")
    fun detail(@PathVariable id: UUID): PublicationResponse =
        PublicationResponse.from(publicationService.getPublic(id))

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    fun create(
        @AuthenticationPrincipal principal: AuthenticatedUser,
        @Valid @RequestBody request: PublicationRequest,
    ): PublicationResponse = PublicationResponse.from(
        publicationService.create(principal.id, request.toCommand()),
    )

    @PutMapping("/{id}")
    fun update(
        @AuthenticationPrincipal principal: AuthenticatedUser,
        @PathVariable id: UUID,
        @Valid @RequestBody request: PublicationRequest,
    ): PublicationResponse = PublicationResponse.from(
        publicationService.update(principal.id, id, request.toCommand()),
    )

    @PatchMapping("/{id}/status")
    fun changeStatus(
        @AuthenticationPrincipal principal: AuthenticatedUser,
        @PathVariable id: UUID,
        @Valid @RequestBody request: ChangePublicationStatusRequest,
    ): PublicationResponse = PublicationResponse.from(
        publicationService.changeStatus(principal.id, id, request.status),
    )

    private fun criteria(
        query: String?,
        categoryId: Int?,
        productId: Int?,
        location: String?,
        minPrice: BigDecimal?,
        maxPrice: BigDecimal?,
        availableFrom: LocalDate?,
        availableTo: LocalDate?,
        minUnitsAvailable: Int?,
    ) = PublicationSearchCriteria(
        query = query,
        categoryId = categoryId,
        productId = productId,
        location = location,
        minPrice = minPrice,
        maxPrice = maxPrice,
        availableFrom = availableFrom,
        availableTo = availableTo,
        minUnitsAvailable = minUnitsAvailable,
    )
}

@RestController
class PublicationCatalogController(
    private val publicationService: PublicationService,
) {
    @GetMapping("/api/categories")
    fun categories(): List<CategoryResponse> = publicationService.categories().map(CategoryResponse::from)

    @GetMapping("/api/unit-types")
    fun unitTypes(): List<CatalogItemResponse> = publicationService.unitTypes().map { it.toResponse() }
}
