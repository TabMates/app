package de.tabmates.features.tabgroup.data.lifecycle

import kotlinx.browser.document
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import org.w3c.dom.events.Event

actual class AppLifecycleObserver {
    actual val isInForeground: Flow<Boolean> =
        callbackFlow {
            trySend(!isDocumentHidden())

            val listener: (Event) -> Unit = { trySend(!isDocumentHidden()) }
            document.addEventListener("visibilitychange", listener)

            awaitClose {
                document.removeEventListener("visibilitychange", listener)
            }
        }
}

@OptIn(ExperimentalWasmJsInterop::class)
private fun isDocumentHidden(): Boolean = js("document.hidden")
