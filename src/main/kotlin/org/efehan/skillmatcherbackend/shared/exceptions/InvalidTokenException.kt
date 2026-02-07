package org.efehan.skillmatcherbackend.shared.exceptions

import org.efehan.skillmatcherbackend.exception.GlobalErrorCode
import org.springframework.http.HttpStatus

class InvalidTokenException(
    message: String,
    cause: Throwable? = null,
    val errorCode: GlobalErrorCode = GlobalErrorCode.INVALID_REFRESH_TOKEN,
    val status: HttpStatus = HttpStatus.UNAUTHORIZED,
) : RuntimeException(message, cause)
