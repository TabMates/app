package de.tabmates.features.tabgroup.data.sync

import de.tabmates.core.data.di.APPLICATION_SCOPE
import de.tabmates.core.domain.auth.SessionStorage
import de.tabmates.core.domain.logging.TabMatesLogger
import de.tabmates.core.domain.util.onFailure
import de.tabmates.features.tabgroup.data.network.ConnectionGate
import de.tabmates.features.tabgroup.data.network.ConnectionState
import de.tabmates.features.tabgroup.data.network.KtorWebSocketConnector
import de.tabmates.features.tabgroup.domain.sync.SyncRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import org.koin.core.annotation.Named
import org.koin.core.annotation.Single
import kotlin.time.Duration.Companion.seconds
import kotlin.time.TimeMark
import kotlin.time.TimeSource

/**
 * Breaks the stale-token dead-end.
 *
 * A WebSocket handshake made with an expired access token is rejected with a 401, but nothing here
 * ever sees that status: the connector reports only `ERROR_NETWORK`/`ERROR_UNKNOWN`, Android and
 * iOS classify the underlying exception as non-retriable so the flow terminates, and the browser
 * withholds the handshake response from wasm entirely. The app is then stuck looking permanently
 * offline — the socket cannot connect, and because [SyncReconnectTrigger] only fires on
 * `CONNECTED`, no HTTP request follows either, so Ktor's `Auth` plugin never gets the 401 it needs
 * to refresh or invalidate. The outbox never drains.
 *
 * This trigger deliberately does *not* try to tell an auth failure from a network one, because at
 * this layer it cannot. It fires on any socket error while the device has connectivity and a
 * session exists, and makes one throttled authenticated HTTP call to ask the question where the
 * answer is legible. That call either refreshes the tokens — the new session re-triggers the
 * socket — or gets the refresh rejected, which puts the app into the recoverable expired state. A
 * genuine network outage simply fails the probe and changes nothing.
 *
 * The collector is never cancelled: it is bound to the process-wide application scope by design,
 * so recovery keeps working regardless of which screen is up. Do not re-scope it to a ViewModel.
 */
@Single(createdAtStart = true)
class SessionRevalidationTrigger(
    webSocketConnector: KtorWebSocketConnector,
    private val connectionGate: ConnectionGate,
    private val sessionStorage: SessionStorage,
    private val syncRepository: SyncRepository,
    private val logger: TabMatesLogger,
    @Named(APPLICATION_SCOPE) applicationScope: CoroutineScope,
) {
    /**
     * Monotonic rather than wall-clock: a device that corrects its clock (or crosses a DST/timezone
     * jump) must not be able to widen or collapse the throttle window.
     */
    private var lastAttemptAt: TimeMark? = null

    init {
        webSocketConnector
            .connectionState
            .filter { it == ConnectionState.ERROR_NETWORK || it == ConnectionState.ERROR_UNKNOWN }
            .onEach { revalidate() }
            .launchIn(applicationScope)
    }

    private suspend fun revalidate() {
        // No session means the app is already in the signed-out or expired state; nothing to prove.
        if (sessionStorage.get() == null) return
        // A socket error with the device genuinely offline is just an offline device.
        if (!connectionGate.isConnected.value) return

        val last = lastAttemptAt
        if (last != null && last.elapsedNow() < THROTTLE) return
        lastAttemptAt = TimeSource.Monotonic.markNow()

        logger.info(TAG, "Socket errored while online — probing the session over HTTP")
        syncRepository
            .sync()
            .onFailure { error -> logger.warning(TAG, "Session revalidation failed: $error") }
    }

    private companion object {
        private const val TAG = "SessionRevalidationTrigger"

        /** Socket errors can arrive in bursts; one probe per window is enough to settle the question. */
        private val THROTTLE = 30.seconds
    }
}
