package de.tabmates.features.notifications.domain

import kotlinx.coroutines.flow.StateFlow

/** OS-level permission to display notifications. */
enum class NotificationPermissionStatus {
    /** Allowed to show notifications. */
    GRANTED,

    /** Explicitly denied / disabled — only the OS settings can re-enable it. */
    DENIED,

    /** Not yet asked (can still be requested in-app). */
    NOT_DETERMINED,

    /** Platform has no notification-permission concept (treated as allowed). */
    UNSUPPORTED,
}

/**
 * Reads the OS notification-permission status and opens the OS settings to change it.
 * Platform-specific; injected so the presentation layer can disable the toggle and show a
 * rationale banner when notifications aren't permitted.
 */
interface NotificationPermissionController {
    val status: StateFlow<NotificationPermissionStatus>

    /** Re-read the OS permission state (call when the screen resumes). */
    suspend fun refresh()

    /** Open this app's OS notification settings, where supported. */
    fun openSettings()
}
