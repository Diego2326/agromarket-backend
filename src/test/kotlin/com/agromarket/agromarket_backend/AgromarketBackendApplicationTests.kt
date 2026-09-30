package com.agromarket.agromarket_backend

import org.junit.jupiter.api.Test
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.MediaType
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import org.springframework.transaction.annotation.Transactional
import tools.jackson.databind.ObjectMapper
import java.time.LocalDate

@SpringBootTest(
    properties = [
        "spring.datasource.url=jdbc:h2:mem:agromarket;MODE=PostgreSQL;DB_CLOSE_DELAY=-1",
        "spring.datasource.driver-class-name=org.h2.Driver",
        "spring.datasource.username=sa",
        "spring.datasource.password=",
        "app.security.jwt.secret=test-secret-with-at-least-thirty-two-characters",
    ],
)
@AutoConfigureMockMvc
@Transactional
class AgromarketBackendApplicationTests @Autowired constructor(
    private val mockMvc: MockMvc,
    private val objectMapper: ObjectMapper,
) {
    @Test
    fun contextLoads() {
    }

    @Test
    fun `register login and current profile work end to end`() {
        val registration = mockMvc.perform(
            post("/api/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    """
                    {
                      "email": "producer.integration@example.com",
                      "password": "password123",
                      "fullName": "Productora Integración",
                      "location": "Guatemala"
                    }
                    """.trimIndent(),
                ),
        )
            .andExpect(status().isCreated)
            .andExpect(jsonPath("$.tokenType").value("Bearer"))
            .andExpect(jsonPath("$.user.email").value("producer.integration@example.com"))
            .andReturn()

        val token = objectMapper.readTree(registration.response.contentAsString)["accessToken"].asString()

        mockMvc.perform(get("/api/users/me").header("Authorization", "Bearer $token"))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.fullName").value("Productora Integración"))

        mockMvc.perform(
            patch("/api/users/me")
                .header("Authorization", "Bearer $token")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""{"phone":"5555-9876","location":"Escuintla"}"""),
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.phone").value("5555-9876"))
            .andExpect(jsonPath("$.location").value("Escuintla"))

        mockMvc.perform(
            post("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    """{"email":"PRODUCER.INTEGRATION@example.com","password":"password123"}""",
                ),
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.accessToken").isString)
    }

    @Test
    fun `protected endpoint rejects missing token`() {
        mockMvc.perform(get("/api/health"))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.status").value("UP"))

        mockMvc.perform(get("/api/users/me"))
            .andExpect(status().isUnauthorized)
            .andExpect(jsonPath("$.detail").value("Se requiere un token válido"))
    }

    @Test
    fun `publication lifecycle enforces ownership visibility and catalogs`() {
        val ownerToken = registerAndGetToken("owner.publications@example.com", "Proveedor Uno")
        val otherToken = registerAndGetToken("other.publications@example.com", "Proveedor Dos")

        val categoriesResponse = mockMvc.perform(
            get("/api/categories").header("Authorization", "Bearer $ownerToken"),
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$[0].products").isArray)
            .andReturn()
        val categories = objectMapper.readTree(categoriesResponse.response.contentAsString)
        val agricultural = categories.first { it["code"].asString() == "AGRICULTURAL" }
        val categoryId = agricultural["id"].asInt()
        val productId = agricultural["products"][0]["id"].asInt()

        val unitTypesResponse = mockMvc.perform(
            get("/api/unit-types").header("Authorization", "Bearer $ownerToken"),
        )
            .andExpect(status().isOk)
            .andReturn()
        val unitTypeId = objectMapper.readTree(unitTypesResponse.response.contentAsString)[0]["id"].asInt()
        val availableDate = LocalDate.now().plusDays(10)

        val createdResponse = mockMvc.perform(
            post("/api/publications")
                .header("Authorization", "Bearer $ownerToken")
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    """
                    {
                      "categoryId": $categoryId,
                      "productId": $productId,
                      "title": "Maíz amarillo premium",
                      "description": "Cosecha de primera calidad",
                      "price": 225.50,
                      "units": 100,
                      "unitTypeId": $unitTypeId,
                      "location": "Zacapa",
                      "availableDate": "$availableDate"
                    }
                    """.trimIndent(),
                ),
        )
            .andExpect(status().isCreated)
            .andExpect(jsonPath("$.status").value("ACTIVE"))
            .andExpect(jsonPath("$.unitsAvailable").value(100))
            .andExpect(jsonPath("$.currencyCode").value("GTQ"))
            .andReturn()
        val publicationId = objectMapper.readTree(createdResponse.response.contentAsString)["id"].asString()

        mockMvc.perform(
            get("/api/publications")
                .header("Authorization", "Bearer $otherToken")
                .queryParam("query", "amarillo")
                .queryParam("location", "Zacapa")
                .queryParam("minPrice", "200")
                .queryParam("maxPrice", "250"),
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.totalElements").value(1))
            .andExpect(jsonPath("$.content[0].id").value(publicationId))

        mockMvc.perform(
            put("/api/publications/$publicationId")
                .header("Authorization", "Bearer $otherToken")
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    """
                    {
                      "categoryId": $categoryId,
                      "productId": $productId,
                      "title": "Intento ajeno",
                      "price": 200,
                      "units": 100,
                      "unitTypeId": $unitTypeId
                    }
                    """.trimIndent(),
                ),
        )
            .andExpect(status().isForbidden)

        mockMvc.perform(
            put("/api/publications/$publicationId")
                .header("Authorization", "Bearer $ownerToken")
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    """
                    {
                      "categoryId": $categoryId,
                      "productId": $productId,
                      "title": "Maíz amarillo actualizado",
                      "description": "Nueva cosecha",
                      "price": 230.75,
                      "units": 120,
                      "unitTypeId": $unitTypeId,
                      "location": "Zacapa",
                      "availableDate": "$availableDate"
                    }
                    """.trimIndent(),
                ),
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.title").value("Maíz amarillo actualizado"))
            .andExpect(jsonPath("$.unitsAvailable").value(120))
            .andExpect(jsonPath("$.ownerName").value("Proveedor Uno"))

        mockMvc.perform(
            get("/api/publications/$publicationId").header("Authorization", "Bearer $otherToken"),
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.price").value(230.75))
            .andExpect(jsonPath("$.ownerName").value("Proveedor Uno"))

        mockMvc.perform(
            patch("/api/publications/$publicationId/status")
                .header("Authorization", "Bearer $ownerToken")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""{"status":"PAUSED"}"""),
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.status").value("PAUSED"))

        mockMvc.perform(
            get("/api/publications").header("Authorization", "Bearer $otherToken"),
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.totalElements").value(0))

        mockMvc.perform(
            get("/api/publications/mine").header("Authorization", "Bearer $ownerToken"),
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.totalElements").value(1))
            .andExpect(jsonPath("$.content[0].status").value("PAUSED"))
    }

    @Test
    fun `commercial flow keeps stock permissions chat reviews favorites and analytics consistent`() {
        val providerToken = registerAndGetToken("provider.flow@example.com", "Proveedor Flujo")
        val buyerToken = registerAndGetToken("buyer.flow@example.com", "Comprador Flujo")
        val outsiderToken = registerAndGetToken("outsider.flow@example.com", "Usuario Ajeno")
        val catalog = catalogIds(providerToken)
        val availableDate = LocalDate.now().plusDays(15)

        val publicationResponse = mockMvc.perform(
            post("/api/publications")
                .header("Authorization", "Bearer $providerToken")
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    """
                    {
                      "categoryId": ${catalog.categoryId},
                      "productId": ${catalog.productId},
                      "title": "Café para flujo comercial",
                      "description": "Lote de prueba",
                      "price": 100.00,
                      "units": 100,
                      "unitTypeId": ${catalog.unitTypeId},
                      "location": "Zacapa",
                      "availableDate": "$availableDate"
                    }
                    """.trimIndent(),
                ),
        ).andExpect(status().isCreated).andReturn()
        val publication = objectMapper.readTree(publicationResponse.response.contentAsString)
        val publicationId = publication["id"].asString()
        val providerId = publication["ownerId"].asString()
        val productId = publication["product"]["id"].asInt()

        mockMvc.perform(
            post("/api/favorites/$publicationId").header("Authorization", "Bearer $buyerToken"),
        ).andExpect(status().isNoContent)
        mockMvc.perform(get("/api/favorites").header("Authorization", "Bearer $buyerToken"))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$[0].id").value(publicationId))

        val reservationResponse = mockMvc.perform(
            post("/api/reservations")
                .header("Authorization", "Bearer $buyerToken")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""{"publicationId":"$publicationId","quantity":3}"""),
        )
            .andExpect(status().isCreated)
            .andExpect(jsonPath("$.status").value("PENDING_PROVIDER"))
            .andExpect(jsonPath("$.total").value(300.0))
            .andReturn()
        val reservation = objectMapper.readTree(reservationResponse.response.contentAsString)
        val reservationId = reservation["id"].asString()

        mockMvc.perform(
            get("/api/publications/$publicationId").header("Authorization", "Bearer $buyerToken"),
        ).andExpect(status().isOk).andExpect(jsonPath("$.unitsAvailable").value(97))

        mockMvc.perform(
            post("/api/reservations")
                .header("Authorization", "Bearer $buyerToken")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""{"publicationId":"$publicationId","quantity":98}"""),
        ).andExpect(status().isBadRequest)

        mockMvc.perform(
            patch("/api/reservations/$reservationId/accept")
                .header("Authorization", "Bearer $outsiderToken"),
        ).andExpect(status().isForbidden)

        mockMvc.perform(
            get("/api/reservations/received").header("Authorization", "Bearer $providerToken"),
        ).andExpect(status().isOk).andExpect(jsonPath("$[0].id").value(reservationId))

        listOf(
            providerToken to "accept",
            buyerToken to "confirm",
            providerToken to "ready",
            providerToken to "delivered",
            buyerToken to "complete",
        ).forEach { (token, action) ->
            mockMvc.perform(
                patch("/api/reservations/$reservationId/$action")
                    .header("Authorization", "Bearer $token"),
            ).andExpect(status().isOk)
        }

        val secondReservationResponse = mockMvc.perform(
            post("/api/reservations")
                .header("Authorization", "Bearer $buyerToken")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""{"publicationId":"$publicationId","quantity":5}"""),
        ).andExpect(status().isCreated).andReturn()
        val secondReservationId = objectMapper.readTree(secondReservationResponse.response.contentAsString)["id"].asString()
        mockMvc.perform(
            patch("/api/reservations/$secondReservationId/reject")
                .header("Authorization", "Bearer $providerToken"),
        ).andExpect(status().isOk).andExpect(jsonPath("$.status").value("REJECTED"))
        mockMvc.perform(
            patch("/api/reservations/$secondReservationId/reject")
                .header("Authorization", "Bearer $providerToken"),
        ).andExpect(status().isBadRequest)
        mockMvc.perform(
            get("/api/publications/$publicationId").header("Authorization", "Bearer $buyerToken"),
        ).andExpect(status().isOk).andExpect(jsonPath("$.unitsAvailable").value(97))

        val conversationResponse = mockMvc.perform(
            post("/api/conversations")
                .header("Authorization", "Bearer $buyerToken")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""{"publicationId":"$publicationId"}"""),
        ).andExpect(status().isCreated).andReturn()
        val conversationId = objectMapper.readTree(conversationResponse.response.contentAsString)["id"].asString()

        mockMvc.perform(
            post("/api/conversations/$conversationId/messages")
                .header("Authorization", "Bearer $buyerToken")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""{"content":"¿Tiene disponibilidad inmediata?"}"""),
        ).andExpect(status().isCreated)
        mockMvc.perform(
            post("/api/conversations/$conversationId/messages")
                .header("Authorization", "Bearer $providerToken")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""{"content":"Sí, podemos coordinar la entrega."}"""),
        ).andExpect(status().isCreated)
        mockMvc.perform(
            get("/api/conversations/$conversationId/messages")
                .header("Authorization", "Bearer $outsiderToken"),
        ).andExpect(status().isForbidden)

        mockMvc.perform(
            post("/api/reviews/providers")
                .header("Authorization", "Bearer $buyerToken")
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    """{"conversationId":"$conversationId","purchased":true,"rating":5,"comment":"Excelente atención"}""",
                ),
        ).andExpect(status().isCreated)
        mockMvc.perform(
            post("/api/reviews/products")
                .header("Authorization", "Bearer $buyerToken")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""{"reservationId":"$reservationId","rating":4,"comment":"Buen producto"}"""),
        ).andExpect(status().isCreated)

        mockMvc.perform(
            get("/api/reputation/providers/$providerId").header("Authorization", "Bearer $buyerToken"),
        ).andExpect(status().isOk)
            .andExpect(jsonPath("$.average").value(5.0))
            .andExpect(jsonPath("$.count").value(1))
        mockMvc.perform(
            get("/api/reputation/products/$productId").header("Authorization", "Bearer $buyerToken"),
        ).andExpect(status().isOk)
            .andExpect(jsonPath("$.average").value(4.0))

        mockMvc.perform(
            get("/api/analytics/me").header("Authorization", "Bearer $providerToken"),
        ).andExpect(status().isOk)
            .andExpect(jsonPath("$.activePublications").value(1))
            .andExpect(jsonPath("$.reservationsReceived").value(2))
            .andExpect(jsonPath("$.unitsCommitted").value(3))
            .andExpect(jsonPath("$.completedOperations").value(1))

        mockMvc.perform(
            delete("/api/favorites/$publicationId").header("Authorization", "Bearer $buyerToken"),
        ).andExpect(status().isNoContent)
        mockMvc.perform(get("/api/favorites").header("Authorization", "Bearer $buyerToken"))
            .andExpect(status().isOk).andExpect(jsonPath("$").isEmpty)
    }

    private fun registerAndGetToken(email: String, fullName: String): String {
        val response = mockMvc.perform(
            post("/api/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    """
                    {
                      "email": "$email",
                      "password": "password123",
                      "fullName": "$fullName"
                    }
                    """.trimIndent(),
                ),
        )
            .andExpect(status().isCreated)
            .andReturn()
        return objectMapper.readTree(response.response.contentAsString)["accessToken"].asString()
    }

    private fun catalogIds(token: String): CatalogIds {
        val categoriesResponse = mockMvc.perform(
            get("/api/categories").header("Authorization", "Bearer $token"),
        ).andExpect(status().isOk).andReturn()
        val category = objectMapper.readTree(categoriesResponse.response.contentAsString)
            .first { it["code"].asString() == "AGRICULTURAL" }
        val unitsResponse = mockMvc.perform(
            get("/api/unit-types").header("Authorization", "Bearer $token"),
        ).andExpect(status().isOk).andReturn()
        return CatalogIds(
            categoryId = category["id"].asInt(),
            productId = category["products"][0]["id"].asInt(),
            unitTypeId = objectMapper.readTree(unitsResponse.response.contentAsString)[0]["id"].asInt(),
        )
    }

    private data class CatalogIds(val categoryId: Int, val productId: Int, val unitTypeId: Int)
}
