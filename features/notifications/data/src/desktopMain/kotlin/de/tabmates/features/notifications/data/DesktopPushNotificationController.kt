package de.tabmates.features.notifications.data

import com.mmk.kmpnotifier.notification.NotifierManager
import com.mmk.kmpnotifier.notification.configuration.NotificationPlatformConfiguration
import de.tabmates.core.domain.logging.TabMatesLogger
import de.tabmates.features.notifications.data.dto.NotificationEventDto
import de.tabmates.features.notifications.domain.PushNotificationController
import io.ktor.client.HttpClient
import io.ktor.client.plugins.websocket.webSocket
import io.ktor.websocket.Frame
import io.ktor.websocket.readText
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
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
    private val wsBaseUrl: String,
    private val appScope: CoroutineScope,
    private val logger: TabMatesLogger,
) : PushNotificationController {
    private var streamJob: Job? = null

    override fun start() {
        NotifierManager.initialize(
            NotificationPlatformConfiguration.Desktop(
                showPushNotification = true,
                // TODO: ship a real .png icon and point this at its extracted file path.
                notificationIconPath = "",
            ),
        )
        if (streamJob?.isActive == true) return
        streamJob =
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
    }

    private suspend fun collectStream() {
        httpClient.webSocket("$wsBaseUrl/api/notifications/stream") {
            for (frame in incoming) {
                if (frame !is Frame.Text) continue
                val event = json.decodeFromString<NotificationEventDto>(frame.readText())
                NotifierManager.getLocalNotifier().notify(event.title, event.body)
            }
        }
    }

    override suspend fun refreshRegistration() {
        // No device token on desktop — nothing to refresh.
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
