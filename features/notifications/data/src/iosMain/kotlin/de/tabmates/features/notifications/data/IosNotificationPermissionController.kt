package de.tabmates.features.notifications.data

import de.tabmates.features.notifications.domain.NotificationPermissionController
import de.tabmates.features.notifications.domain.NotificationPermissionStatus
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.suspendCancellableCoroutine
import platform.Foundation.NSURL
import platform.UIKit.UIApplication
import platform.UIKit.UIApplicationOpenSettingsURLString
import platform.UserNotifications.UNAuthorizationStatusAuthorized
import platform.UserNotifications.UNAuthorizationStatusDenied
import platform.UserNotifications.UNAuthorizationStatusEphemeral
import platform.UserNotifications.UNAuthorizationStatusProvisional
import platform.UserNotifications.UNUserNotificationCenter
import kotlin.coroutines.resume

/** Reads the iOS notification authorization status (required for FCM push on iOS). */
class IosNotificationPermissionController : NotificationPermissionController {
    private val _status = MutableStateFlow(NotificationPermissionStatus.NOT_DETERMINED)
    override val status: StateFlow<NotificationPermissionStatus> = _status.asStateFlow()

    override suspend fun refresh() {
        val center = UNUserNotificationCenter.currentNotificationCenter()
        _status.value =
            suspendCancellableCoroutine { continuation ->
                center.getNotificationSettingsWithCompletionHandler { settings ->
                    val mapped =
                        when (settings?.authorizationStatus) {
                            UNAuthorizationStatusAuthorized,
                            UNAuthorizationStatusProvisional,
                            UNAuthorizationStatusEphemeral,
                            -> NotificationPermissionStatus.GRANTED

                            UNAuthorizationStatusDenied -> NotificationPermissionStatus.DENIED

                            else -> NotificationPermissionStatus.NOT_DETERMINED
                        }
                    continuation.resume(mapped)
                }
            }
    }

    override fun openSettings() {
        val url = NSURL.URLWithString(UIApplicationOpenSettingsURLString) ?: return
        UIApplication.sharedApplication.openURL(url)
    }
}
