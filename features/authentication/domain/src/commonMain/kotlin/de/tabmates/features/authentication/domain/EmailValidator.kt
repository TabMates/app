package de.tabmates.features.authentication.domain

interface EmailValidator {
    fun validate(email: String): Boolean
}
