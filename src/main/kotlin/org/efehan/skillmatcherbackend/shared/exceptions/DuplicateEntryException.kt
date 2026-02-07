package org.efehan.skillmatcherbackend.shared.exceptions

import org.efehan.skillmatcherbackend.exception.GlobalErrorCode
import org.springframework.http.HttpStatus

class DuplicateEntryException(
    val resource: String,
    val field: String,
    val value: String,
    val errorCode: GlobalErrorCode = GlobalErrorCode.DUPLICATE_ENTRY,
    val status: HttpStatus = HttpStatus.CONFLICT,
) : RuntimeException("$resource with $field '$value' already exists")
