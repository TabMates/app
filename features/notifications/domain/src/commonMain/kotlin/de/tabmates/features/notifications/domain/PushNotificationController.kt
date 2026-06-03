package de.tabmates.features.notifications.domain

/**
 * Platform-specific entry point for push notifications. Implementations initialize
 * the underlying messaging SDK (Firebase Cloud Messaging via kmpnotifier on mobile),
 * obtain the device token, register it with the backend via [NotificationService] and
 * forward token refreshes. Desktop/Web use a no-op implementation.
 */
interface PushNotificationController {
    /** Initialize messaging and register the current device token with the backend. */
    fun start()

    /** Re-send the current device token (e.g. after the in-app language changed). */
    suspend fun refreshRegistration()

    /** Unregister the current device token from the backend (call on logout). */
    suspend fun stop()
}
