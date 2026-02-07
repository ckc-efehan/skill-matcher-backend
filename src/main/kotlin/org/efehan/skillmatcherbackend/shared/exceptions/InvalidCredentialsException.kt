package org.efehan.skillmatcherbackend.shared.exceptions

import org.efehan.skillmatcherbackend.exception.GlobalErrorCode
import org.springframework.http.HttpStatus

class InvalidCredentialsException(
    val errorCode: GlobalErrorCode = GlobalErrorCode.BAD_CREDENTIALS,
    val status: HttpStatus = HttpStatus.UNAUTHORIZED,
) : RuntimeException("Bad credentials")
