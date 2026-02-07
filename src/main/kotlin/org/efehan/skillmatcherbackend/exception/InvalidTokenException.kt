package org.efehan.skillmatcherbackend.exception

class InvalidTokenException(
    message: String,
    cause: Throwable? = null,
) : RuntimeException(message, cause)
