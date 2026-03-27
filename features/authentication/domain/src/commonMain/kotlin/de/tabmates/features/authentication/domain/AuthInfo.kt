package de.tabmates.features.authentication.domain

data class AuthInfo(
    val accessToken: String,
    val refreshToken: String,
    val user: User,
)
