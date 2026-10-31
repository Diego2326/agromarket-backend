package com.agromarket.agromarket_backend.conversation.presentation

import com.agromarket.agromarket_backend.conversation.application.ConversationMessageBroadcaster
import com.agromarket.agromarket_backend.conversation.domain.ConversationMessage
import org.springframework.stereotype.Component
import org.springframework.web.socket.TextMessage
import org.springframework.web.socket.WebSocketSession
import tools.jackson.databind.ObjectMapper
import java.util.concurrent.ConcurrentHashMap

data class ConversationSocketEvent(
    val type: String,
    val conversationId: String? = null,
    val message: MessageResponse? = null,
    val detail: String? = null,
)

@Component
class ConversationWebSocketSessionRegistry(
    private val objectMapper: ObjectMapper,
) : ConversationMessageBroadcaster {
    private val sessions = ConcurrentHashMap<String, MutableSet<WebSocketSession>>()
    private val deliveredMessages = ConcurrentHashMap<String, Long>()

    fun register(conversationId: String, session: WebSocketSession) {
        sessions.computeIfAbsent(conversationId) { ConcurrentHashMap.newKeySet() }.add(session)
        send(session, ConversationSocketEvent(type = "connected", conversationId = conversationId))
    }

    fun unregister(session: WebSocketSession) {
        sessions.values.forEach { conversationSessions -> conversationSessions.remove(session) }
        sessions.entries.removeIf { it.value.isEmpty() }
    }

    fun sendError(session: WebSocketSession, detail: String) {
        send(session, ConversationSocketEvent(type = "error", detail = detail))
    }

    override fun broadcast(message: ConversationMessage) {
        if (!rememberFirstDelivery(message.id.toString())) return
        val conversationId = message.conversationId.toString()
        val event = ConversationSocketEvent(
            type = "message",
            conversationId = conversationId,
            message = MessageResponse.from(message),
        )
        sessions[conversationId]?.toList()?.forEach { send(it, event) }
    }

    private fun rememberFirstDelivery(messageId: String): Boolean {
        val now = System.currentTimeMillis()
        if (deliveredMessages.putIfAbsent(messageId, now) != null) return false
        if (deliveredMessages.size > 4_096) deliveredMessages.entries.removeIf { now - it.value > 120_000 }
        return true
    }

    private fun send(session: WebSocketSession, event: ConversationSocketEvent) {
        if (!session.isOpen) {
            unregister(session)
            return
        }
        val payload = runCatching { objectMapper.writeValueAsString(event) }.getOrNull() ?: return
        runCatching {
            synchronized(session) {
                if (session.isOpen) session.sendMessage(TextMessage(payload))
            }
        }.onFailure { unregister(session) }
    }
}
