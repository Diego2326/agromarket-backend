package com.agromarket.agromarket_backend.conversation.infrastructure.messaging

import com.agromarket.agromarket_backend.conversation.application.ConversationMessageBroadcaster
import com.agromarket.agromarket_backend.conversation.application.ConversationMessagePublisher
import com.agromarket.agromarket_backend.conversation.domain.ConversationMessage
import org.slf4j.LoggerFactory
import org.springframework.jdbc.core.ConnectionCallback
import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.stereotype.Component
import tools.jackson.databind.ObjectMapper

internal data class ConversationMessageNotification(
    val type: String,
    val conversationId: String,
    val message: ConversationMessage,
)

@Component
class PostgresConversationMessagePublisher(
    private val objectMapper: ObjectMapper,
    private val jdbcTemplate: JdbcTemplate,
    private val broadcaster: ConversationMessageBroadcaster,
) : ConversationMessagePublisher {
    private val logger = LoggerFactory.getLogger(javaClass)

    override fun publish(message: ConversationMessage) {
        val event = ConversationMessageNotification(
            type = "message",
            conversationId = message.conversationId.toString(),
            message = message,
        )
        val payload = objectMapper.writeValueAsString(event)
        runCatching {
            jdbcTemplate.execute(ConnectionCallback<Unit> { connection ->
                connection.prepareStatement("SELECT pg_notify(?, ?)").use { statement ->
                    statement.setString(1, "agromarket_messages")
                    statement.setString(2, payload)
                    statement.execute()
                }
                Unit
            })
        }.onFailure {
            logger.warn("No se pudo propagar un mensaje de conversación a las otras instancias")
        }
        broadcaster.broadcast(message)
    }
}
