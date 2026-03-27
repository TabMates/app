package de.tabmates.features.authentication.data.dto

import kotlinx.serialization.Serializable

enum class UserTypeSerializable {
    REGISTERED,
    ANONYMOUS,
}

@Serializable
data class UserSerializable(
    val id: String,
    val email: String,
    val username: String,
    val hasVerifiedEmail: Boolean,
    val userType: UserTypeSerializable,
)
