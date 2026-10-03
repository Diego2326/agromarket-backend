package com.agromarket.agromarket_backend.publication.infrastructure.persistence

import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Lock
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import java.math.BigDecimal
import java.time.LocalDate
import java.util.UUID
import jakarta.persistence.LockModeType

interface SpringDataPublicationRepository : JpaRepository<PublicationJpaEntity, UUID> {
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT p FROM PublicationJpaEntity p WHERE p.id = :id")
    fun findByIdForUpdate(@Param("id") id: UUID): PublicationJpaEntity?

    @Query(
        """
        SELECT p FROM PublicationJpaEntity p
        WHERE (:ownerId IS NULL OR p.owner.id = :ownerId)
          AND (:statusName IS NULL OR p.status.name = :statusName)
          AND (:query = '' OR LOWER(p.title) LIKE LOWER(CONCAT('%', :query, '%'))
               OR LOWER(COALESCE(p.description, '')) LIKE LOWER(CONCAT('%', :query, '%'))
               OR LOWER(p.product.name) LIKE LOWER(CONCAT('%', :query, '%')))
          AND (:categoryId IS NULL OR p.category.id = :categoryId)
          AND (:productId IS NULL OR p.product.id = :productId)
          AND (:location = '' OR LOWER(COALESCE(p.location, '')) LIKE LOWER(CONCAT('%', :location, '%')))
          AND (:minPrice IS NULL OR p.price >= :minPrice)
          AND (:maxPrice IS NULL OR p.price <= :maxPrice)
          AND (:availableFrom IS NULL OR p.availableDate >= :availableFrom)
          AND (:availableTo IS NULL OR p.availableDate <= :availableTo)
          AND (:minUnits IS NULL OR p.unitsAvailable >= :minUnits)
        """,
    )
    fun search(
        @Param("ownerId") ownerId: UUID?,
        @Param("statusName") statusName: String?,
        @Param("query") query: String,
        @Param("categoryId") categoryId: Int?,
        @Param("productId") productId: Int?,
        @Param("location") location: String,
        @Param("minPrice") minPrice: BigDecimal?,
        @Param("maxPrice") maxPrice: BigDecimal?,
        @Param("availableFrom") availableFrom: LocalDate?,
        @Param("availableTo") availableTo: LocalDate?,
        @Param("minUnits") minUnits: Int?,
        pageable: Pageable,
    ): Page<PublicationJpaEntity>
}

interface SpringDataPublicationStatusRepository : JpaRepository<PublicationStatusJpaEntity, Int> {
    fun findByName(name: String): PublicationStatusJpaEntity?
}

interface SpringDataUnitTypeRepository : JpaRepository<UnitTypeJpaEntity, Int> {
    fun findAllByOrderByNameAsc(): List<UnitTypeJpaEntity>
}

interface SpringDataPublicationCategoryRepository : JpaRepository<PublicationCategoryJpaEntity, Int> {
    fun findAllByOrderByNameAsc(): List<PublicationCategoryJpaEntity>
}

interface SpringDataPublicationProductRepository : JpaRepository<PublicationProductJpaEntity, Int> {
    fun findAllByCategoryIdOrderByNameAsc(categoryId: Int): List<PublicationProductJpaEntity>
}
