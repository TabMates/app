@file:OptIn(FlowPreview::class)

package de.tabmates.features.tabgroup.data.network

import de.tabmates.core.data.di.APPLICATION_SCOPE
import de.tabmates.core.domain.auth.AuthInfo
import de.tabmates.core.domain.auth.SessionStorage
import de.tabmates.features.tabgroup.data.lifecycle.AppLifecycleObserver
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.stateIn
import org.koin.core.annotation.Named
import org.koin.core.annotation.Single
import kotlin.time.Duration.Companion.seconds

/**
 * Combines the three inputs that gate whether a WebSocket connection should be open:
 * auth state, device connectivity, and app foreground state. Returning to the foreground
 * resets the retry backoff so the next reconnect attempt is immediate.
 */
@Single
class ConnectionGate(
    sessionStorage: SessionStorage,
    connectivityObserver: ConnectivityObserver,
    appLifecycleObserver: AppLifecycleObserver,
    connectionRetryHandler: ConnectionRetryHandler,
    @Named(APPLICATION_SCOPE) applicationScope: CoroutineScope,
) {
    val authState: StateFlow<AuthInfo?> = sessionStorage.authState

    val isConnected: StateFlow<Boolean> =
        connectivityObserver
            .isConnected
            .debounce(1.seconds)
            .stateIn(
                applicationScope,
                SharingStarted.WhileSubscribed(5_000L),
                false,
            )

    val isInForeground: StateFlow<Boolean> =
        appLifecycleObserver
            .isInForeground
            .onEach { isInForeground ->
                if (isInForeground) {
                    connectionRetryHandler.resetDelay()
                }
            }.stateIn(
                applicationScope,
                SharingStarted.WhileSubscribed(5_000L),
                false,
            )
}
