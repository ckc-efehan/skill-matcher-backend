package org.efehan.skillmatcherbackend.core.auth

import org.springframework.stereotype.Service

@Service
class PasswordValidationService {
    companion object {
        const val MIN_LENGTH = 8
        const val MAX_LENGTH = 72 // BCrypt truncates after 72 bytes
    }

    private val commonPasswords: Set<String> by lazy {
        javaClass
            .getResourceAsStream("/common-passwords.txt")
            ?.bufferedReader()
            ?.lineSequence()
            ?.map { it.trim().lowercase() }
            ?.filter { it.isNotBlank() }
            ?.toSet()
            ?: emptySet()
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

        if (password.lowercase() in commonPasswords) {
            errors += "Password is too common and easily guessable"
        }

        if (password.isNotEmpty() && password.trim().length < password.length) {
            errors += "Password must not start or end with whitespace"
        }

        return errors
    }

    fun isValid(password: String): Boolean = validate(password).isEmpty()
}
