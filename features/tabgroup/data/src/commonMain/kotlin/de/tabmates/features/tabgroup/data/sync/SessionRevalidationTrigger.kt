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
import kotlin.time.Clock
import kotlin.time.Duration.Companion.seconds
import kotlin.time.Instant

/**
 * Breaks the stale-token dead-end.
 *
 * A WebSocket handshake made with an expired access token is rejected with a 401. That surfaces as
 * a plain connection error — Android and iOS classify it as non-retriable so the flow terminates,
 * and the browser hides the handshake status from wasm entirely — leaving the app looking
 * permanently offline: the socket cannot connect, and because [SyncReconnectTrigger] only fires on
 * `CONNECTED`, no HTTP request is ever made either. Ktor's `Auth` plugin therefore never gets the
 * 401 it needs to refresh or invalidate the session, and the outbox never drains.
 *
 * So: whenever the socket lands in an error state while the device *does* have connectivity and a
 * session exists, make one throttled authenticated HTTP call. That call either refreshes the
 * tokens — the new session re-triggers the socket with a fresh token — or gets the refresh token
 * rejected, which puts the app into the recoverable expired-session state. Doing it over HTTP
 * keeps the recovery identical on every platform, independent of how each classifies socket errors.
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
    private var lastAttemptAt: Instant? = null

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

        val now = Clock.System.now()
        val last = lastAttemptAt
        if (last != null && now - last < THROTTLE) return
        lastAttemptAt = now

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
