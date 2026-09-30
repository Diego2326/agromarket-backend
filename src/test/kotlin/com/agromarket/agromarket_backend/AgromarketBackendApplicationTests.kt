package com.agromarket.agromarket_backend

import org.junit.jupiter.api.Test
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.MediaType
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import org.springframework.transaction.annotation.Transactional
import tools.jackson.databind.ObjectMapper

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
        mockMvc.perform(get("/api/users/me"))
            .andExpect(status().isUnauthorized)
            .andExpect(jsonPath("$.detail").value("Se requiere un token válido"))
    }
}
