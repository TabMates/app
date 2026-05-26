package de.tabmates.features.tabgroup.data.network

import de.tabmates.core.domain.logging.TabMatesLogger
import de.tabmates.features.tabgroup.data.BuildKonfig
import de.tabmates.features.tabgroup.data.network.dto.WebSocketMessageDto
import io.ktor.client.HttpClient
import io.ktor.client.plugins.websocket.webSocketSession
import io.ktor.client.request.header
import io.ktor.websocket.WebSocketSession
import kotlinx.coroutines.ensureActive
import kotlinx.serialization.json.Json
import org.koin.core.annotation.Single
import kotlin.coroutines.coroutineContext

/**
 * Owns the raw Ktor WebSocket plumbing: opening an authenticated session and decoding
 * inbound text frames. Connection lifecycle/state is orchestrated by [KtorWebSocketConnector].
 */
@Single
class WebSocketTransport(
    private val httpClient: HttpClient,
    private val json: Json,
    private val logger: TabMatesLogger,
) {
    suspend fun openSession(accessToken: String): WebSocketSession =
        httpClient.webSocketSession(
            urlString = "${BuildKonfig.BASE_URL_WS}/group",
        ) {
            header("Authorization", "Bearer $accessToken")
            header("x-api-key", BuildKonfig.API_KEY)
        }

    suspend fun decode(text: String): WebSocketMessageDto? =
        try {
            json.decodeFromString<WebSocketMessageDto>(text)
        } catch (e: Exception) {
            coroutineContext.ensureActive()
            logger.error(TAG, "Could not decode WS frame, skipping: $text", e)
            null
        }

    private companion object {
        const val TAG = "WebSocketTransport"
    }
}
