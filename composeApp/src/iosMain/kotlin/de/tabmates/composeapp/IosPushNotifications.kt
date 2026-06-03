package de.tabmates.composeapp

import com.mmk.kmpnotifier.extensions.onApplicationDidReceiveRemoteNotification
import com.mmk.kmpnotifier.extensions.onNotificationClicked
import com.mmk.kmpnotifier.notification.NotifierManager
import platform.UserNotifications.UNNotificationResponse

/**
 * Bridge functions called from the iOS AppDelegate (Swift) to forward remote-notification
 * lifecycle callbacks into kmpnotifier. Mirrors KotlinConf's IOSNotifications.kt.
 */

fun handleRemoteNotification(userInfo: Map<Any?, *>) {
    NotifierManager.onApplicationDidReceiveRemoteNotification(userInfo)
}

fun handleNotificationResponse(response: UNNotificationResponse) {
    NotifierManager.onNotificationClicked(response.notification.request.content)
}
