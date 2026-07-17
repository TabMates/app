package de.tabmates.features.tabgroup.data.sync

import de.tabmates.core.data.di.APPLICATION_SCOPE
import de.tabmates.core.domain.sync.LastServerContactStore
import de.tabmates.features.tabgroup.data.network.ConnectionState
import de.tabmates.features.tabgroup.data.network.KtorWebSocketConnector
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import org.koin.core.annotation.Named
import org.koin.core.annotation.Single

/**
 * Stamps the [LastServerContactStore] the moment the socket leaves `CONNECTED`: realtime events
 * kept local data fresh right up to the drop, so the disconnect edge *is* the last server contact.
 * While connected nothing is written (the banner is hidden anyway); successful delta syncs stamp
 * separately in [OfflineFirstSyncRepository].
 */
@Single(createdAtStart = true)
class LastServerContactTracker(
    webSocketConnector: KtorWebSocketConnector,
    lastServerContactStore: LastServerContactStore,
    @Named(APPLICATION_SCOPE) applicationScope: CoroutineScope,
) {
    init {
        var previous = webSocketConnector.connectionState.value
        webSocketConnector
            .connectionState
            .onEach { current ->
                if (leftConnectedState(previous, current)) {
                    lastServerContactStore.recordContactNow()
                }
                previous = current
            }.launchIn(applicationScope)
    }
}

internal fun leftConnectedState(
    previous: ConnectionState,
    current: ConnectionState,
): Boolean = previous == ConnectionState.CONNECTED && current != ConnectionState.CONNECTED
