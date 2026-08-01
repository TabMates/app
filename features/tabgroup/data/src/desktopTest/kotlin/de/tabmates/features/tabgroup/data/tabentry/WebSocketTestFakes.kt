package de.tabmates.features.tabgroup.data.tabentry

import de.tabmates.core.domain.auth.StaleSession
import de.tabmates.core.domain.auth.StaleSessionStore
import de.tabmates.core.domain.util.DataError
import de.tabmates.core.domain.util.EmptyResult
import de.tabmates.core.domain.util.Result
import de.tabmates.features.tabgroup.data.network.ConnectionState
import de.tabmates.features.tabgroup.data.network.WebSocketChannel
import de.tabmates.features.tabgroup.data.network.dto.WebSocketMessageDto
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow

/** A socket the test drives by hand: what was sent, and what the server says back. */
class FakeWebSocketChannel : WebSocketChannel {
    private val _connectionState = MutableStateFlow(ConnectionState.DISCONNECTED)
    override val connectionState: StateFlow<ConnectionState> = _connectionState.asStateFlow()

    private val _messages = MutableSharedFlow<WebSocketMessageDto>(extraBufferCapacity = 16)
    override val messages = _messages.asSharedFlow()

    val sent: MutableList<String> = mutableListOf()

    override suspend fun sendMessage(message: String): EmptyResult<DataError.Connection> {
        if (_connectionState.value != ConnectionState.CONNECTED) {
            return Result.Failure(DataError.Connection.NOT_CONNECTED)
        }
        sent += message
        return Result.Success(Unit)
    }

    fun connect() {
        _connectionState.value = ConnectionState.CONNECTED
    }

    fun disconnect() {
        _connectionState.value = ConnectionState.DISCONNECTED
    }

    fun emit(message: WebSocketMessageDto) {
        check(_messages.tryEmit(message)) { "no subscriber for $message" }
    }
}

class FakeStaleSessionStore : StaleSessionStore {
    private val _state = MutableStateFlow<StaleSession?>(null)
    override val state: StateFlow<StaleSession?> = _state.asStateFlow()

    override fun get(): StaleSession? = _state.value

    override fun set(session: StaleSession?) {
        _state.value = session
    }

    override fun clear() {
        _state.value = null
    }
}
