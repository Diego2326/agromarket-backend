package com.agromarket.agromarket_backend.shared.presentation

import com.agromarket.agromarket_backend.shared.domain.DisabledUserException
import com.agromarket.agromarket_backend.shared.domain.EmailAlreadyRegisteredException
import com.agromarket.agromarket_backend.shared.domain.InvalidCredentialsException
import com.agromarket.agromarket_backend.shared.domain.UserNotFoundException
import jakarta.servlet.http.HttpServletRequest
import org.springframework.http.HttpStatus
import org.springframework.http.ProblemDetail
import org.springframework.web.bind.MethodArgumentNotValidException
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.bind.annotation.RestControllerAdvice
import org.springframework.http.converter.HttpMessageNotReadableException
import java.net.URI

@RestControllerAdvice
class GlobalExceptionHandler {
    @ExceptionHandler(EmailAlreadyRegisteredException::class)
    fun duplicateEmail(exception: EmailAlreadyRegisteredException, request: HttpServletRequest): ProblemDetail =
        problem(HttpStatus.CONFLICT, exception.message!!, request)

    @ExceptionHandler(InvalidCredentialsException::class, DisabledUserException::class)
    fun unauthorized(exception: RuntimeException, request: HttpServletRequest): ProblemDetail =
        problem(HttpStatus.UNAUTHORIZED, exception.message!!, request)

    @ExceptionHandler(UserNotFoundException::class)
    fun notFound(exception: UserNotFoundException, request: HttpServletRequest): ProblemDetail =
        problem(HttpStatus.NOT_FOUND, exception.message!!, request)

    @ExceptionHandler(MethodArgumentNotValidException::class)
    fun validation(exception: MethodArgumentNotValidException, request: HttpServletRequest): ProblemDetail {
        val errors = exception.bindingResult.fieldErrors
            .groupBy({ it.field }, { it.defaultMessage ?: "Valor inválido" })
            .mapValues { (_, messages) -> messages.distinct() }
        return problem(HttpStatus.BAD_REQUEST, "La solicitud contiene datos inválidos", request).apply {
            setProperty("errors", errors)
        }
    }

    @ExceptionHandler(HttpMessageNotReadableException::class)
    fun malformedBody(exception: HttpMessageNotReadableException, request: HttpServletRequest): ProblemDetail =
        problem(HttpStatus.BAD_REQUEST, "El cuerpo JSON está vacío o tiene un formato inválido", request)

    private fun problem(status: HttpStatus, detail: String, request: HttpServletRequest): ProblemDetail =
        ProblemDetail.forStatusAndDetail(status, detail).apply {
            title = when (status) {
                HttpStatus.BAD_REQUEST -> "Solicitud inválida"
                HttpStatus.UNAUTHORIZED -> "No autorizado"
                HttpStatus.NOT_FOUND -> "No encontrado"
                HttpStatus.CONFLICT -> "Conflicto"
                else -> status.reasonPhrase
            }
            instance = URI.create(request.requestURI)
        }
}
