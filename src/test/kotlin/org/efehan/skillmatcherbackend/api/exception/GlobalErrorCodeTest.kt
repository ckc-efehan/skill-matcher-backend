package org.efehan.skillmatcherbackend.api.exception

import org.assertj.core.api.Assertions.assertThat
import org.efehan.skillmatcherbackend.exception.GlobalErrorCode
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test

@DisplayName("Global Error Code Unit Tests")
class GlobalErrorCodeTest {
    // Authentication
    val expectedBadCredentialsErrorCode = "BAD_CREDENTIALS"
    val expectedForbiddenErrorCode = "FORBIDDEN"
    val expectedUserMustLoginErrorCode = "USER_MUST_LOGIN"

    // Validation
    val expectedValidationErrorCode = "VALIDATION_ERROR"
    val expectedMalformedRequestErrorCode = "MALFORMED_REQUEST"
    val expectedInvalidPasswordErrorCode = "INVALID_PASSWORD"
    val expectedInvalidRefreshTokenErrorCode = "INVALID_REFRESH_TOKEN"

    // Duplicate entries
    val expectedDuplicateEntryErrorCode = "DUPLICATE_ENTRY"
    val expectedUserAlreadyExistsErrorCode = "USER_ALREADY_EXISTS"

    // Not Found
    val expectedNotFoundErrorCode = "NOT_FOUND"
    val expectedRoleNotFoundErrorCode = "ROLE_NOT_FOUND"
    val expectedUserNotFoundErrorCode = "USER_NOT_FOUND"
    val expectedRefreshTokenNotFoundErrorCode = "REFRESH_TOKEN_NOT_FOUND"

    // User state
    val expectedUserInvalidOperationErrorCode = "USER_INVALID_OPERATION"
    val expectedAccountDisabledErrorCode = "ACCOUNT_DISABLED"

    // General
    val expectedInternalServerErrorCode = "INTERNAL_SERVER_ERROR"

    @Test
    fun `should not change global error codes otherwise api contract breaks`() {
        // Authentication
        assertThat(expectedBadCredentialsErrorCode).isEqualTo(GlobalErrorCode.BAD_CREDENTIALS.name)
        assertThat(expectedForbiddenErrorCode).isEqualTo(GlobalErrorCode.FORBIDDEN.name)
        assertThat(expectedUserMustLoginErrorCode).isEqualTo(GlobalErrorCode.USER_MUST_LOGIN.name)

        // Validation
        assertThat(expectedValidationErrorCode).isEqualTo(GlobalErrorCode.VALIDATION_ERROR.name)
        assertThat(expectedMalformedRequestErrorCode).isEqualTo(GlobalErrorCode.MALFORMED_REQUEST.name)
        assertThat(expectedInvalidPasswordErrorCode).isEqualTo(GlobalErrorCode.INVALID_PASSWORD.name)
        assertThat(expectedInvalidRefreshTokenErrorCode).isEqualTo(GlobalErrorCode.INVALID_REFRESH_TOKEN.name)

        // Duplicate entries
        assertThat(expectedDuplicateEntryErrorCode).isEqualTo(GlobalErrorCode.DUPLICATE_ENTRY.name)
        assertThat(expectedUserAlreadyExistsErrorCode).isEqualTo(GlobalErrorCode.USER_ALREADY_EXISTS.name)

        // Not Found
        assertThat(expectedNotFoundErrorCode).isEqualTo(GlobalErrorCode.NOT_FOUND.name)
        assertThat(expectedRoleNotFoundErrorCode).isEqualTo(GlobalErrorCode.ROLE_NOT_FOUND.name)
        assertThat(expectedUserNotFoundErrorCode).isEqualTo(GlobalErrorCode.USER_NOT_FOUND.name)
        assertThat(expectedRefreshTokenNotFoundErrorCode).isEqualTo(GlobalErrorCode.REFRESH_TOKEN_NOT_FOUND.name)

        // User state
        assertThat(expectedUserInvalidOperationErrorCode).isEqualTo(GlobalErrorCode.USER_INVALID_OPERATION.name)
        assertThat(expectedAccountDisabledErrorCode).isEqualTo(GlobalErrorCode.ACCOUNT_DISABLED.name)

        // General
        assertThat(expectedInternalServerErrorCode).isEqualTo(GlobalErrorCode.INTERNAL_SERVER_ERROR.name)

        assertThat(GlobalErrorCode.entries.size).isEqualTo(16)
    }

    @Test
    fun `all error codes should have non-blank descriptions`() {
        GlobalErrorCode.entries.forEach { code ->
            assertThat(code.description).isNotBlank()
        }
    }

    @Test
    fun `all error codes should have descriptions ending with period`() {
        GlobalErrorCode.entries.forEach { code ->
            assertThat(code.description).endsWith(".")
        }
    }
}
