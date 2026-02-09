package org.efehan.skillmatcherbackend.core.auth

import jakarta.validation.Constraint
import jakarta.validation.ConstraintValidator
import jakarta.validation.ConstraintValidatorContext
import jakarta.validation.Payload
import kotlin.reflect.KClass

@Target(AnnotationTarget.FIELD, AnnotationTarget.VALUE_PARAMETER)
@Retention(AnnotationRetention.RUNTIME)
@Constraint(validatedBy = [PasswordConstraintValidator::class])
annotation class ValidPassword(
    val message: String = "Invalid password",
    val groups: Array<KClass<*>> = [],
    val payload: Array<KClass<out Payload>> = [],
)

class PasswordConstraintValidator(
    private val passwordValidationService: PasswordValidationService,
) : ConstraintValidator<ValidPassword, String> {
    override fun isValid(
        password: String?,
        context: ConstraintValidatorContext,
    ): Boolean {
        if (password == null) return false

        val errors = passwordValidationService.validate(password)
        if (errors.isEmpty()) return true

        context.disableDefaultConstraintViolation()
        errors.forEach { error ->
            context.buildConstraintViolationWithTemplate(error).addConstraintViolation()
        }
        return false
    }
}
