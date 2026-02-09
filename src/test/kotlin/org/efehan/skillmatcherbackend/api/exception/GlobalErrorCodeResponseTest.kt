package org.efehan.skillmatcherbackend.api.exception

import org.assertj.core.api.Assertions.assertThat
import org.efehan.skillmatcherbackend.exception.FieldErrorResponse
import org.efehan.skillmatcherbackend.exception.GlobalErrorCode
import org.efehan.skillmatcherbackend.exception.GlobalErrorCodeResponse
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test

@DisplayName("GlobalErrorCodeResponse Unit Tests")
class GlobalErrorCodeResponseTest {
    @Test
    fun `should use error code description as default error message`() {
        val response = GlobalErrorCodeResponse(errorCode = GlobalErrorCode.BAD_CREDENTIALS)

        assertThat(response.errorMessage).isEqualTo(GlobalErrorCode.BAD_CREDENTIALS.description)
        assertThat(response.fieldErrors).isEmpty()
    }

    @Test
    fun `should include field errors when provided`() {
        val fieldErrors =
            listOf(
                FieldErrorResponse(field = "email", message = "must not be blank"),
                FieldErrorResponse(field = "password", message = "too short"),
            )
        val response =
            GlobalErrorCodeResponse(
                errorCode = GlobalErrorCode.VALIDATION_ERROR,
                fieldErrors = fieldErrors,
            )

        assertThat(response.fieldErrors).hasSize(2)
        assertThat(response.fieldErrors[0].field).isEqualTo("email")
        assertThat(response.fieldErrors[1].field).isEqualTo("password")
    }

    @Test
    fun `FieldErrorResponse should hold all fields`() {
        val fieldError =
            FieldErrorResponse(
                field = "name",
                code = "NotBlank",
                message = "must not be blank",
                params = mapOf("min" to 1),
            )

        assertThat(fieldError.field).isEqualTo("name")
        assertThat(fieldError.code).isEqualTo("NotBlank")
        assertThat(fieldError.message).isEqualTo("must not be blank")
        assertThat(fieldError.params).containsEntry("min", 1)
    }

    @Test
    fun `FieldErrorResponse defaults should be null`() {
        val fieldError = FieldErrorResponse(field = "name", message = "invalid")

        assertThat(fieldError.code).isNull()
        assertThat(fieldError.params).isNull()
    }
}
