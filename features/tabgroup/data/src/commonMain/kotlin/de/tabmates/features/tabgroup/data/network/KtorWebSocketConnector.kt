@file:OptIn(FlowPreview::class, ExperimentalCoroutinesApi::class)

package de.tabmates.features.tabgroup.data.network

import de.tabmates.core.data.di.APPLICATION_SCOPE
import de.tabmates.core.domain.logging.TabMatesLogger
import de.tabmates.core.domain.util.DataError
import de.tabmates.core.domain.util.EmptyResult
import de.tabmates.core.domain.util.Result
import io.ktor.websocket.Frame
import io.ktor.websocket.WebSocketSession
import io.ktor.websocket.close
import io.ktor.websocket.readText
import io.ktor.websocket.send
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.buffer
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.consumeAsFlow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.retryWhen
import kotlinx.coroutines.flow.shareIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import org.koin.core.annotation.Named
import org.koin.core.annotation.Single
import kotlin.coroutines.coroutineContext

@Single
class KtorWebSocketConnector(
    @Named(APPLICATION_SCOPE) private val applicationScope: CoroutineScope,
    private val connectionGate: ConnectionGate,
    private val transport: WebSocketTransport,
    private val connectionRetryHandler: ConnectionRetryHandler,
    private val logger: TabMatesLogger,
) {
    private val _connectionState = MutableStateFlow(ConnectionState.DISCONNECTED)
    val connectionState = _connectionState.asStateFlow()

    private val sessionMutex = Mutex()
    private var currentSession: WebSocketSession? = null

    private suspend fun setCurrentSession(session: WebSocketSession?) {
        sessionMutex.withLock { currentSession = session }
    }

    private suspend fun currentSessionSnapshot(): WebSocketSession? = sessionMutex.withLock { currentSession }

    private suspend fun closeAndClearSession() {
        sessionMutex.withLock {
            currentSession?.close()
            currentSession = null
        }
    }

    /**
     * The inbound frame stream, **shared**: one socket no matter how many collectors there are.
     *
     * Without [shareIn] this is a cold flow, and every collector runs its own
     * [createWebSocketFlow] — two `createdAtStart` singletons collect it, so the app held two live
     * sockets per user. Broadcasts hid that (the server fans them out to every session a user has,
     * and applying one twice is an idempotent upsert), but a unicast frame does not: it arrives on
     * exactly one socket, and [sendMessage] writes to whichever of the two won the last-writer race
     * in [setCurrentSession]. A frame addressed to the sender would land in a nondeterministic
     * collector.
     *
     * `replay = 0` because every frame is applied to the database on arrival; a replayed one would
     * be re-applied for each new subscriber.
     */
    val messages =
        combine(
            connectionGate.authState,
            connectionGate.isConnected,
            connectionGate.isInForeground,
        ) { authInfo, isConnected, isInForeground ->
            when {
                authInfo == null -> {
                    logger.info(TAG, "No authentication details. Clearing session and disconnecting...")
                    _connectionState.value = ConnectionState.DISCONNECTED
                    closeAndClearSession()
                    connectionRetryHandler.resetDelay()
                    null
                }

                !isInForeground -> {
                    logger.info(TAG, "App in background, disconnecting socket proactively.")
                    _connectionState.value = ConnectionState.DISCONNECTED
                    closeAndClearSession()
                    null
                }

                !isConnected -> {
                    logger.info(TAG, "Device is disconnected, closing WebSocket connection.")
                    _connectionState.value = ConnectionState.ERROR_NETWORK
                    closeAndClearSession()
                    null
                }

                else -> {
                    logger.info(TAG, "App in foreground & connected. Establishing connection...")

                    if (_connectionState.value !in
                        listOf(
                            ConnectionState.CONNECTING,
                            ConnectionState.CONNECTED,
                        )
                    ) {
                        _connectionState.value = ConnectionState.CONNECTING
                    }

                    authInfo
                }
            }
        }.flatMapLatest { authInfo ->
            if (authInfo == null) {
                emptyFlow()
            } else {
                createWebSocketFlow(authInfo.accessToken)
                    // Catch block to transform exceptions for platform compatibility
                    .catch { e ->
                        logger.error(TAG, "Exception in WebSocket", e)

                        closeAndClearSession()

                        val transformedException = connectionRetryHandler.transformException(e)
                        throw transformedException
                    }.retryWhen { t, attempt ->
                        logger.info(TAG, "Connection failed on attempt $attempt")

                        val shouldRetry = connectionRetryHandler.shouldRetry(t)

                        if (shouldRetry) {
                            _connectionState.value = ConnectionState.CONNECTING
                            connectionRetryHandler.applyRetryDelay(attempt)
                        }

                        shouldRetry
                    }
                    // Catch block for non-retriable errors
                    .catch { e ->
                        logger.error(TAG, "Unhandled WebSocket error", e)

                        _connectionState.value = connectionRetryHandler.getConnectionStateForError(e)
                    }
            }
        }.shareIn(
            scope = applicationScope,
            started = SharingStarted.WhileSubscribed(SHARE_STOP_TIMEOUT_MS),
            replay = 0,
        )

    private fun createWebSocketFlow(accessToken: String) =
        callbackFlow {
            _connectionState.value = ConnectionState.CONNECTING

            val session = transport.openSession(accessToken)
            setCurrentSession(session)
            _connectionState.value = ConnectionState.CONNECTED

            session
                .incoming
                .consumeAsFlow()
                .buffer(capacity = 100)
                .collect { frame ->
                    when (frame) {
                        is Frame.Text -> {
                            val text = frame.readText()
                            logger.debug(TAG, "Received raw text frame: $text")

                            val messageDto = transport.decode(text)
                            if (messageDto != null) {
                                send(messageDto)
                            }
                        }

                        is Frame.Ping -> {
                            logger.debug(TAG, "Received ping from server. Sending pong...")
                            session.send(Frame.Pong(frame.data))
                        }

                        else -> {}
                    }
                }

            awaitClose {
                applicationScope.launch {
                    withContext(NonCancellable) {
                        logger.info(TAG, "Disconnecting from WebSocket session...")
                        _connectionState.value = ConnectionState.DISCONNECTED
                        closeAndClearSession()
                    }
                }
            }
        }

    suspend fun sendMessage(message: String): EmptyResult<DataError.Connection> {
        val session = currentSessionSnapshot()
        if (session == null || connectionState.value != ConnectionState.CONNECTED) {
            return Result.Failure(DataError.Connection.NOT_CONNECTED)
        }

        return try {
            session.send(message)
            Result.Success(Unit)
        } catch (e: Exception) {
            coroutineContext.ensureActive()
            logger.error(TAG, "Unable to send WebSocket message", e)
            Result.Failure(DataError.Connection.MESSAGE_SEND_FAILED)
        }
    }

    private companion object {
        private const val TAG = "KtorWebSocketConnector"

        // Matches ConnectionGate's own stateIn windows, so a brief gap between collectors does not
        // tear the socket down and immediately redial it.
        private const val SHARE_STOP_TIMEOUT_MS = 5_000L
    }
}
