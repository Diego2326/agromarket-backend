package com.agromarket.agromarket_backend.conversation.presentation

import com.agromarket.agromarket_backend.auth.infrastructure.security.JwtTokenProvider
import com.agromarket.agromarket_backend.conversation.application.ConversationService
import com.agromarket.agromarket_backend.user.domain.UserRepository
import org.springframework.context.annotation.Configuration
import org.springframework.stereotype.Component
import org.springframework.http.HttpHeaders
import org.springframework.http.server.ServerHttpRequest
import org.springframework.http.server.ServerHttpResponse
import org.springframework.web.socket.WebSocketHandler
import org.springframework.web.socket.config.annotation.EnableWebSocket
import org.springframework.web.socket.config.annotation.WebSocketConfigurer
import org.springframework.web.socket.config.annotation.WebSocketHandlerRegistry
import org.springframework.web.socket.server.HandshakeInterceptor
import java.util.UUID

@Configuration
@EnableWebSocket
class ConversationWebSocketConfiguration(
    private val handler: ConversationWebSocketHandler,
    private val interceptor: ConversationHandshakeInterceptor,
) : WebSocketConfigurer {
    override fun registerWebSocketHandlers(registry: WebSocketHandlerRegistry) {
        registry.addHandler(handler, "/ws/conversations/**")
            .addInterceptors(interceptor)
            .setAllowedOriginPatterns("*")
    }
}

@Component
class ConversationHandshakeInterceptor(
    private val tokens: JwtTokenProvider,
    private val users: UserRepository,
    private val conversations: ConversationService,
) : HandshakeInterceptor {
    override fun beforeHandshake(
        request: ServerHttpRequest,
        response: ServerHttpResponse,
        wsHandler: WebSocketHandler,
        attributes: MutableMap<String, Any>,
    ): Boolean {
        val authorization = request.headers.getFirst(HttpHeaders.AUTHORIZATION)
            ?.takeIf { it.startsWith("Bearer ", ignoreCase = true) }
            ?: return false
        val userId = runCatching { tokens.verifyAndGetUserId(authorization.substringAfter(' ').trim()) }
            .getOrNull() ?: return false
        if (users.findById(userId)?.enabled != true) return false

        val conversationId = request.uri.path.substringAfterLast('/')
        val id = runCatching { UUID.fromString(conversationId) }.getOrNull() ?: return false
        val authorized = runCatching { conversations.messages(userId, id) }.isSuccess
        if (!authorized) return false

        attributes["conversationId"] = id.toString()
        attributes["userId"] = userId
        return true
    }

    override fun afterHandshake(
        request: ServerHttpRequest,
        response: ServerHttpResponse,
        wsHandler: WebSocketHandler,
        exception: Exception?,
    ) = Unit
}
