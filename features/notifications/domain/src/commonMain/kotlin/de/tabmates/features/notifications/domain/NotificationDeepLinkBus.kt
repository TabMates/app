package de.tabmates.features.notifications.domain

import kotlinx.coroutines.flow.Flow

/**
 * One-way channel for deep links extracted from clicked notifications. The platform
 * [PushNotificationController] **publishes**; the app layer **collects** and forwards to its
 * navigation/deep-link handler. This inverts the dependency (data → app) so the data layer
 * never depends on app-layer navigation, and keeps it injectable/testable.
 */
interface NotificationDeepLinkBus {
    val deepLinks: Flow<String>

    fun publish(uri: String)
}
