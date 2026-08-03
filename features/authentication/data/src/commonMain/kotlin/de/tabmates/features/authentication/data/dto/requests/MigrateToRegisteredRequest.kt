package de.tabmates.features.authentication.data.dto.requests

import kotlinx.serialization.Serializable

@Serializable
data class MigrateToRegisteredRequest(
    val email: String,
    val password: String,
)
