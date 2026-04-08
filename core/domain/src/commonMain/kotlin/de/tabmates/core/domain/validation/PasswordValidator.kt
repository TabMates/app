package de.tabmates.core.domain.validation

object PasswordValidator {
    private val passwordRegex = Regex("^(?=.*[\\d!@#$%^&*()_+\\-=\\[\\]{};':\"\\\\|,.<>/?])(.{8,40})$")

    fun validate(password: String): Boolean {
        return password.matches(passwordRegex)
    }
}
