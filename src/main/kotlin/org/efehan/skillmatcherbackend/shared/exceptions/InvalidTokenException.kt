package org.efehan.skillmatcherbackend.shared.exceptions

import org.efehan.skillmatcherbackend.exception.GlobalErrorCode
import org.springframework.http.HttpStatus

data class InvalidTokenException(
    override val message: String,
    override val cause: Throwable? = null,
    val errorCode: GlobalErrorCode = GlobalErrorCode.INVALID_REFRESH_TOKEN,
    val status: HttpStatus = HttpStatus.UNAUTHORIZED,
) : RuntimeException(message, cause)
