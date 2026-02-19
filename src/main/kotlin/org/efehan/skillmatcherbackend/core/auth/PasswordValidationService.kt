package org.efehan.skillmatcherbackend.core.auth

import org.efehan.skillmatcherbackend.exception.FieldErrorResponse
import org.efehan.skillmatcherbackend.exception.GlobalErrorCode
import org.efehan.skillmatcherbackend.shared.exceptions.PasswordValidationException
import org.springframework.http.HttpStatus
import org.springframework.stereotype.Service

@Service
class PasswordValidationService {
    companion object {
        const val MIN_LENGTH = 8
        const val MAX_LENGTH = 72 // BCrypt truncates after 72 bytes
    }

    fun validate(password: String): List<String> {
        val errors = mutableListOf<String>()

        if (password.length < MIN_LENGTH) {
            errors += "Password must be at least $MIN_LENGTH characters long"
        }

        if (password.length > MAX_LENGTH) {
            errors += "Password must not exceed $MAX_LENGTH characters"
        }

        if (!password.any { it.isUpperCase() }) {
            errors += "Password must contain at least one uppercase letter"
        }

        if (!password.any { it.isLowerCase() }) {
            errors += "Password must contain at least one lowercase letter"
        }

        if (!password.any { it.isDigit() }) {
            errors += "Password must contain at least one digit"
        }

        if (password.all { it.isLetterOrDigit() }) {
            errors += "Password must contain at least one special character"
        }

        if (password.isNotEmpty() && password.trim().length < password.length) {
            errors += "Password must not start or end with whitespace"
        }

        return errors
    }

    fun isValid(password: String): Boolean = validate(password).isEmpty()

    fun validateOrThrow(password: String) {
        val errors = validate(password)
        if (errors.isNotEmpty()) {
            throw PasswordValidationException(
                errorCode = GlobalErrorCode.VALIDATION_ERROR,
                status = HttpStatus.BAD_REQUEST,
                fieldErrors = errors.map { FieldErrorResponse(field = "password", message = it) },
                message = "Password does not meet the required complexity.",
            )
        }
    }
}
