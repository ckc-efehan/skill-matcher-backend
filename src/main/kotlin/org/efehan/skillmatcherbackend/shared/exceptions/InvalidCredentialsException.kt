package org.efehan.skillmatcherbackend.shared.exceptions

import org.efehan.skillmatcherbackend.exception.GlobalErrorCode
import org.springframework.http.HttpStatus

data class InvalidCredentialsException(
    val errorCode: GlobalErrorCode,
    val status: HttpStatus,
    override val message: String = "Bad credentials",
) : RuntimeException(message)
