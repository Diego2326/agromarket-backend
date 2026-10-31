package com.agromarket.agromarket_backend.conversation.application

import com.agromarket.agromarket_backend.conversation.domain.ConversationMessage

/** Output port for publishing messages across API instances. */
interface ConversationMessagePublisher {
    fun publish(message: ConversationMessage)
}
