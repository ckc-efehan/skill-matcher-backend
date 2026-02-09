package org.efehan.skillmatcherbackend.shared.exceptions

import org.efehan.skillmatcherbackend.exception.GlobalErrorCode
import org.springframework.http.HttpStatus

data class InvalidCredentialsException(
    val errorCode: GlobalErrorCode = GlobalErrorCode.BAD_CREDENTIALS,
    val status: HttpStatus = HttpStatus.UNAUTHORIZED,
    override val message: String = "Bad credentials",
) : RuntimeException(message)
