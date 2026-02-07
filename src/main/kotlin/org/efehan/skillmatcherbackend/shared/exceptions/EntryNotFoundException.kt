package org.efehan.skillmatcherbackend.shared.exceptions

import org.efehan.skillmatcherbackend.exception.GlobalErrorCode
import org.springframework.http.HttpStatus

class EntryNotFoundException(
    val resource: String,
    val identifier: String,
    val errorCode: GlobalErrorCode = GlobalErrorCode.NOT_FOUND,
    val status: HttpStatus = HttpStatus.NOT_FOUND,
) : RuntimeException("$resource with identifier '$identifier' not found")
