package de.tabmates.composeapp

import com.mmk.kmpnotifier.KMPNotifier
import com.mmk.kmpnotifier.extensions.onNotificationClicked
import com.mmk.kmpnotifier.push.firebase.onApplicationDidReceiveRemoteNotification
import platform.UserNotifications.UNNotificationResponse

/**
 * Bridge functions called from the iOS AppDelegate (Swift) to forward remote-notification
 * lifecycle callbacks into kmpnotifier. Mirrors KotlinConf's IOSNotifications.kt.
 */

fun handleRemoteNotification(userInfo: Map<Any?, *>) {
    KMPNotifier.onApplicationDidReceiveRemoteNotification(userInfo)
}

fun handleNotificationResponse(response: UNNotificationResponse) {
    KMPNotifier.onNotificationClicked(response.notification.request.content)
}
