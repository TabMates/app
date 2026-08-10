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

/** What an avatar falls back to, matching `GroupParticipant.initials` for other people. */
val User.initials: String get() = username.take(2).uppercase()
