package de.tabmates.features.notifications.domain

/** Keys the backend includes in a push notification's data payload. */
object PushNotificationConstants {
    /**
     * Optional deep-link URL in the payload. When a notification carrying this key is
     * clicked, [PushNotificationRouter] forwards it to the app's deep-link handler.
     */
    const val KEY_DEEP_LINK = "deepLink"

    /** Group a notification refers to (new expense, member joined, settle-up). */
    const val KEY_GROUP_ID = "groupId"
}
