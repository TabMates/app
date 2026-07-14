package de.tabmates.features.notifications.data.di

import de.tabmates.core.data.di.APPLICATION_SCOPE
import de.tabmates.core.data.di.WS_BASE_URL
import de.tabmates.core.domain.logging.TabMatesLogger
import de.tabmates.core.domain.preferences.LocaleProvider
import de.tabmates.features.notifications.data.DesktopPushNotificationController
import de.tabmates.features.notifications.data.UnsupportedNotificationPermissionController
import de.tabmates.features.notifications.domain.NotificationDeepLinkBus
import de.tabmates.features.notifications.domain.NotificationPermissionController
import de.tabmates.features.notifications.domain.PushNotificationController
import io.ktor.client.HttpClient
import kotlinx.coroutines.CoroutineScope
import kotlinx.serialization.json.Json
import org.koin.core.annotation.Configuration
import org.koin.core.annotation.Module
import org.koin.core.annotation.Named
import org.koin.core.annotation.Single

@Module
@Configuration
actual class PlatformNotificationsModule {
    @Single
    fun providePushNotificationController(
        httpClient: HttpClient,
        json: Json,
        @Named(WS_BASE_URL) wsBaseUrl: String,
        @Named(APPLICATION_SCOPE) scope: CoroutineScope,
        logger: TabMatesLogger,
        deepLinkBus: NotificationDeepLinkBus,
        localeProvider: LocaleProvider,
    ): PushNotificationController =
        DesktopPushNotificationController(
            httpClient = httpClient,
            json = json,
            wsBaseUrl = wsBaseUrl,
            appScope = scope,
            logger = logger,
            deepLinkBus = deepLinkBus,
            localeProvider = localeProvider,
        )

    @Single
    fun provideNotificationPermissionController(): NotificationPermissionController =
        UnsupportedNotificationPermissionController()
}
