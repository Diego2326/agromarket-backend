package com.agromarket.agromarket_backend.conversation.application

import com.agromarket.agromarket_backend.conversation.domain.Conversation
import com.agromarket.agromarket_backend.conversation.domain.ConversationMessage
import com.agromarket.agromarket_backend.conversation.domain.ConversationRepository
import com.agromarket.agromarket_backend.publication.domain.PublicationRepository
import com.agromarket.agromarket_backend.publication.domain.PublicationStatus
import com.agromarket.agromarket_backend.shared.domain.ConversationNotFoundException
import com.agromarket.agromarket_backend.shared.domain.ForbiddenOperationException
import com.agromarket.agromarket_backend.shared.domain.InvalidConversationException
import com.agromarket.agromarket_backend.shared.domain.PublicationNotFoundException
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.Clock
import java.time.LocalDateTime
import java.util.UUID

@Service
class ConversationService(
    private val conversations: ConversationRepository,
    private val publications: PublicationRepository,
    private val clock: Clock,
) {
    @Transactional
    fun start(buyerId: UUID, publicationId: UUID): Conversation {
        val publication = publications.findById(publicationId) ?: throw PublicationNotFoundException()
        if (publication.status != PublicationStatus.ACTIVE) {
            throw InvalidConversationException("La publicación no está disponible para iniciar una conversación")
        }
        if (publication.userOwnerId == buyerId) {
            throw InvalidConversationException("No puedes iniciar una conversación contigo mismo")
        }
        conversations.findByContext(publicationId, buyerId, publication.userOwnerId)?.let { return it }
        val now = LocalDateTime.now(clock)
        return conversations.save(
            Conversation(
                id = UUID.randomUUID(),
                publicationId = publication.id,
                publicationTitle = publication.title,
                buyerId = buyerId,
                buyerName = "",
                providerId = publication.userOwnerId,
                providerName = publication.ownerName.orEmpty(),
                createdAt = now,
                updatedAt = now,
            ),
        )
    }

    @Transactional(readOnly = true)
    fun list(userId: UUID): List<Conversation> = conversations.findByParticipant(userId)

    @Transactional(readOnly = true)
    fun messages(userId: UUID, conversationId: UUID): List<ConversationMessage> {
        participant(conversationId, userId)
        return conversations.findMessages(conversationId)
    }

    @Transactional
    fun send(userId: UUID, conversationId: UUID, rawContent: String): ConversationMessage {
        val conversation = participant(conversationId, userId)
        val content = rawContent.trim()
        if (content.isEmpty()) throw InvalidConversationException("El mensaje no puede estar vacío")
        if (content.length > 2000) throw InvalidConversationException("El mensaje no puede superar 2000 caracteres")
        val now = LocalDateTime.now(clock)
        val message = conversations.saveMessage(
            ConversationMessage(
                id = UUID.randomUUID(),
                conversationId = conversation.id,
                senderId = userId,
                senderName = "",
                content = content,
                createdAt = now,
            ),
        )
        conversations.save(conversation.copy(updatedAt = now))
        return message
    }

    private fun participant(conversationId: UUID, userId: UUID): Conversation {
        val conversation = conversations.findById(conversationId) ?: throw ConversationNotFoundException()
        if (userId != conversation.buyerId && userId != conversation.providerId) {
            throw ForbiddenOperationException("No tienes acceso a esta conversación")
        }
        return conversation
    }
}
