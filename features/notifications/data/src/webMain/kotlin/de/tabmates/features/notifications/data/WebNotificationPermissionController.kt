@file:OptIn(kotlin.js.ExperimentalWasmJsInterop::class)

package de.tabmates.features.notifications.data

import de.tabmates.features.notifications.domain.NotificationPermissionController
import de.tabmates.features.notifications.domain.NotificationPermissionStatus
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/** Reads the browser Notification permission (which the user can deny in site settings). */
class WebNotificationPermissionController : NotificationPermissionController {
    private val _status = MutableStateFlow(readStatus())
    override val status: StateFlow<NotificationPermissionStatus> = _status.asStateFlow()

    override suspend fun refresh() {
        _status.value = readStatus()
    }

    // Browsers expose no programmatic per-app settings page; the rationale banner guides the user.
    override fun openSettings() = Unit

    private fun readStatus(): NotificationPermissionStatus =
        when (browserNotificationPermission().toString()) {
            "granted" -> NotificationPermissionStatus.GRANTED
            "denied" -> NotificationPermissionStatus.DENIED
            else -> NotificationPermissionStatus.NOT_DETERMINED // "default"
        }
}

private fun browserNotificationPermission(): JsString =
    js("(typeof Notification !== 'undefined' ? Notification.permission : 'default')")
