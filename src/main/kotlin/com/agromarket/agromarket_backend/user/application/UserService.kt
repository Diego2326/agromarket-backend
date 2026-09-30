package com.agromarket.agromarket_backend.user.application

import com.agromarket.agromarket_backend.shared.domain.UserNotFoundException
import com.agromarket.agromarket_backend.user.domain.User
import com.agromarket.agromarket_backend.user.domain.UserRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.Clock
import java.time.Instant
import java.util.UUID

@Service
class UserService(
    private val users: UserRepository,
    private val clock: Clock,
) {
    @Transactional(readOnly = true)
    fun getCurrent(userId: UUID): User = users.findById(userId) ?: throw UserNotFoundException()

    @Transactional
    fun updateCurrent(userId: UUID, command: UpdateProfileCommand): User {
        val current = users.findById(userId) ?: throw UserNotFoundException()
        val updated = current.copy(
            fullName = command.fullName?.trim() ?: current.fullName,
            phone = command.phone.cleanNullableOrKeep(current.phone),
            location = command.location.cleanNullableOrKeep(current.location),
            updatedAt = Instant.now(clock),
        )
        return users.save(updated)
    }

    private fun String?.cleanNullableOrKeep(current: String?): String? = when {
        this == null -> current
        else -> trim().takeIf { it.isNotEmpty() }
    }
}

data class UpdateProfileCommand(
    val fullName: String?,
    val phone: String?,
    val location: String?,
)
