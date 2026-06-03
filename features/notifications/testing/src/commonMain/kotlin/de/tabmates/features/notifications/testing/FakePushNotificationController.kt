package de.tabmates.features.notifications.testing

import de.tabmates.features.notifications.domain.PushNotificationController

/** Records lifecycle calls for verifying coordinators that drive push registration. */
open class FakePushNotificationController : PushNotificationController {
    var startCalls: Int = 0
        private set
    var refreshCalls: Int = 0
        private set
    var stopCalls: Int = 0
        private set

    override fun start() {
        startCalls++
    }

    override suspend fun refreshRegistration() {
        refreshCalls++
    }

    override suspend fun stop() {
        stopCalls++
    }
}
