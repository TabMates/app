package de.tabmates.features.tabgroup.data.sync

import de.tabmates.core.data.di.APPLICATION_SCOPE
import de.tabmates.core.domain.logging.TabMatesLogger
import de.tabmates.core.domain.util.onFailure
import de.tabmates.core.domain.util.onSuccess
import de.tabmates.features.tabgroup.data.network.ConnectionState
import de.tabmates.features.tabgroup.data.network.KtorWebSocketConnector
import de.tabmates.features.tabgroup.domain.activity.ActivityRepository
import de.tabmates.features.tabgroup.domain.sync.SyncRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import org.koin.core.annotation.Named
import org.koin.core.annotation.Single

/**
 * Runs a delta sync whenever the WebSocket (re)connects, catching up on anything missed while the
 * socket was down. The connection-state [StateFlow][kotlinx.coroutines.flow.StateFlow] only re-emits
 * `CONNECTED` after leaving it, so this fires once per reconnect. Runs serialized with the login
 * sync via the [SyncRepository]'s internal mutex.
 */
@Single(createdAtStart = true)
class SyncReconnectTrigger(
    webSocketConnector: KtorWebSocketConnector,
    private val syncRepository: SyncRepository,
    private val activityRepository: ActivityRepository,
    private val logger: TabMatesLogger,
    @Named(APPLICATION_SCOPE) applicationScope: CoroutineScope,
) {
    init {
        webSocketConnector
            .connectionState
            .filter { it == ConnectionState.CONNECTED }
            .onEach {
                syncRepository
                    .sync()
                    // Activity events foreign-key onto their group, so the mirror can only be
                    // written once group sync has landed the groups themselves.
                    .onSuccess {
                        activityRepository
                            .sync()
                            .onFailure { error -> logger.warning(TAG, "Reconnect activity sync failed: $error") }
                    }.onFailure { error -> logger.warning(TAG, "Reconnect delta sync failed: $error") }
            }.launchIn(applicationScope)
    }

    private companion object {
        private const val TAG = "SyncReconnectTrigger"
    }
}
