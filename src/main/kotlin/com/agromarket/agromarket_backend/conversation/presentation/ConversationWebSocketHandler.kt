package com.agromarket.agromarket_backend.conversation.presentation

import com.agromarket.agromarket_backend.conversation.application.ConversationService
import com.agromarket.agromarket_backend.conversation.application.ConversationMessagePublisher
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component
import org.springframework.web.socket.CloseStatus
import org.springframework.web.socket.TextMessage
import org.springframework.web.socket.WebSocketSession
import org.springframework.web.socket.handler.TextWebSocketHandler
import tools.jackson.databind.ObjectMapper
import java.util.UUID

@Component
class ConversationWebSocketHandler(
    private val conversationService: ConversationService,
    private val messagePublisher: ConversationMessagePublisher,
    private val sessions: ConversationWebSocketSessionRegistry,
    private val objectMapper: ObjectMapper,
) : TextWebSocketHandler() {
    private val logger = LoggerFactory.getLogger(javaClass)

    override fun afterConnectionEstablished(session: WebSocketSession) {
        val conversationId = session.attributes["conversationId"] as? String
        if (conversationId == null) {
            session.close(CloseStatus.POLICY_VIOLATION)
            return
        }
        sessions.register(conversationId, session)
    }

    override fun handleTextMessage(session: WebSocketSession, message: TextMessage) {
        val conversationId = session.attributes["conversationId"] as? String
        val userId = session.attributes["userId"] as? UUID
        if (conversationId == null || userId == null) {
            session.close(CloseStatus.POLICY_VIOLATION)
            return
        }

        try {
            val request = objectMapper.readValue(message.payload, SendMessageRequest::class.java)
            val saved = conversationService.send(userId, UUID.fromString(conversationId), request.content)
            messagePublisher.publish(saved)
        } catch (exception: Exception) {
            logger.info("Se rechazó un mensaje WebSocket inválido")
            sessions.sendError(session, "No se pudo enviar el mensaje. Revisa el contenido y tus permisos.")
        }
    }

    override fun afterConnectionClosed(session: WebSocketSession, status: CloseStatus) {
        sessions.unregister(session)
    }

    override fun handleTransportError(session: WebSocketSession, exception: Throwable) {
        sessions.unregister(session)
        if (session.isOpen) session.close(CloseStatus.SERVER_ERROR)
    }
}
