package org.efehan.skillmatcherbackend.shared.exceptions

import org.efehan.skillmatcherbackend.exception.FieldErrorResponse
import org.efehan.skillmatcherbackend.exception.GlobalErrorCode
import org.springframework.http.HttpStatus

class PasswordValidationException(
    val errors: List<String>,
    val errorCode: GlobalErrorCode = GlobalErrorCode.INVALID_PASSWORD,
    val status: HttpStatus = HttpStatus.BAD_REQUEST,
) : RuntimeException("Password validation failed: ${errors.joinToString("; ")}") {
    val fieldErrors: List<FieldErrorResponse>
        get() = errors.map { FieldErrorResponse(field = "password", message = it) }
}
