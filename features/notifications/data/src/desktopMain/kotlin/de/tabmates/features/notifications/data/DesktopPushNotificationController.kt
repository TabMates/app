package de.tabmates.features.notifications.data

import com.mmk.kmpnotifier.KMPNotifier
import com.mmk.kmpnotifier.local.LocalNotifications
import com.mmk.kmpnotifier.local.localNotifier
import com.mmk.kmpnotifier.notification.PayloadData
import com.mmk.kmpnotifier.notification.configuration.NotificationPlatformConfiguration
import de.tabmates.core.domain.environment.EnvironmentRepository
import de.tabmates.core.domain.logging.TabMatesLogger
import de.tabmates.core.domain.preferences.LocaleProvider
import de.tabmates.features.notifications.data.dto.NotificationEventDto
import de.tabmates.features.notifications.domain.NotificationDeepLinkBus
import de.tabmates.features.notifications.domain.PushNotificationConstants
import de.tabmates.features.notifications.domain.PushNotificationController
import io.ktor.client.HttpClient
import io.ktor.client.plugins.websocket.webSocket
import io.ktor.http.URLBuilder
import io.ktor.websocket.Frame
import io.ktor.websocket.readText
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.serialization.json.Json

/**
 * Desktop has no FCM client, so notifications are delivered over the backend WebSocket stream
 * and rendered as local desktop notifications via kmpnotifier. No device token is registered.
 */
class DesktopPushNotificationController(
    private val httpClient: HttpClient,
    private val json: Json,
    private val environmentRepository: EnvironmentRepository,
    private val appScope: CoroutineScope,
    private val logger: TabMatesLogger,
    private val deepLinkBus: NotificationDeepLinkBus,
    private val localeProvider: LocaleProvider,
) : PushNotificationController {
    private var streamJob: Job? = null
    private var notificationId = 0

    private val clickListener =
        object : KMPNotifier.Listener {
            override fun onNotificationClicked(data: PayloadData) {
                val deepLink = data[PushNotificationConstants.KEY_DEEP_LINK] as? String
                if (deepLink != null) {
                    logger.debug(TAG, "Notification clicked, routing deep link")
                    deepLinkBus.publish(deepLink)
                }
            }
        }

    override fun start() {
        KMPNotifier.initialize(
            NotificationPlatformConfiguration.Desktop(
                showPushNotification = true,
                // TODO: ship a real .png icon and point this at its extracted file path.
                notificationIconPath = "",
            ),
            LocalNotifications,
        )
        KMPNotifier.addListener(clickListener)
        if (streamJob?.isActive == true) return
        streamJob = launchStreamLoop()
    }

    private fun launchStreamLoop(): Job =
        appScope.launch {
            var attempt = 0
            while (isActive) {
                runCatching { collectStream() }
                    .onSuccess { attempt = 0 } // clean close — reset back-off
                    .onFailure {
                        attempt++
                        logger.warning(TAG, "Notification stream failed (attempt $attempt): ${it.message}")
                    }
                // Exponential back-off, capped, so a persistent failure doesn't hammer the backend.
                val delayMs =
                    minOf(
                        MAX_RECONNECT_DELAY_MS,
                        BASE_RECONNECT_DELAY_MS shl (attempt - 1).coerceIn(0, 5),
                    )
                delay(delayMs)
            }
        }

    private suspend fun collectStream() {
        // The server localizes the stream per the `lang` handshake query param (stream clients
        // register no device token, so it's the only locale signal). Resolved per-connect so a
        // reconnect after a language change picks up the new tag.
        val lang = localeProvider.currentLanguageTag()
        // Resolved per-connect like the language above, so a reconnect after an environment
        // switch dials the new backend instead of the one this controller started on.
        val wsBaseUrl = environmentRepository.current.wsBaseUrl
        val url =
            URLBuilder("$wsBaseUrl/api/notifications/stream")
                .apply { parameters.append("lang", lang) }
                .buildString()
        httpClient.webSocket(url) {
            for (frame in incoming) {
                if (frame !is Frame.Text) continue
                val event = json.decodeFromString<NotificationEventDto>(frame.readText())
                KMPNotifier.localNotifier.notify {
                    id = notificationId++
                    title = event.title
                    body = event.body
                    // Carry the deep link so onNotificationClicked can route it.
                    event.deepLink?.let { payloadData = mapOf(PushNotificationConstants.KEY_DEEP_LINK to it) }
                }
            }
        }
    }

    override suspend fun refreshRegistration() {
        // No device token to re-register, but the in-app language may have changed: reconnect so
        // the stream re-opens with the new `lang` and the server re-localizes its content.
        if (streamJob == null) return
        streamJob?.cancelAndJoin()
        streamJob = launchStreamLoop()
    }

    override suspend fun stop() {
        streamJob?.cancel()
        streamJob = null
    }

    private companion object {
        private const val TAG = "PushNotifications"
        private const val BASE_RECONNECT_DELAY_MS = 5_000L
        private const val MAX_RECONNECT_DELAY_MS = 160_000L
    }
}
