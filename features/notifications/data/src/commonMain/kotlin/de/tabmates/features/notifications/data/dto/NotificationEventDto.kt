package de.tabmates.features.notifications.data.dto

import kotlinx.serialization.Serializable

/**
 * A notification pushed over the backend WebSocket stream (`/api/notifications/stream`).
 * Used by platforms without FCM (Desktop) to render a local notification.
 */
@Serializable
data class NotificationEventDto(
    val title: String,
    val body: String,
    val deepLink: String? = null,
)
