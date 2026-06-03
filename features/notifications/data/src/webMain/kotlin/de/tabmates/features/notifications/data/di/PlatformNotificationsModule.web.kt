package de.tabmates.features.notifications.data.di

import de.tabmates.core.data.di.APPLICATION_SCOPE
import de.tabmates.core.domain.logging.TabMatesLogger
import de.tabmates.features.notifications.data.PushTokenStore
import de.tabmates.features.notifications.data.WebNotificationPermissionController
import de.tabmates.features.notifications.data.WebPushNotificationController
import de.tabmates.features.notifications.domain.NotificationPermissionController
import de.tabmates.features.notifications.domain.NotificationService
import de.tabmates.features.notifications.domain.PushNotificationController
import kotlinx.coroutines.CoroutineScope
import org.koin.core.annotation.Configuration
import org.koin.core.annotation.Module
import org.koin.core.annotation.Named
import org.koin.core.annotation.Single

@Module
@Configuration
actual class PlatformNotificationsModule {
    @Single
    fun providePushNotificationController(
        notificationService: NotificationService,
        @Named(APPLICATION_SCOPE) scope: CoroutineScope,
        logger: TabMatesLogger,
        tokenStore: PushTokenStore,
    ): PushNotificationController =
        WebPushNotificationController(
            notificationService = notificationService,
            appScope = scope,
            logger = logger,
            tokenStore = tokenStore,
        )

    @Single
    fun provideNotificationPermissionController(): NotificationPermissionController =
        WebNotificationPermissionController()
}
