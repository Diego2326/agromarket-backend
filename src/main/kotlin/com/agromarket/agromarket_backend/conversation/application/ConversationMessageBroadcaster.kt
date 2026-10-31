package com.agromarket.agromarket_backend.conversation.application

import com.agromarket.agromarket_backend.conversation.domain.ConversationMessage

/** Output port for delivering a committed conversation message to connected clients. */
interface ConversationMessageBroadcaster {
    fun broadcast(message: ConversationMessage)
}
