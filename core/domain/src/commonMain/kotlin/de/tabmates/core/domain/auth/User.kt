package de.tabmates.core.domain.auth

enum class UserType {
    REGISTERED,
    ANONYMOUS,
}

data class User(
    val id: String,
    val email: String,
    val username: String,
    val hasVerifiedEmail: Boolean,
    val userType: UserType,
)
