package de.tabmates.features.notifications.data.dto.requests

import kotlinx.serialization.Serializable

@Serializable
data class RegisterDeviceRequest(
    val token: String,
    val platform: String,
    // BCP-47 tag (e.g. "en", "de") telling the backend which language to localize pushes in.
    val locale: String,
)
