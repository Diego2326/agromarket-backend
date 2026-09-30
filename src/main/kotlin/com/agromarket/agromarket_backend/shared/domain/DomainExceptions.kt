
package com.agromarket.agromarket_backend.shared.domain

class EmailAlreadyRegisteredException : RuntimeException("El correo electrónico ya está registrado")

class InvalidCredentialsException : RuntimeException("Correo electrónico o contraseña incorrectos")

class UserNotFoundException : RuntimeException("Usuario no encontrado")

class DisabledUserException : RuntimeException("La cuenta de usuario está deshabilitada")
