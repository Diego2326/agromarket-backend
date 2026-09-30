package com.agromarket.agromarket_backend.publication.infrastructure.persistence

import com.agromarket.agromarket_backend.user.infrastructure.persistence.UserJpaEntity
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.FetchType
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.JoinColumn
import jakarta.persistence.ManyToOne
import jakarta.persistence.Table
import jakarta.persistence.Version
import java.math.BigDecimal
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.UUID

@Entity
@Table(name = "status")
class PublicationStatusJpaEntity(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    var id: Int = 0,

    @Column(nullable = false, unique = true, length = 50)
    var name: String,
)

@Entity
@Table(name = "unittype")
class UnitTypeJpaEntity(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    var id: Int = 0,

    @Column(nullable = false, unique = true, length = 50)
    var name: String,
)

@Entity
@Table(name = "publication_category")
class PublicationCategoryJpaEntity(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    var id: Int = 0,

    @Column(nullable = false, unique = true, length = 40)
    var code: String,

    @Column(nullable = false, length = 100)
    var name: String,
)

@Entity
@Table(name = "publication_product")
class PublicationProductJpaEntity(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    var id: Int = 0,

    @ManyToOne(fetch = FetchType.EAGER, optional = false)
    @JoinColumn(name = "category_id", nullable = false)
    var category: PublicationCategoryJpaEntity,

    @Column(nullable = false, unique = true, length = 60)
    var code: String,

    @Column(nullable = false, length = 120)
    var name: String,
)

@Entity
@Table(name = "publication")
class PublicationJpaEntity(
    @Id
    var id: UUID,

    @ManyToOne(fetch = FetchType.EAGER, optional = false)
    @JoinColumn(name = "user_owner_id", nullable = false)
    var owner: UserJpaEntity,

    @ManyToOne(fetch = FetchType.EAGER, optional = false)
    @JoinColumn(name = "category_id", nullable = false)
    var category: PublicationCategoryJpaEntity,

    @ManyToOne(fetch = FetchType.EAGER, optional = false)
    @JoinColumn(name = "product_id", nullable = false)
    var product: PublicationProductJpaEntity,

    @Column(nullable = false, length = 100)
    var title: String,

    @Column(columnDefinition = "text")
    var description: String?,

    @Column(nullable = false, precision = 12, scale = 2)
    var price: BigDecimal,

    @Column(name = "currency_code", nullable = false, length = 3)
    var currencyCode: String,

    @Column(nullable = false)
    var units: Int,

    @Column(name = "units_available", nullable = false)
    var unitsAvailable: Int,

    @ManyToOne(fetch = FetchType.EAGER, optional = false)
    @JoinColumn(name = "unit_type_id", nullable = false)
    var unitType: UnitTypeJpaEntity,

    @Column(length = 100)
    var location: String?,

    @Column(name = "available_date")
    var availableDate: LocalDate?,

    @ManyToOne(fetch = FetchType.EAGER, optional = false)
    @JoinColumn(name = "status_id", nullable = false)
    var status: PublicationStatusJpaEntity,

    @Column(name = "date_created", nullable = false, updatable = false)
    var dateCreated: LocalDateTime,

    @Column(name = "date_updated", nullable = false)
    var dateUpdated: LocalDateTime,

    @Version
    @Column(nullable = false)
    var version: Long = 0,
)
