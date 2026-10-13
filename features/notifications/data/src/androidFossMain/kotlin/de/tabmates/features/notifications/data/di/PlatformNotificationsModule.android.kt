package de.tabmates.features.notifications.data.di

import de.tabmates.features.notifications.data.NoOpPushNotificationController
import de.tabmates.features.notifications.data.UnsupportedNotificationPermissionController
import de.tabmates.features.notifications.domain.NotificationPermissionController
import de.tabmates.features.notifications.domain.PushNotificationController
import org.koin.core.annotation.Configuration
import org.koin.core.annotation.Module
import org.koin.core.annotation.Single

/**
 * FOSS (F-Droid) Android notifications wiring: there is none.
 *
 * Push runs on Firebase Cloud Messaging, which is proprietary and therefore absent from this
 * build, and no local-notification source replaces it — so the controller is the shared no-op.
 *
 * The permission controller reports `UNSUPPORTED` rather than reading the real
 * `POST_NOTIFICATIONS` state, which keeps the settings screen honest: with nothing to deliver, a
 * permission toggle would control nothing. It is also the only correct answer here, because the
 * permission is not in this flavor's merged manifest at all — it reaches the Play build solely
 * through the `firebase-messaging` and `kmpnotifier-core` AAR manifests, and disappears with them.
 * Reading an undeclared permission always reports "denied", which the real controller would show
 * as a banner the user has no way to resolve.
 */
@Module
@Configuration
actual class PlatformNotificationsModule {
    @Single
    fun providePushNotificationController(): PushNotificationController = NoOpPushNotificationController()

    @Single
    fun provideNotificationPermissionController(): NotificationPermissionController =
        UnsupportedNotificationPermissionController()
}
