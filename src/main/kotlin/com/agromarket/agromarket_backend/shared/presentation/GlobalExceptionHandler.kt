package com.agromarket.agromarket_backend.shared.presentation

import com.agromarket.agromarket_backend.shared.domain.DisabledUserException
import com.agromarket.agromarket_backend.shared.domain.EmailAlreadyRegisteredException
import com.agromarket.agromarket_backend.shared.domain.InvalidCredentialsException
import com.agromarket.agromarket_backend.shared.domain.InvalidPublicationException
import com.agromarket.agromarket_backend.shared.domain.ForbiddenOperationException
import com.agromarket.agromarket_backend.shared.domain.PublicationNotFoundException
import com.agromarket.agromarket_backend.shared.domain.ReservationNotFoundException
import com.agromarket.agromarket_backend.shared.domain.InvalidReservationException
import com.agromarket.agromarket_backend.shared.domain.ConversationNotFoundException
import com.agromarket.agromarket_backend.shared.domain.InvalidConversationException
import com.agromarket.agromarket_backend.shared.domain.InvalidReviewException
import com.agromarket.agromarket_backend.shared.domain.UserNotFoundException
import jakarta.servlet.http.HttpServletRequest
import org.springframework.http.HttpStatus
import org.springframework.http.ProblemDetail
import org.springframework.web.bind.MethodArgumentNotValidException
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.bind.annotation.RestControllerAdvice
import org.springframework.http.converter.HttpMessageNotReadableException
import org.springframework.dao.CannotAcquireLockException
import org.springframework.dao.DataIntegrityViolationException
import org.springframework.orm.ObjectOptimisticLockingFailureException
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException
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

    @ExceptionHandler(PublicationNotFoundException::class)
    fun publicationNotFound(exception: PublicationNotFoundException, request: HttpServletRequest): ProblemDetail =
        problem(HttpStatus.NOT_FOUND, exception.message!!, request)

    @ExceptionHandler(ReservationNotFoundException::class, ConversationNotFoundException::class)
    fun resourceNotFound(exception: RuntimeException, request: HttpServletRequest): ProblemDetail =
        problem(HttpStatus.NOT_FOUND, exception.message!!, request)

    @ExceptionHandler(InvalidPublicationException::class)
    fun invalidPublication(exception: InvalidPublicationException, request: HttpServletRequest): ProblemDetail =
        problem(HttpStatus.BAD_REQUEST, exception.message!!, request)

    @ExceptionHandler(
        InvalidReservationException::class,
        InvalidConversationException::class,
        InvalidReviewException::class,
    )
    fun invalidBusinessOperation(exception: RuntimeException, request: HttpServletRequest): ProblemDetail =
        problem(HttpStatus.BAD_REQUEST, exception.message!!, request)

    @ExceptionHandler(ForbiddenOperationException::class)
    fun forbidden(exception: ForbiddenOperationException, request: HttpServletRequest): ProblemDetail =
        problem(HttpStatus.FORBIDDEN, exception.message!!, request)

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

    @ExceptionHandler(MethodArgumentTypeMismatchException::class)
    fun invalidParameter(exception: MethodArgumentTypeMismatchException, request: HttpServletRequest): ProblemDetail =
        problem(HttpStatus.BAD_REQUEST, "El parámetro '${exception.name}' tiene un valor inválido", request)

    @ExceptionHandler(
        ObjectOptimisticLockingFailureException::class,
        CannotAcquireLockException::class,
    )
    fun concurrentConflict(exception: RuntimeException, request: HttpServletRequest): ProblemDetail =
        problem(HttpStatus.CONFLICT, "El recurso cambió durante la operación; vuelve a intentarlo", request)

    @ExceptionHandler(DataIntegrityViolationException::class)
    fun integrityConflict(exception: DataIntegrityViolationException, request: HttpServletRequest): ProblemDetail =
        problem(HttpStatus.CONFLICT, "La operación entra en conflicto con datos existentes", request)

    private fun problem(status: HttpStatus, detail: String, request: HttpServletRequest): ProblemDetail =
        ProblemDetail.forStatusAndDetail(status, detail).apply {
            title = when (status) {
                HttpStatus.BAD_REQUEST -> "Solicitud inválida"
                HttpStatus.UNAUTHORIZED -> "No autorizado"
                HttpStatus.FORBIDDEN -> "Operación no permitida"
                HttpStatus.NOT_FOUND -> "No encontrado"
                HttpStatus.CONFLICT -> "Conflicto"
                else -> status.reasonPhrase
            }
            instance = URI.create(request.requestURI)
        }
}
