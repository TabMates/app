package de.tabmates.androidapp

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import androidx.core.content.getSystemService

/**
 * Android notification categories (channels). Created on app launch so the system can route
 * **background** FCM notification messages by `android.notification.channel_id` — the backend
 * must send one of these ids. [GENERAL] is the manifest default
 * (`com.google.firebase.messaging.default_notification_channel_id`) used when no id is sent.
 *
 * Note: foreground notifications are shown by kmpnotifier on its own channel and don't honor
 * these categories (kmpnotifier exposes no channel config). minSdk is 26, so channels always exist.
 */
object NotificationChannels {
    const val GENERAL = "general"
    const val EXPENSES = "expenses"
    const val MEMBERS = "members"
    const val SETTLE_UPS = "settle_ups"

    fun register(context: Context) {
        val manager = context.getSystemService<NotificationManager>() ?: return
        val channels =
            listOf(
                NotificationChannel(
                    GENERAL,
                    context.getString(R.string.notification_channel_general),
                    NotificationManager.IMPORTANCE_DEFAULT,
                ),
                NotificationChannel(
                    EXPENSES,
                    context.getString(R.string.notification_channel_expenses),
                    NotificationManager.IMPORTANCE_DEFAULT,
                ),
                NotificationChannel(
                    MEMBERS,
                    context.getString(R.string.notification_channel_members),
                    NotificationManager.IMPORTANCE_DEFAULT,
                ),
                NotificationChannel(
                    SETTLE_UPS,
                    context.getString(R.string.notification_channel_settle_ups),
                    NotificationManager.IMPORTANCE_DEFAULT,
                ),
            )
        manager.createNotificationChannels(channels)
    }
}
