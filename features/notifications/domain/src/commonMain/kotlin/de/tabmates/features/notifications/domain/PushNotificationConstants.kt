package de.tabmates.features.notifications.domain

/** Keys the backend includes in a push notification's data payload. */
object PushNotificationConstants {
    /**
     * Deep-link URL the notification should open on tap. Forwarded to the app via
     * [NotificationDeepLinkBus] and resolved to a destination. For a group notification
     * (new expense, member joined, settle-up) the backend sends the group URL, e.g.
     * `https://<host>/groups/<groupId>`, which opens the group detail screen.
     */
    const val KEY_DEEP_LINK = "deepLink"
}
