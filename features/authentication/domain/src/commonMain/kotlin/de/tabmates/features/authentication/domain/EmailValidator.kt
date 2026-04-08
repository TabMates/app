package de.tabmates.features.authentication.domain

object EmailValidator {
    private val emailRegex = Regex("^[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\\.[a-zA-Z]{2,6}$")

    fun validate(email: String): Boolean {
        return email.matches(emailRegex)
    }
}
