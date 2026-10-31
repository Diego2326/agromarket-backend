package com.agromarket.agromarket_backend.conversation.presentation

import com.agromarket.agromarket_backend.auth.infrastructure.security.AuthenticatedUser
import com.agromarket.agromarket_backend.conversation.application.ConversationService
import com.agromarket.agromarket_backend.conversation.application.ConversationMessagePublisher
import com.agromarket.agromarket_backend.conversation.domain.Conversation
import com.agromarket.agromarket_backend.conversation.domain.ConversationMessage
import jakarta.validation.Valid
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Size
import org.springframework.http.HttpStatus
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.ResponseStatus
import org.springframework.web.bind.annotation.RestController
import java.time.LocalDateTime
import java.util.UUID

@RestController
@RequestMapping("/api/conversations")
class ConversationController(
    private val conversationService: ConversationService,
    private val messagePublisher: ConversationMessagePublisher,
) {
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    fun start(
        @AuthenticationPrincipal principal: AuthenticatedUser,
        @RequestBody request: StartConversationRequest,
    ): ConversationResponse = ConversationResponse.from(
        conversationService.start(principal.id, request.publicationId),
    )

    @GetMapping
    fun list(@AuthenticationPrincipal principal: AuthenticatedUser): List<ConversationResponse> =
        conversationService.list(principal.id).map(ConversationResponse::from)

    @GetMapping("/{id}/messages")
    fun messages(
        @AuthenticationPrincipal principal: AuthenticatedUser,
        @PathVariable id: UUID,
    ): List<MessageResponse> = conversationService.messages(principal.id, id).map(MessageResponse::from)

    @PostMapping("/{id}/messages")
    @ResponseStatus(HttpStatus.CREATED)
    fun send(
        @AuthenticationPrincipal principal: AuthenticatedUser,
        @PathVariable id: UUID,
        @Valid @RequestBody request: SendMessageRequest,
    ): MessageResponse {
        val saved = conversationService.send(principal.id, id, request.content)
        messagePublisher.publish(saved)
        val message = MessageResponse.from(saved)
        return message
    }
}

data class StartConversationRequest(val publicationId: UUID)

data class SendMessageRequest(
    @field:NotBlank(message = "El mensaje es obligatorio")
    @field:Size(max = 2000, message = "El mensaje no puede superar 2000 caracteres")
    val content: String,
)

data class ConversationResponse(
    val id: UUID,
    val publicationId: UUID,
    val publicationTitle: String,
    val buyerId: UUID,
    val buyerName: String,
    val providerId: UUID,
    val providerName: String,
    val createdAt: LocalDateTime,
    val updatedAt: LocalDateTime,
) {
    companion object {
        fun from(value: Conversation) = ConversationResponse(
            value.id,
            value.publicationId,
            value.publicationTitle,
            value.buyerId,
            value.buyerName,
            value.providerId,
            value.providerName,
            value.createdAt,
            value.updatedAt,
        )
    }
}

data class MessageResponse(
    val id: UUID,
    val senderId: UUID,
    val senderName: String,
    val content: String,
    val createdAt: LocalDateTime,
) {
    companion object {
        fun from(value: ConversationMessage) = MessageResponse(
            value.id,
            value.senderId,
            value.senderName,
            value.content,
            value.createdAt,
        )
    }
}
