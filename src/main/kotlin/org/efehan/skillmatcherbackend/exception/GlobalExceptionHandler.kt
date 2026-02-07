package org.efehan.skillmatcherbackend.exception

import jakarta.servlet.http.HttpServletRequest
import org.efehan.skillmatcherbackend.shared.exceptions.AccountDisabledException
import org.efehan.skillmatcherbackend.shared.exceptions.DuplicateEntryException
import org.efehan.skillmatcherbackend.shared.exceptions.EntryNotFoundException
import org.efehan.skillmatcherbackend.shared.exceptions.InvalidCredentialsException
import org.efehan.skillmatcherbackend.shared.exceptions.InvalidTokenException
import org.efehan.skillmatcherbackend.shared.exceptions.PasswordValidationException
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpStatus
import org.springframework.http.HttpStatusCode
import org.springframework.http.ResponseEntity
import org.springframework.http.converter.HttpMessageNotReadableException
import org.springframework.security.access.AccessDeniedException
import org.springframework.web.bind.MethodArgumentNotValidException
import org.springframework.web.bind.annotation.ControllerAdvice
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.context.request.WebRequest
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler

@ControllerAdvice
class GlobalExceptionHandler : ResponseEntityExceptionHandler() {
    @ExceptionHandler(InvalidCredentialsException::class)
    fun handleInvalidCredentials(
        exception: InvalidCredentialsException,
        request: HttpServletRequest,
    ): ResponseEntity<GlobalErrorCodeResponse> {
        logger.error(
            "InvalidCredentialsException [${request.method} ${request.requestURI}]",
            exception,
        )
        val body = GlobalErrorCodeResponse(errorCode = exception.errorCode)
        return ResponseEntity(body, exception.status)
    }

    @ExceptionHandler(InvalidTokenException::class)
    fun handleInvalidToken(
        exception: InvalidTokenException,
        request: HttpServletRequest,
    ): ResponseEntity<GlobalErrorCodeResponse> {
        logger.error(
            "InvalidTokenException [${request.method} ${request.requestURI}]",
            exception,
        )
        val body = GlobalErrorCodeResponse(errorCode = exception.errorCode)
        return ResponseEntity(body, exception.status)
    }

    @ExceptionHandler(AccountDisabledException::class)
    fun handleAccountDisabled(
        exception: AccountDisabledException,
        request: HttpServletRequest,
    ): ResponseEntity<GlobalErrorCodeResponse> {
        logger.error(
            "AccountDisabledException [${request.method} ${request.requestURI}]",
            exception,
        )
        val body = GlobalErrorCodeResponse(errorCode = exception.errorCode)
        return ResponseEntity(body, exception.status)
    }

    @ExceptionHandler(PasswordValidationException::class)
    fun handlePasswordValidation(
        exception: PasswordValidationException,
        request: HttpServletRequest,
    ): ResponseEntity<GlobalErrorCodeResponse> {
        logger.error(
            "PasswordValidationException [${request.method} ${request.requestURI}] " +
                "errorCode=${exception.errorCode}",
            exception,
        )
        val body = GlobalErrorCodeResponse(errorCode = exception.errorCode, fieldErrors = exception.fieldErrors)
        return ResponseEntity(body, exception.status)
    }

    @ExceptionHandler(DuplicateEntryException::class)
    fun handleDuplicateEntry(
        exception: DuplicateEntryException,
        request: HttpServletRequest,
    ): ResponseEntity<GlobalErrorCodeResponse> {
        logger.error(
            "DuplicateEntryException [${request.method} ${request.requestURI}] " +
                "resource=${exception.resource}, field=${exception.field}, value=${exception.value}, errorCode=${exception.errorCode}",
            exception,
        )
        val body = GlobalErrorCodeResponse(errorCode = exception.errorCode)
        return ResponseEntity(body, exception.status)
    }

    @ExceptionHandler(EntryNotFoundException::class)
    fun handleEntryNotFound(
        exception: EntryNotFoundException,
        request: HttpServletRequest,
    ): ResponseEntity<GlobalErrorCodeResponse> {
        logger.error(
            "EntryNotFoundException [${request.method} ${request.requestURI}] " +
                "resource=${exception.resource}, identifier=${exception.identifier}, errorCode=${exception.errorCode}",
            exception,
        )
        val body = GlobalErrorCodeResponse(errorCode = exception.errorCode)
        return ResponseEntity(body, exception.status)
    }

    @ExceptionHandler(AccessDeniedException::class)
    fun handleAccessDenied(
        exception: AccessDeniedException,
        request: HttpServletRequest,
    ): ResponseEntity<GlobalErrorCodeResponse> {
        logger.error(
            "AccessDeniedException [${request.method} ${request.requestURI}]",
            exception,
        )
        val body = GlobalErrorCodeResponse(errorCode = GlobalErrorCode.FORBIDDEN)
        return ResponseEntity(body, HttpStatus.FORBIDDEN)
    }

    override fun handleMethodArgumentNotValid(
        ex: MethodArgumentNotValidException,
        headers: HttpHeaders,
        status: HttpStatusCode,
        request: WebRequest,
    ): ResponseEntity<Any> {
        logger.error("Validation failed", ex)
        val fieldErrors =
            ex.bindingResult.fieldErrors.map { error ->
                FieldErrorResponse(
                    field = error.field,
                    message = error.defaultMessage ?: "Invalid value",
                )
            }
        val body =
            GlobalErrorCodeResponse(
                errorCode = GlobalErrorCode.VALIDATION_ERROR,
                fieldErrors = fieldErrors,
            )
        return ResponseEntity(body, HttpStatus.BAD_REQUEST)
    }

    override fun handleHttpMessageNotReadable(
        ex: HttpMessageNotReadableException,
        headers: HttpHeaders,
        statusCode: HttpStatusCode,
        request: WebRequest,
    ): ResponseEntity<Any> {
        logger.error("Malformed request body", ex)
        val body = GlobalErrorCodeResponse(errorCode = GlobalErrorCode.MALFORMED_REQUEST)
        return ResponseEntity(body, HttpStatus.BAD_REQUEST)
    }

    @ExceptionHandler(Exception::class)
    fun handleGeneral(
        exception: Exception,
        request: HttpServletRequest,
    ): ResponseEntity<GlobalErrorCodeResponse> {
        logger.error(
            "Unhandled exception [${request.method} ${request.requestURI}]",
            exception,
        )
        val body = GlobalErrorCodeResponse(errorCode = GlobalErrorCode.INTERNAL_SERVER_ERROR)
        return ResponseEntity(body, HttpStatus.INTERNAL_SERVER_ERROR)
    }
}
