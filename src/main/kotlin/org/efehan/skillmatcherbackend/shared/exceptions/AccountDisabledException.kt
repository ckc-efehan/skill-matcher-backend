package org.efehan.skillmatcherbackend.shared.exceptions

import org.efehan.skillmatcherbackend.exception.GlobalErrorCode
import org.springframework.http.HttpStatus

class AccountDisabledException(
    val errorCode: GlobalErrorCode = GlobalErrorCode.ACCOUNT_DISABLED,
    val status: HttpStatus = HttpStatus.FORBIDDEN,
) : RuntimeException("Account is disabled")
