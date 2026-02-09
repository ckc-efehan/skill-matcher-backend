package org.efehan.skillmatcherbackend.api.exception

import org.assertj.core.api.Assertions.assertThat
import org.efehan.skillmatcherbackend.exception.GlobalErrorCode
import org.efehan.skillmatcherbackend.shared.exceptions.AccountDisabledException
import org.efehan.skillmatcherbackend.shared.exceptions.DuplicateEntryException
import org.efehan.skillmatcherbackend.shared.exceptions.EntryNotFoundException
import org.efehan.skillmatcherbackend.shared.exceptions.InvalidCredentialsException
import org.efehan.skillmatcherbackend.shared.exceptions.InvalidTokenException
import org.efehan.skillmatcherbackend.shared.exceptions.PasswordValidationException
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.springframework.http.HttpStatus

@DisplayName("Exception Models Unit Tests")
class ExceptionModelsTest {
    @Test
    fun `InvalidCredentialsException should have correct defaults`() {
        val ex = InvalidCredentialsException()

        assertThat(ex.errorCode).isEqualTo(GlobalErrorCode.BAD_CREDENTIALS)
        assertThat(ex.status).isEqualTo(HttpStatus.UNAUTHORIZED)
        assertThat(ex.message).isEqualTo("Bad credentials")
    }

    @Test
    fun `InvalidTokenException should hold message and cause`() {
        val cause = RuntimeException("root cause")
        val ex = InvalidTokenException(message = "Token expired", cause = cause)

        assertThat(ex.message).isEqualTo("Token expired")
        assertThat(ex.cause).isEqualTo(cause)
        assertThat(ex.errorCode).isEqualTo(GlobalErrorCode.INVALID_REFRESH_TOKEN)
        assertThat(ex.status).isEqualTo(HttpStatus.UNAUTHORIZED)
    }

    @Test
    fun `InvalidTokenException should have null cause by default`() {
        val ex = InvalidTokenException(message = "Token expired")

        assertThat(ex.cause).isNull()
    }

    @Test
    fun `AccountDisabledException should have correct defaults`() {
        val ex = AccountDisabledException()

        assertThat(ex.errorCode).isEqualTo(GlobalErrorCode.ACCOUNT_DISABLED)
        assertThat(ex.status).isEqualTo(HttpStatus.FORBIDDEN)
        assertThat(ex.message).isEqualTo("Account is disabled")
    }

    @Test
    fun `DuplicateEntryException should build default message from fields`() {
        val ex = DuplicateEntryException(resource = "User", field = "email", value = "a@b.com")

        assertThat(ex.message).isEqualTo("User with email 'a@b.com' already exists.")
        assertThat(ex.errorCode).isEqualTo(GlobalErrorCode.DUPLICATE_ENTRY)
        assertThat(ex.status).isEqualTo(HttpStatus.CONFLICT)
    }

    @Test
    fun `DuplicateEntryException should allow custom error code`() {
        val ex =
            DuplicateEntryException(
                resource = "User",
                field = "email",
                value = "a@b.com",
                errorCode = GlobalErrorCode.USER_ALREADY_EXISTS,
            )

        assertThat(ex.errorCode).isEqualTo(GlobalErrorCode.USER_ALREADY_EXISTS)
    }

    @Test
    fun `EntryNotFoundException should build default message from fields`() {
        val ex = EntryNotFoundException(resource = "User", field = "id", value = "123")

        assertThat(ex.message).isEqualTo("User with id '123' could not be found.")
        assertThat(ex.errorCode).isEqualTo(GlobalErrorCode.NOT_FOUND)
        assertThat(ex.status).isEqualTo(HttpStatus.NOT_FOUND)
    }

    @Test
    fun `EntryNotFoundException should allow custom error code`() {
        val ex =
            EntryNotFoundException(
                resource = "User",
                field = "id",
                value = "123",
                errorCode = GlobalErrorCode.USER_NOT_FOUND,
            )

        assertThat(ex.errorCode).isEqualTo(GlobalErrorCode.USER_NOT_FOUND)
    }

    @Test
    fun `PasswordValidationException should hold field errors`() {
        val ex =
            PasswordValidationException(
                errorCode = GlobalErrorCode.INVALID_PASSWORD,
                message = "Password validation failed",
            )

        assertThat(ex.errorCode).isEqualTo(GlobalErrorCode.INVALID_PASSWORD)
        assertThat(ex.status).isEqualTo(HttpStatus.BAD_REQUEST)
        assertThat(ex.fieldErrors).isEmpty()
    }
}
