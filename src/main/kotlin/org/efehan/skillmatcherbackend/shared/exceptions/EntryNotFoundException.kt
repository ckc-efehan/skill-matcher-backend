package org.efehan.skillmatcherbackend.shared.exceptions

import org.efehan.skillmatcherbackend.exception.GlobalErrorCode
import org.springframework.http.HttpStatus
import java.lang.IllegalArgumentException

data class EntryNotFoundException(
    val resource: String,
    val field: String,
    val value: String,
    val errorCode: GlobalErrorCode,
    val status: HttpStatus,
    override val message: String = "$resource with $field '$value' could not be found.",
) : IllegalArgumentException(message)
