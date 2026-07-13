package de.tabmates.features.authentication.data.dto.requests

import kotlinx.serialization.Serializable

@Serializable
data class ChangeEmailRequest(
    val newEmail: String,
    val password: String,
)
