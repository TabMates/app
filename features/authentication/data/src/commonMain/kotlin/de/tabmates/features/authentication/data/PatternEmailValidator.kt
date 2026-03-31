package de.tabmates.features.authentication.data

import de.tabmates.features.authentication.domain.EmailValidator

private const val EMAIL_PATTERN = "^[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\\.[a-zA-Z]{2,}$"

class PatternEmailValidator : EmailValidator {
    override fun validate(email: String): Boolean {
        return EMAIL_PATTERN.toRegex().matches(email)
    }
}
