package de.tabmates.core.domain.auth

import kotlinx.serialization.Serializable

enum class UserType {
    REGISTERED,
    ANONYMOUS,
}

@Serializable
data class User(
    val id: String,
    val email: String,
    val username: String,
    val hasVerifiedEmail: Boolean,
    val userType: UserType,
)
