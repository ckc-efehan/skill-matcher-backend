package org.efehan.skillmatcherbackend.shared.exceptions

import org.efehan.skillmatcherbackend.exception.FieldErrorResponse
import org.efehan.skillmatcherbackend.exception.GlobalErrorCode
import org.springframework.http.HttpStatus

data class PasswordValidationException(
    val errorCode: GlobalErrorCode,
    val status: HttpStatus = HttpStatus.BAD_REQUEST,
    val fieldErrors: List<FieldErrorResponse> = emptyList(),
    override val message: String,
) : RuntimeException(message)
