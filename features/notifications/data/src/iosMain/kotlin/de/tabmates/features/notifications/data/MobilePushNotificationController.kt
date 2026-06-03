package de.tabmates.features.notifications.data

import com.mmk.kmpnotifier.notification.NotifierManager
import com.mmk.kmpnotifier.notification.NotifierManager.Listener
import com.mmk.kmpnotifier.notification.PayloadData
import com.mmk.kmpnotifier.notification.configuration.NotificationPlatformConfiguration
import de.tabmates.core.domain.logging.TabMatesLogger
import de.tabmates.core.domain.util.onFailure
import de.tabmates.core.domain.util.onSuccess
import de.tabmates.features.notifications.domain.DevicePlatform
import de.tabmates.features.notifications.domain.NotificationDeepLinkBus
import de.tabmates.features.notifications.domain.NotificationService
import de.tabmates.features.notifications.domain.PushNotificationConstants
import de.tabmates.features.notifications.domain.PushNotificationController
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

/**
 * Push controller backed by Firebase Cloud Messaging (kmpnotifier). The platform-specific
 * [config] and [platform] are injected by the platform Koin module. Kept per-platform
 * (androidMain/iosMain) because this project's AGP KMP android target is not part of the
 * shared `mobileMain` source set.
 */
class MobilePushNotificationController(
    private val notificationService: NotificationService,
    private val appScope: CoroutineScope,
    private val logger: TabMatesLogger,
    private val config: NotificationPlatformConfiguration,
    private val platform: DevicePlatform,
    private val tokenStore: PushTokenStore,
    private val deepLinkBus: NotificationDeepLinkBus,
) : PushNotificationController {
    private val notifierManagerListener =
        object : Listener {
            override fun onNewToken(token: String) {
                logger.debug(TAG, "New push token received")
                registerToken(token)
            }

            override fun onNotificationClicked(data: PayloadData) {
                val deepLink = data[PushNotificationConstants.KEY_DEEP_LINK] as? String
                if (deepLink != null) {
                    logger.debug(TAG, "Notification clicked, routing deep link")
                    deepLinkBus.publish(deepLink)
                }
            }
        }

    override fun start() {
        NotifierManager.initialize(config)
        NotifierManager.addListener(notifierManagerListener)

        appScope.launch {
            val token = NotifierManager.getPushNotifier().getToken() ?: return@launch
            registerAndCache(token, force = false)
        }
    }

    override suspend fun refreshRegistration() {
        val token = NotifierManager.getPushNotifier().getToken() ?: return
        // Force: locale may have changed even though the token is the same.
        registerAndCache(token, force = true)
    }

    override suspend fun stop() {
        val token = NotifierManager.getPushNotifier().getToken()
        if (token != null) {
            notificationService
                .unregisterDevice(token)
                .onFailure { logger.warning(TAG, "Device unregistration failed: $it") }
        }
        // Clear regardless so the next login always re-registers.
        tokenStore.setLastRegistered(null)
    }

    private fun registerToken(token: String) {
        appScope.launch { registerAndCache(token, force = false) }
    }

    private suspend fun registerAndCache(
        token: String,
        force: Boolean,
    ) {
        if (!force && token == tokenStore.lastRegistered()) return
        notificationService
            .registerDevice(token, platform)
            .onSuccess { tokenStore.setLastRegistered(token) }
            .onFailure { logger.warning(TAG, "Device registration failed: $it") }
    }

    private companion object {
        private const val TAG = "PushNotifications"
    }
}
