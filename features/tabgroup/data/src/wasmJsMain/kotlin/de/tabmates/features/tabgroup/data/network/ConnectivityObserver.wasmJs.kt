package de.tabmates.features.tabgroup.data.network

import kotlinx.browser.window
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import org.w3c.dom.events.Event

actual class ConnectivityObserver {
    actual val isConnected: Flow<Boolean> =
        callbackFlow {
            trySend(window.navigator.onLine)

            val onlineListener: (Event) -> Unit = { trySend(true) }
            val offlineListener: (Event) -> Unit = { trySend(false) }

            window.addEventListener("online", onlineListener)
            window.addEventListener("offline", offlineListener)

            awaitClose {
                window.removeEventListener("online", onlineListener)
                window.removeEventListener("offline", offlineListener)
            }
        }
}
