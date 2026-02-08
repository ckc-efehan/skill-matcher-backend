package org.efehan.skillmatcherbackend.shared.exceptions

import org.efehan.skillmatcherbackend.exception.GlobalErrorCode
import org.springframework.http.HttpStatus

data class AccountDisabledException(
    val errorCode: GlobalErrorCode = GlobalErrorCode.ACCOUNT_DISABLED,
    val status: HttpStatus = HttpStatus.FORBIDDEN,
    override val message: String = "Account is disabled",
) : RuntimeException(message)
