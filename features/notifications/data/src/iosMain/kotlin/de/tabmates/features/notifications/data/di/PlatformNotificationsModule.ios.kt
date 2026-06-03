package de.tabmates.features.notifications.data.di

import com.mmk.kmpnotifier.notification.configuration.NotificationPlatformConfiguration
import de.tabmates.core.data.di.APPLICATION_SCOPE
import de.tabmates.core.domain.logging.TabMatesLogger
import de.tabmates.features.notifications.data.IosNotificationPermissionController
import de.tabmates.features.notifications.data.MobilePushNotificationController
import de.tabmates.features.notifications.data.PushTokenStore
import de.tabmates.features.notifications.domain.DevicePlatform
import de.tabmates.features.notifications.domain.NotificationDeepLinkBus
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
        deepLinkBus: NotificationDeepLinkBus,
    ): PushNotificationController =
        MobilePushNotificationController(
            notificationService = notificationService,
            appScope = scope,
            logger = logger,
            config =
                NotificationPlatformConfiguration.Ios(
                    showPushNotification = true,
                    askNotificationPermissionOnStart = true,
                    notificationSoundName = null,
                ),
            platform = DevicePlatform.IOS,
            tokenStore = tokenStore,
            deepLinkBus = deepLinkBus,
        )

    @Single
    fun provideNotificationPermissionController(): NotificationPermissionController =
        IosNotificationPermissionController()
}
