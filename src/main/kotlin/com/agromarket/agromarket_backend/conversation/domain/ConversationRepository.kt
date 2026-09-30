package com.agromarket.agromarket_backend.conversation.domain

import java.util.UUID

interface ConversationRepository {
    fun save(conversation: Conversation): Conversation
    fun findByContext(publicationId: UUID, buyerId: UUID, providerId: UUID): Conversation?
    fun findById(id: UUID): Conversation?
    fun findByParticipant(userId: UUID): List<Conversation>
    fun saveMessage(message: ConversationMessage): ConversationMessage
    fun findMessages(conversationId: UUID): List<ConversationMessage>
    fun hasMessagesFromBothParticipants(conversationId: UUID, buyerId: UUID, providerId: UUID): Boolean
}
