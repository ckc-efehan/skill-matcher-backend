package org.efehan.skillmatcherbackend.shared.exceptions

import org.efehan.skillmatcherbackend.exception.GlobalErrorCode
import org.springframework.http.HttpStatus

data class AccessDeniedException(
    val resource: String,
    val errorCode: GlobalErrorCode,
    val status: HttpStatus,
    override val message: String = "Not allowed to modify $resource.",
) : RuntimeException(message)
