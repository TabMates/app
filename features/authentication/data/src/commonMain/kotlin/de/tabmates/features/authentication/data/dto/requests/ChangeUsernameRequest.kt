package de.tabmates.features.authentication.data.dto.requests

import kotlinx.serialization.Serializable

@Serializable
data class ChangeUsernameRequest(
    val newUsername: String,
)
