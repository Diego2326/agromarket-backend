package com.agromarket.agromarket_backend.conversation.infrastructure.persistence

import com.agromarket.agromarket_backend.conversation.domain.Conversation
import com.agromarket.agromarket_backend.conversation.domain.ConversationMessage
import com.agromarket.agromarket_backend.conversation.domain.ConversationRepository
import com.agromarket.agromarket_backend.publication.infrastructure.persistence.PublicationJpaEntity
import com.agromarket.agromarket_backend.publication.infrastructure.persistence.SpringDataPublicationRepository
import com.agromarket.agromarket_backend.user.infrastructure.persistence.SpringDataUserRepository
import com.agromarket.agromarket_backend.user.infrastructure.persistence.UserJpaEntity
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.FetchType
import jakarta.persistence.Id
import jakarta.persistence.JoinColumn
import jakarta.persistence.ManyToOne
import jakarta.persistence.Table
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import org.springframework.stereotype.Repository
import java.time.LocalDateTime
import java.util.UUID

@Entity
@Table(name = "conversations")
class ConversationJpaEntity(
    @Id
    var id: UUID,

    @ManyToOne(fetch = FetchType.EAGER, optional = false)
    @JoinColumn(name = "publication_id", nullable = false)
    var publication: PublicationJpaEntity,

    @ManyToOne(fetch = FetchType.EAGER, optional = false)
    @JoinColumn(name = "buyer_id", nullable = false)
    var buyer: UserJpaEntity,

    @ManyToOne(fetch = FetchType.EAGER, optional = false)
    @JoinColumn(name = "provider_id", nullable = false)
    var provider: UserJpaEntity,

    @Column(name = "created_at", nullable = false, updatable = false)
    var createdAt: LocalDateTime,

    @Column(name = "updated_at", nullable = false)
    var updatedAt: LocalDateTime,
)

@Entity
@Table(name = "conversation_messages")
class ConversationMessageJpaEntity(
    @Id
    var id: UUID,

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "conversation_id", nullable = false)
    var conversation: ConversationJpaEntity,

    @ManyToOne(fetch = FetchType.EAGER, optional = false)
    @JoinColumn(name = "sender_id", nullable = false)
    var sender: UserJpaEntity,

    @Column(nullable = false, length = 2000)
    var content: String,

    @Column(name = "created_at", nullable = false)
    var createdAt: LocalDateTime,
)

interface SpringDataConversationRepository : JpaRepository<ConversationJpaEntity, UUID> {
    fun findByPublicationIdAndBuyerIdAndProviderId(
        publicationId: UUID,
        buyerId: UUID,
        providerId: UUID,
    ): ConversationJpaEntity?

    @Query(
        """
        SELECT c FROM ConversationJpaEntity c
        WHERE c.buyer.id = :userId OR c.provider.id = :userId
        ORDER BY c.updatedAt DESC
        """,
    )
    fun findByParticipant(@Param("userId") userId: UUID): List<ConversationJpaEntity>
}

interface SpringDataConversationMessageRepository : JpaRepository<ConversationMessageJpaEntity, UUID> {
    fun findAllByConversationIdOrderByCreatedAtAsc(conversationId: UUID): List<ConversationMessageJpaEntity>
    fun existsByConversationIdAndSenderId(conversationId: UUID, senderId: UUID): Boolean
}

@Repository
class JpaConversationRepositoryAdapter(
    private val conversationRepository: SpringDataConversationRepository,
    private val messageRepository: SpringDataConversationMessageRepository,
    private val publicationRepository: SpringDataPublicationRepository,
    private val userRepository: SpringDataUserRepository,
) : ConversationRepository {
    override fun save(conversation: Conversation): Conversation =
        conversationRepository.saveAndFlush(conversation.toEntity()).toDomain()

    override fun findByContext(publicationId: UUID, buyerId: UUID, providerId: UUID): Conversation? =
        conversationRepository.findByPublicationIdAndBuyerIdAndProviderId(publicationId, buyerId, providerId)?.toDomain()

    override fun findById(id: UUID): Conversation? =
        conversationRepository.findById(id).orElse(null)?.toDomain()

    override fun findByParticipant(userId: UUID): List<Conversation> =
        conversationRepository.findByParticipant(userId).map { it.toDomain() }

    override fun saveMessage(message: ConversationMessage): ConversationMessage =
        messageRepository.saveAndFlush(
            ConversationMessageJpaEntity(
                id = message.id,
                conversation = conversationRepository.getReferenceById(message.conversationId),
                sender = userRepository.getReferenceById(message.senderId),
                content = message.content,
                createdAt = message.createdAt,
            ),
        ).toDomain()

    override fun findMessages(conversationId: UUID): List<ConversationMessage> =
        messageRepository.findAllByConversationIdOrderByCreatedAtAsc(conversationId).map { it.toDomain() }

    override fun hasMessagesFromBothParticipants(conversationId: UUID, buyerId: UUID, providerId: UUID): Boolean =
        messageRepository.existsByConversationIdAndSenderId(conversationId, buyerId) &&
            messageRepository.existsByConversationIdAndSenderId(conversationId, providerId)

    private fun Conversation.toEntity() = ConversationJpaEntity(
        id = id,
        publication = publicationRepository.getReferenceById(publicationId),
        buyer = userRepository.getReferenceById(buyerId),
        provider = userRepository.getReferenceById(providerId),
        createdAt = createdAt,
        updatedAt = updatedAt,
    )

    private fun ConversationJpaEntity.toDomain() = Conversation(
        id = id,
        publicationId = publication.id,
        publicationTitle = publication.title,
        buyerId = buyer.id,
        buyerName = buyer.fullName,
        providerId = provider.id,
        providerName = provider.fullName,
        createdAt = createdAt,
        updatedAt = updatedAt,
    )

    private fun ConversationMessageJpaEntity.toDomain() = ConversationMessage(
        id = id,
        conversationId = conversation.id,
        senderId = sender.id,
        senderName = sender.fullName,
        content = content,
        createdAt = createdAt,
    )
}
