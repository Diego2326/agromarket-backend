package com.agromarket.agromarket_backend.conversation.infrastructure.messaging

import com.agromarket.agromarket_backend.conversation.application.ConversationMessageBroadcaster
import jakarta.annotation.PostConstruct
import jakarta.annotation.PreDestroy
import org.postgresql.PGConnection
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Component
import tools.jackson.databind.ObjectMapper
import java.sql.DriverManager
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit

@Component
class PostgresMessageNotificationListener(
    @Value("\${app.messaging.db-listener-url:}") private val databaseUrl: String,
    @Value("\${spring.datasource.username:}") private val username: String,
    @Value("\${spring.datasource.password:}") private val password: String,
    private val objectMapper: ObjectMapper,
    private val broadcaster: ConversationMessageBroadcaster,
) {
    private val logger = LoggerFactory.getLogger(javaClass)
    private val running = java.util.concurrent.atomic.AtomicBoolean(false)
    private val executor = Executors.newSingleThreadExecutor { task ->
        Thread(task, "agromarket-message-notifications").apply { isDaemon = true }
    }

    @PostConstruct
    fun start() {
        // H2-based tests do not need cross-instance notifications.
        if (!databaseUrl.startsWith("jdbc:postgresql:")) return
        running.set(true)
        executor.submit(::listenForever)
    }

    @PreDestroy
    fun stop() {
        running.set(false)
        executor.shutdownNow()
        executor.awaitTermination(2, TimeUnit.SECONDS)
    }

    private fun listenForever() {
        while (running.get() && !Thread.currentThread().isInterrupted) {
            try {
                DriverManager.getConnection(databaseUrl, username, password).use { connection ->
                    connection.createStatement().use { it.execute("LISTEN agromarket_messages") }
                    val postgres = connection.unwrap(PGConnection::class.java)
                    while (running.get() && !Thread.currentThread().isInterrupted) {
                        postgres.getNotifications(1_000)?.forEach { notification ->
                            val event = runCatching {
                                objectMapper.readValue(notification.parameter, ConversationMessageNotification::class.java)
                            }.getOrNull()
                            if (event?.type == "message") broadcaster.broadcast(event.message)
                        }
                    }
                }
            } catch (_: InterruptedException) {
                Thread.currentThread().interrupt()
            } catch (_: Exception) {
                if (running.get()) {
                    logger.warn("La escucha de mensajes de PostgreSQL se desconectó; reintentando")
                    try {
                        Thread.sleep(2_000)
                    } catch (_: InterruptedException) {
                        Thread.currentThread().interrupt()
                    }
                }
            }
        }
    }
}
