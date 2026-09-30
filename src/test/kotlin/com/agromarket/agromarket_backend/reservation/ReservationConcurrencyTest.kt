package com.agromarket.agromarket_backend.reservation

import com.agromarket.agromarket_backend.auth.application.AuthService
import com.agromarket.agromarket_backend.auth.application.RegisterCommand
import com.agromarket.agromarket_backend.publication.application.CreatePublicationCommand
import com.agromarket.agromarket_backend.publication.application.PublicationService
import com.agromarket.agromarket_backend.publication.domain.PublicationRepository
import com.agromarket.agromarket_backend.reservation.application.ReservationService
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import java.math.BigDecimal
import java.time.LocalDate
import java.util.concurrent.CountDownLatch
import java.util.concurrent.Executors
import kotlin.test.assertEquals

@SpringBootTest(
    properties = [
        "spring.datasource.url=jdbc:h2:mem:agromarket-concurrency;MODE=PostgreSQL;DB_CLOSE_DELAY=-1",
        "spring.datasource.driver-class-name=org.h2.Driver",
        "spring.datasource.username=sa",
        "spring.datasource.password=",
        "app.security.jwt.secret=test-secret-with-at-least-thirty-two-characters",
    ],
)
class ReservationConcurrencyTest @Autowired constructor(
    private val authService: AuthService,
    private val publicationService: PublicationService,
    private val reservationService: ReservationService,
    private val publicationRepository: PublicationRepository,
) {
    @Test
    fun `concurrent reservations never make inventory negative`() {
        val provider = register("provider.concurrent@example.com", "Proveedor Concurrente")
        val buyer = register("buyer.concurrent@example.com", "Comprador Concurrente")
        val category = publicationService.categories().first()
        val product = category.products.first()
        val unit = publicationService.unitTypes().first()
        val publication = publicationService.create(
            provider.id,
            CreatePublicationCommand(
                categoryId = category.category.id,
                productId = product.id,
                title = "Inventario concurrente",
                description = null,
                price = BigDecimal("100.00"),
                units = 100,
                unitTypeId = unit.id,
                location = "Zacapa",
                availableDate = LocalDate.now().plusDays(5),
            ),
        )

        val ready = CountDownLatch(10)
        val start = CountDownLatch(1)
        val executor = Executors.newFixedThreadPool(10)
        try {
            val futures = (1..10).map {
                executor.submit<Boolean> {
                    ready.countDown()
                    start.await()
                    runCatching { reservationService.create(buyer.id, publication.id, 15) }.isSuccess
                }
            }
            ready.await()
            start.countDown()
            val successful = futures.count { it.get() }
            val persisted = requireNotNull(publicationRepository.findById(publication.id))

            assertEquals(6, successful)
            assertEquals(10, persisted.unitsAvailable)
            assertEquals(90, persisted.unitsCommitted)
        } finally {
            executor.shutdownNow()
        }
    }

    private fun register(email: String, name: String) = authService.register(
        RegisterCommand(email, "password123", name, null, "Guatemala"),
    ).user
}
