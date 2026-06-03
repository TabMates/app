package de.tabmates.features.notifications.data.dto.requests

import kotlinx.serialization.Serializable

@Serializable
data class UnregisterDeviceRequest(
    val token: String,
)
