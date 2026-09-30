package com.agromarket.agromarket_backend

import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.jdbc.core.simple.JdbcClient
import kotlin.test.assertEquals

@SpringBootTest(
    properties = [
        "spring.datasource.url=jdbc:h2:mem:agromarket-demo;MODE=PostgreSQL;DB_CLOSE_DELAY=-1",
        "spring.datasource.driver-class-name=org.h2.Driver",
        "spring.datasource.username=sa",
        "spring.datasource.password=",
        "app.security.jwt.secret=test-secret-with-at-least-thirty-two-characters",
        "app.demo-data.enabled=true",
    ],
)
class DemoDataSeederTest @Autowired constructor(
    private val jdbc: JdbcClient,
) {
    @Test
    fun `demo seed creates a reproducible commercial dataset`() {
        assertEquals(12L, count("users"))
        assertEquals(24L, count("publication"))
        assertEquals(10L, count("reservations"))
        assertEquals(5L, count("conversations"))
        assertEquals(5L, count("supplier_reviews"))
        assertEquals(3L, count("product_reviews"))
    }

    private fun count(table: String): Long =
        jdbc.sql("SELECT COUNT(*) FROM $table").query(Long::class.java).single()
}
