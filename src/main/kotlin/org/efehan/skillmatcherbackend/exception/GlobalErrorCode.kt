package org.efehan.skillmatcherbackend.exception

enum class GlobalErrorCode(
    val description: String,
) {
    // Authentication
    BAD_CREDENTIALS("Bad credentials."),
    FORBIDDEN("Forbidden."),
    USER_MUST_LOGIN("User must be logged in."),

    // Validation
    VALIDATION_ERROR("Request validation failed."),
    MALFORMED_REQUEST("Malformed or unreadable request body."),
    INVALID_PASSWORD("Invalid password."),
    INVALID_REFRESH_TOKEN("Invalid refresh token."),

    // Duplicate entries
    DUPLICATE_ENTRY("A resource with the same unique value already exists."),
    USER_ALREADY_EXISTS("User already exists."),

    // Not Found
    NOT_FOUND("A resource with the value could not be found."),
    ROLE_NOT_FOUND("Role could not be found."),
    USER_NOT_FOUND("User could not be found."),
    REFRESH_TOKEN_NOT_FOUND("Refresh token could not be found."),

    // User state
    USER_INVALID_OPERATION("Current user state does not allow this operation."),
    ACCOUNT_DISABLED("Account is disabled."),

    // General
    INTERNAL_SERVER_ERROR("An unexpected error occurred."),
}
