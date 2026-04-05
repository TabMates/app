package de.tabmates.core.domain.auth

import kotlinx.serialization.Serializable

@Serializable
data class AuthInfo(
    val accessToken: String,
    val refreshToken: String,
    val user: User,
)
