package de.tabmates.features.notifications.data

import de.tabmates.features.notifications.domain.PushNotificationController

/** No-op controller for platforms without push support (Desktop, Web). */
class NoOpPushNotificationController : PushNotificationController {
    override fun start() {}

    override suspend fun refreshRegistration() {}

    override suspend fun stop() {}
}
