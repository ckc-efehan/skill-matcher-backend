package org.efehan.skillmatcherbackend.shared.exceptions

import org.efehan.skillmatcherbackend.exception.GlobalErrorCode
import org.springframework.http.HttpStatus

data class InvalidTokenException(
    override val message: String,
    override val cause: Throwable? = null,
    val errorCode: GlobalErrorCode,
    val status: HttpStatus,
) : RuntimeException(message, cause)
