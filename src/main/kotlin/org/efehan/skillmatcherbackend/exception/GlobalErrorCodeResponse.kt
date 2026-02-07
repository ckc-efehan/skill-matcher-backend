package org.efehan.skillmatcherbackend.exception

data class GlobalErrorCodeResponse(
    val errorCode: GlobalErrorCode,
    val errorMessage: String = errorCode.description,
    val fieldErrors: List<FieldErrorResponse> = emptyList(),
)

data class FieldErrorResponse(
    val field: String,
    val code: String? = null,
    val message: String,
    val params: Map<String, Any?>? = null,
)
