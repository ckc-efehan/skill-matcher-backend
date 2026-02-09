package org.efehan.skillmatcherbackend.api.exception

import jakarta.servlet.http.HttpServletRequest
import org.assertj.core.api.Assertions.assertThat
import org.efehan.skillmatcherbackend.exception.FieldErrorResponse
import org.efehan.skillmatcherbackend.exception.GlobalErrorCode
import org.efehan.skillmatcherbackend.exception.GlobalExceptionHandler
import org.efehan.skillmatcherbackend.shared.exceptions.AccountDisabledException
import org.efehan.skillmatcherbackend.shared.exceptions.DuplicateEntryException
import org.efehan.skillmatcherbackend.shared.exceptions.EntryNotFoundException
import org.efehan.skillmatcherbackend.shared.exceptions.InvalidCredentialsException
import org.efehan.skillmatcherbackend.shared.exceptions.InvalidTokenException
import org.efehan.skillmatcherbackend.shared.exceptions.PasswordValidationException
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.mockito.Mockito.mock
import org.mockito.Mockito.`when`
import org.springframework.http.HttpStatus
import org.springframework.security.access.AccessDeniedException
import org.springframework.security.authentication.BadCredentialsException

@DisplayName("GlobalExceptionHandler Unit Tests")
class GlobalExceptionHandlerTest {
    private lateinit var handler: GlobalExceptionHandler
    private lateinit var request: HttpServletRequest

    @BeforeEach
    fun setUp() {
        handler = GlobalExceptionHandler()
        request = mock(HttpServletRequest::class.java)
        `when`(request.method).thenReturn("POST")
        `when`(request.requestURI).thenReturn("/api/test")
    }

    @Test
    fun `handleInvalidCredentials should return UNAUTHORIZED with BAD_CREDENTIALS error code`() {
        val exception = InvalidCredentialsException()

        val response = handler.handleInvalidCredentials(exception, request)

        assertThat(response.statusCode).isEqualTo(HttpStatus.UNAUTHORIZED)
        assertThat(response.body?.errorCode).isEqualTo(GlobalErrorCode.BAD_CREDENTIALS)
    }

    @Test
    fun `handleInvalidToken should return UNAUTHORIZED with INVALID_REFRESH_TOKEN error code`() {
        val exception = InvalidTokenException(message = "Token expired")

        val response = handler.handleInvalidToken(exception, request)

        assertThat(response.statusCode).isEqualTo(HttpStatus.UNAUTHORIZED)
        assertThat(response.body?.errorCode).isEqualTo(GlobalErrorCode.INVALID_REFRESH_TOKEN)
    }

    @Test
    fun `handleAccountDisabled should return FORBIDDEN with ACCOUNT_DISABLED error code`() {
        val exception = AccountDisabledException()

        val response = handler.handleAccountDisabled(exception, request)

        assertThat(response.statusCode).isEqualTo(HttpStatus.FORBIDDEN)
        assertThat(response.body?.errorCode).isEqualTo(GlobalErrorCode.ACCOUNT_DISABLED)
    }

    @Test
    fun `handlePasswordValidation should return BAD_REQUEST with field errors`() {
        val fieldErrors = listOf(FieldErrorResponse(field = "password", message = "Too short"))
        val exception =
            PasswordValidationException(
                errorCode = GlobalErrorCode.INVALID_PASSWORD,
                fieldErrors = fieldErrors,
                message = "Password validation failed",
            )

        val response = handler.handlePasswordValidation(exception, request)

        assertThat(response.statusCode).isEqualTo(HttpStatus.BAD_REQUEST)
        assertThat(response.body?.errorCode).isEqualTo(GlobalErrorCode.INVALID_PASSWORD)
        assertThat(response.body?.fieldErrors).hasSize(1)
        assertThat(
            response.body
                ?.fieldErrors
                ?.first()
                ?.field,
        ).isEqualTo("password")
    }

    @Test
    fun `handleDuplicateEntry should return CONFLICT with DUPLICATE_ENTRY error code`() {
        val exception =
            DuplicateEntryException(
                resource = "User",
                field = "email",
                value = "test@test.com",
            )

        val response = handler.handleDuplicateEntry(exception, request)

        assertThat(response.statusCode).isEqualTo(HttpStatus.CONFLICT)
        assertThat(response.body?.errorCode).isEqualTo(GlobalErrorCode.DUPLICATE_ENTRY)
    }

    @Test
    fun `handleEntryNotFound should return NOT_FOUND with NOT_FOUND error code`() {
        val exception =
            EntryNotFoundException(
                resource = "User",
                field = "id",
                value = "123",
            )

        val response = handler.handleEntryNotFound(exception, request)

        assertThat(response.statusCode).isEqualTo(HttpStatus.NOT_FOUND)
        assertThat(response.body?.errorCode).isEqualTo(GlobalErrorCode.NOT_FOUND)
    }

    @Test
    fun `handleBadCredentials should return UNAUTHORIZED with BAD_CREDENTIALS error code`() {
        val exception = BadCredentialsException("Bad credentials")

        val response = handler.handleBadCredentials(exception, request)

        assertThat(response.statusCode).isEqualTo(HttpStatus.UNAUTHORIZED)
        assertThat(response.body?.errorCode).isEqualTo(GlobalErrorCode.BAD_CREDENTIALS)
    }

    @Test
    fun `handleAccessDenied should return FORBIDDEN with FORBIDDEN error code`() {
        val exception = AccessDeniedException("Access denied")

        val response = handler.handleAccessDenied(exception, request)

        assertThat(response.statusCode).isEqualTo(HttpStatus.FORBIDDEN)
        assertThat(response.body?.errorCode).isEqualTo(GlobalErrorCode.FORBIDDEN)
    }

    @Test
    fun `handleGeneral should return INTERNAL_SERVER_ERROR`() {
        val exception = RuntimeException("Something went wrong")

        val response = handler.handleGeneral(exception, request)

        assertThat(response.statusCode).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR)
        assertThat(response.body?.errorCode).isEqualTo(GlobalErrorCode.INTERNAL_SERVER_ERROR)
    }
}
