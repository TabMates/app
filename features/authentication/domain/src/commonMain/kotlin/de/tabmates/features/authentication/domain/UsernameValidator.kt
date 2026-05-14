package de.tabmates.features.authentication.domain

object UsernameValidator {
    const val MIN_LENGTH = 3
    const val MAX_LENGTH = 20

    fun validate(username: String): Boolean = username.length in MIN_LENGTH..MAX_LENGTH
}
