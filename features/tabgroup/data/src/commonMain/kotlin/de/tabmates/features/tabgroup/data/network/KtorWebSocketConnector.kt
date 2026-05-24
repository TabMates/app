@file:OptIn(FlowPreview::class, ExperimentalCoroutinesApi::class)

package de.tabmates.features.tabgroup.data.network

import de.tabmates.core.data.di.APPLICATION_SCOPE
import de.tabmates.core.domain.auth.SessionStorage
import de.tabmates.core.domain.logging.TabMatesLogger
import de.tabmates.core.domain.util.DataError
import de.tabmates.core.domain.util.EmptyResult
import de.tabmates.core.domain.util.Result
import de.tabmates.features.tabgroup.data.BuildKonfig
import de.tabmates.features.tabgroup.data.lifecycle.AppLifecycleObserver
import de.tabmates.features.tabgroup.data.network.dto.WebSocketMessageDto
import io.ktor.client.HttpClient
import io.ktor.client.plugins.websocket.webSocketSession
import io.ktor.client.request.header
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
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.retryWhen
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import org.koin.core.annotation.Named
import org.koin.core.annotation.Single
import kotlin.coroutines.coroutineContext
import kotlin.time.Duration.Companion.seconds

@Single
class KtorWebSocketConnector(
    private val httpClient: HttpClient,
    @Named(APPLICATION_SCOPE) private val applicationScope: CoroutineScope,
    private val sessionStorage: SessionStorage,
    private val json: Json,
    private val connectionErrorHandler: ConnectionErrorHandler,
    private val connectionRetryHandler: ConnectionRetryHandler,
    private val appLifecycleObserver: AppLifecycleObserver,
    private val connectivityObserver: ConnectivityObserver,
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

    private val isConnected =
        connectivityObserver
            .isConnected
            .debounce(1.seconds)
            .stateIn(
                applicationScope,
                SharingStarted.WhileSubscribed(5_000L),
                false,
            )

    private val isInForeground =
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

    val messages =
        combine(
            sessionStorage.authState,
            isConnected,
            isInForeground,
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

                        val transformedException = connectionErrorHandler.transformException(e)
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

                        _connectionState.value = connectionErrorHandler.getConnectionStateForError(e)
                    }
            }
        }

    private fun createWebSocketFlow(accessToken: String) =
        callbackFlow {
            _connectionState.value = ConnectionState.CONNECTING

            val session =
                httpClient.webSocketSession(
                    urlString = "${BuildKonfig.BASE_URL_WS}/group",
                ) {
                    header("Authorization", "Bearer $accessToken")
                    header("x-api-key", BuildKonfig.API_KEY)
                }
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

                            val messageDto =
                                try {
                                    json.decodeFromString<WebSocketMessageDto>(text)
                                } catch (e: Exception) {
                                    coroutineContext.ensureActive()
                                    logger.error(TAG, "Could not decode WS frame, skipping: $text", e)
                                    null
                                }
                            if (messageDto != null) {
                                send(messageDto)
                            }
                        }

                        is Frame.Ping -> {
                            logger.debug(TAG, "Received ping from server. Sending pong...")
                            session.send(Frame.Pong(frame.data))
                        }

                        else -> {
                            Unit
                        }
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
        const val TAG = "KtorWebSocketConnector"
    }
}
