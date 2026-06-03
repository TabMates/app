package de.tabmates.features.notifications.data

import de.tabmates.features.notifications.domain.NotificationPermissionController
import de.tabmates.features.notifications.domain.NotificationPermissionStatus
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * For platforms without a runtime notification permission to gate (iOS/Desktop/Web here):
 * always [NotificationPermissionStatus.UNSUPPORTED], so the toggle stays enabled and no banner
 * shows. Only Android needs the real POST_NOTIFICATIONS check.
 */
class UnsupportedNotificationPermissionController : NotificationPermissionController {
    private val _status = MutableStateFlow(NotificationPermissionStatus.UNSUPPORTED)
    override val status: StateFlow<NotificationPermissionStatus> = _status.asStateFlow()

    override suspend fun refresh() = Unit

    override fun openSettings() = Unit
}
