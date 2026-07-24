@file:OptIn(kotlin.js.ExperimentalWasmJsInterop::class)

package de.tabmates.features.notifications.data

import de.tabmates.core.domain.logging.TabMatesLogger
import de.tabmates.core.domain.util.onFailure
import de.tabmates.core.domain.util.onSuccess
import de.tabmates.features.notifications.domain.DevicePlatform
import de.tabmates.features.notifications.domain.NotificationService
import de.tabmates.features.notifications.domain.PushNotificationController
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.await
import kotlinx.coroutines.launch
import kotlin.js.Promise

/**
 * Web push via the Firebase JS SDK. The actual Firebase calls live in `firebase-init.js`
 * (loaded by index.html); this controller drives them through a thin JS glue and registers
 * the resulting FCM web token with the backend. Background notifications are handled by the
 * `firebase-messaging-sw.js` service worker.
 */
class WebPushNotificationController(
    private val notificationService: NotificationService,
    private val appScope: CoroutineScope,
    private val logger: TabMatesLogger,
    private val tokenStore: PushTokenStore,
) : PushNotificationController {
    override fun start() {
        fcmInit()
        appScope.launch {
            runCatching {
                val token = requestToken() ?: return@launch
                registerAndCache(token, force = false)
            }.onFailure { logger.error(TAG, "Web FCM init failed", it) }
        }
    }

    override suspend fun refreshRegistration() {
        val token = runCatching { requestToken() }.getOrNull() ?: return
        // Force: locale may have changed even though the token is the same.
        registerAndCache(token, force = true)
    }

    override suspend fun stop() {
        val token = runCatching { requestToken() }.getOrNull()
        if (token != null) {
            notificationService
                .unregisterDevice(token)
                .onFailure { logger.warning(TAG, "Device unregistration failed: $it") }
        }
        // Clear regardless so the next login always re-registers.
        tokenStore.setLastRegistered(null)
    }

    private suspend fun registerAndCache(
        token: String,
        force: Boolean,
    ) {
        if (!force && token == tokenStore.lastRegistered()) return
        notificationService
            .registerDevice(token, DevicePlatform.WEB)
            .onSuccess { tokenStore.setLastRegistered(token) }
            .onFailure { logger.warning(TAG, "Device registration failed: $it") }
    }

    // No VAPID key configured (unset FCM_VAPID_KEY) means push can't be subscribed to yet;
    // treat it the same as the Firebase glue being absent rather than calling into JS with
    // a missing key.
    private suspend fun requestToken(): String? {
        val vapidKey = BuildKonfig.FCM_VAPID_KEY ?: return null
        return fcmRequestToken(vapidKey).await()?.toString()
    }

    private companion object {
        private const val TAG = "PushNotifications"
    }
}

// Guarded against the Firebase glue being absent (e.g. an offline PWA launch where the
// cross-origin Firebase SDK failed to load): fall back to a no-op / null token so startup never
// throws. firebase-init.js normally installs real implementations.
private fun fcmInit() {
    js("(typeof window.tabmatesFcmInit === 'function') && window.tabmatesFcmInit()")
}

private fun fcmRequestToken(vapidKey: String): Promise<JsString?> =
    js(
        "(typeof window.tabmatesFcmRequestToken === 'function' ? window.tabmatesFcmRequestToken(vapidKey) : Promise.resolve(null))",
    )
