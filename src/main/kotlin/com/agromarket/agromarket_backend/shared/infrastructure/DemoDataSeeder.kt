package com.agromarket.agromarket_backend.shared.infrastructure

import com.agromarket.agromarket_backend.auth.application.AuthService
import com.agromarket.agromarket_backend.auth.application.RegisterCommand
import com.agromarket.agromarket_backend.conversation.application.ConversationService
import com.agromarket.agromarket_backend.publication.application.CreatePublicationCommand
import com.agromarket.agromarket_backend.publication.application.PublicationService
import com.agromarket.agromarket_backend.reservation.application.ReservationService
import com.agromarket.agromarket_backend.review.application.ReviewService
import com.agromarket.agromarket_backend.user.domain.User
import com.agromarket.agromarket_backend.user.domain.UserRepository
import org.springframework.boot.ApplicationArguments
import org.springframework.boot.ApplicationRunner
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional
import java.math.BigDecimal
import java.time.LocalDate

@Component
@ConditionalOnProperty(name = ["app.demo-data.enabled"], havingValue = "true")
class DemoDataSeeder(
    private val authService: AuthService,
    private val publicationService: PublicationService,
    private val reservationService: ReservationService,
    private val conversationService: ConversationService,
    private val reviewService: ReviewService,
    private val users: UserRepository,
) : ApplicationRunner {
    @Transactional
    override fun run(args: ApplicationArguments) {
        if (users.existsByEmail(DEMO_SENTINEL_EMAIL)) return

        val demoUsers = createUsers()
        val categories = publicationService.categories()
        val units = publicationService.unitTypes()
        val products = categories.flatMap { it.products }
        val locations = listOf("Zacapa", "Chiquimula", "Guatemala", "Escuintla", "Alta Verapaz")
        val titles = listOf(
            "Maíz amarillo de primera",
            "Frijol negro seleccionado",
            "Café pergamino",
            "Hortalizas mixtas",
            "Plástico PET clasificado",
            "Cartón compactado",
            "Aluminio reciclable",
            "Vidrio limpio por tonelada",
        )

        val publications = (0 until 24).map { index ->
            val owner = demoUsers[index % 5]
            val product = products[index % products.size]
            val category = categories.first { it.category.id == product.categoryId }.category
            publicationService.create(
                owner.id,
                CreatePublicationCommand(
                    categoryId = category.id,
                    productId = product.id,
                    title = "${titles[index % titles.size]} #${index + 1}",
                    description = "Publicación de demostración con disponibilidad verificable.",
                    price = BigDecimal.valueOf(75L + index * 12L).setScale(2),
                    units = 80 + index * 5,
                    unitTypeId = units[index % units.size].id,
                    location = locations[index % locations.size],
                    availableDate = LocalDate.now().plusDays((index % 20 + 1).toLong()),
                ),
            )
        }

        val reservations = (0 until 10).map { index ->
            val publication = publications[index]
            val buyer = demoUsers[5 + index % 7]
            reservationService.create(buyer.id, publication.id, index % 4 + 2)
        }

        reservations.take(6).forEach { reservationService.accept(it.providerId, it.id) }
        reservations.take(3).forEach {
            reservationService.confirm(it.buyerId, it.id)
            reservationService.markReady(it.providerId, it.id)
            reservationService.markDelivered(it.providerId, it.id)
            reservationService.complete(it.buyerId, it.id)
        }
        reservations.drop(6).take(2).forEach { reservationService.reject(it.providerId, it.id) }

        publications.take(5).forEachIndexed { index, publication ->
            val buyer = demoUsers[5 + index]
            val conversation = conversationService.start(buyer.id, publication.id)
            conversationService.send(buyer.id, conversation.id, "Hola, me interesa esta publicación.")
            conversationService.send(publication.userOwnerId, conversation.id, "Con gusto, tenemos disponibilidad.")
            reviewService.reviewSupplier(
                reviewerId = buyer.id,
                conversationId = conversation.id,
                purchased = index < 3,
                rating = 4 + index % 2,
                comment = "Atención clara y rápida.",
            )
        }

        reservations.take(3).forEachIndexed { index, reservation ->
            reviewService.reviewProduct(
                reviewerId = reservation.buyerId,
                reservationId = reservation.id,
                rating = 4 + index % 2,
                comment = "Producto recibido según lo acordado.",
            )
        }
    }

    private fun createUsers(): List<User> = (0 until 12).map { index ->
        val email = if (index == 0) DEMO_SENTINEL_EMAIL else "demo${index + 1}@agromarket.test"
        authService.register(
            RegisterCommand(
                email = email,
                password = "password123",
                fullName = if (index < 5) "Proveedor Demo ${index + 1}" else "Comprador Demo ${index - 4}",
                phone = "5555-${(1000 + index)}",
                location = listOf("Zacapa", "Chiquimula", "Guatemala")[index % 3],
            ),
        ).user
    }

    companion object {
        private const val DEMO_SENTINEL_EMAIL = "proveedor1@agromarket.test"
    }
}
