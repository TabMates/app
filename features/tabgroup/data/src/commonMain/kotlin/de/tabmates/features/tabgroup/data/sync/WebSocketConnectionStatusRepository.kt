package de.tabmates.features.tabgroup.data.sync

import de.tabmates.core.data.di.APPLICATION_SCOPE
import de.tabmates.core.domain.sync.LastServerContactStore
import de.tabmates.features.tabgroup.data.network.ConnectionState
import de.tabmates.features.tabgroup.data.network.KtorWebSocketConnector
import de.tabmates.features.tabgroup.domain.sync.ConnectionStatusRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import org.koin.core.annotation.Named
import org.koin.core.annotation.Single
import kotlin.time.Instant

/** [ConnectionStatusRepository] backed by the WebSocket connector. */
@Single(binds = [ConnectionStatusRepository::class])
class WebSocketConnectionStatusRepository(
    webSocketConnector: KtorWebSocketConnector,
    lastServerContactStore: LastServerContactStore,
    @Named(APPLICATION_SCOPE) applicationScope: CoroutineScope,
) : ConnectionStatusRepository {
    override val isConnected: StateFlow<Boolean> =
        webSocketConnector
            .connectionState
            .map { it == ConnectionState.CONNECTED }
            .stateIn(
                applicationScope,
                SharingStarted.WhileSubscribed(5_000L),
                webSocketConnector.connectionState.value == ConnectionState.CONNECTED,
            )

    override val lastServerContactAt: StateFlow<Instant?> = lastServerContactStore.lastContactAt
}
