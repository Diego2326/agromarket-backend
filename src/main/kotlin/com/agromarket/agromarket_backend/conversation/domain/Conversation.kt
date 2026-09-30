package com.agromarket.agromarket_backend.conversation.domain

import java.time.LocalDateTime
import java.util.UUID

data class Conversation(
    val id: UUID,
    val publicationId: UUID,
    val publicationTitle: String,
    val buyerId: UUID,
    val buyerName: String,
    val providerId: UUID,
    val providerName: String,
    val createdAt: LocalDateTime,
    val updatedAt: LocalDateTime,
)

data class ConversationMessage(
    val id: UUID,
    val conversationId: UUID,
    val senderId: UUID,
    val senderName: String,
    val content: String,
    val createdAt: LocalDateTime,
)
