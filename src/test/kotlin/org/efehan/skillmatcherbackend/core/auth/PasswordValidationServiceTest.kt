package org.efehan.skillmatcherbackend.core.auth

import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class PasswordValidationServiceTest {
    private lateinit var service: PasswordValidationService

    @BeforeEach
    fun setUp() {
        service = PasswordValidationService()
    }

    @Test
    fun `valid password returns no errors`() {
        assertTrue(service.isValid("MyStr0ngP@ss!"))
        assertTrue(service.validate("MyStr0ngP@ss!").isEmpty())
    }

    @Test
    fun `password shorter than minimum length returns error`() {
        val errors = service.validate("sH0r!")
        assertTrue(errors.any { it.contains("at least ${PasswordValidationService.MIN_LENGTH}") })
    }

    @Test
    fun `password exceeding max length returns error`() {
        val longPassword = "aA1!" + "x".repeat(PasswordValidationService.MAX_LENGTH)
        val errors = service.validate(longPassword)
        assertTrue(errors.any { it.contains("must not exceed") })
    }

    @Test
    fun `password exactly at minimum length is valid`() {
        val errors = service.validate("aB1!xyzw")
        assertTrue(errors.isEmpty())
    }

    @Test
    fun `password exactly at maximum length is valid`() {
        val password = "aB1!" + "x".repeat(PasswordValidationService.MAX_LENGTH - 4)
        val errors = service.validate(password)
        assertTrue(errors.isEmpty())
    }

    @Test
    fun `password without uppercase returns error`() {
        val errors = service.validate("lowercase1!")
        assertTrue(errors.any { it.contains("uppercase") })
    }

    @Test
    fun `password without lowercase returns error`() {
        val errors = service.validate("UPPERCASE1!")
        assertTrue(errors.any { it.contains("lowercase") })
    }

    @Test
    fun `password without digit returns error`() {
        val errors = service.validate("NoDigits!@")
        assertTrue(errors.any { it.contains("digit") })
    }

    @Test
    fun `password without special character returns error`() {
        val errors = service.validate("NoSpecial1x")
        assertTrue(errors.any { it.contains("special character") })
    }

    @Test
    fun `common password returns error`() {
        val errors = service.validate("password1")
        assertTrue(errors.any { it.contains("too common") })
    }

    @Test
    fun `common password check is case insensitive`() {
        val errors = service.validate("Password")
        assertTrue(errors.any { it.contains("too common") })
    }

    @Test
    fun `password with leading whitespace returns error`() {
        val errors = service.validate(" Valid1pass!")
        assertTrue(errors.any { it.contains("whitespace") })
    }

    @Test
    fun `password with trailing whitespace returns error`() {
        val errors = service.validate("Valid1pass! ")
        assertTrue(errors.any { it.contains("whitespace") })
    }

    @Test
    fun `password with whitespace in the middle is valid`() {
        val errors = service.validate("Valid1 pass phrase!")
        assertTrue(errors.isEmpty())
    }

    @Test
    fun `multiple validation errors are returned together`() {
        val errors = service.validate("  abc  ")
        assertTrue(errors.size >= 2)
    }
}
