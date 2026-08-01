package de.tabmates.features.tabgroup.data.network

import de.tabmates.core.domain.util.DataError
import de.tabmates.core.domain.util.EmptyResult
import de.tabmates.features.tabgroup.data.network.dto.WebSocketMessageDto
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow

/**
 * The socket as the outbox needs it: send a frame, watch the connection, read what comes back.
 *
 * Exists so the outbox's acknowledgement handling can be tested — [KtorWebSocketConnector] opens a
 * real Ktor session on construction and cannot stand in for one. Every other collaborator still
 * depends on the connector directly.
 */
interface WebSocketChannel {
    val connectionState: StateFlow<ConnectionState>

    val messages: Flow<WebSocketMessageDto>

    suspend fun sendMessage(message: String): EmptyResult<DataError.Connection>
}
