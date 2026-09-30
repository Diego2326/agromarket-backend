
package com.agromarket.agromarket_backend.shared.domain

class EmailAlreadyRegisteredException : RuntimeException("El correo electrónico ya está registrado")

class InvalidCredentialsException : RuntimeException("Correo electrónico o contraseña incorrectos")

class UserNotFoundException : RuntimeException("Usuario no encontrado")

class DisabledUserException : RuntimeException("La cuenta de usuario está deshabilitada")

class PublicationNotFoundException : RuntimeException("Publicación no encontrada")

class InvalidPublicationException(message: String) : RuntimeException(message)

class ForbiddenOperationException(message: String) : RuntimeException(message)

class ReservationNotFoundException : RuntimeException("Reserva no encontrada")

class InvalidReservationException(message: String) : RuntimeException(message)

class ConversationNotFoundException : RuntimeException("Conversación no encontrada")

class InvalidConversationException(message: String) : RuntimeException(message)

class InvalidReviewException(message: String) : RuntimeException(message)
