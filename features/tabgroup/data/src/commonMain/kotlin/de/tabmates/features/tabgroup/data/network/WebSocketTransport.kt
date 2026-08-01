package de.tabmates.features.tabgroup.data.network

import de.tabmates.core.data.AppBuildInfo
import de.tabmates.core.domain.environment.EnvironmentRepository
import de.tabmates.core.domain.logging.TabMatesLogger
import de.tabmates.features.tabgroup.data.network.dto.WebSocketMessageDto
import io.ktor.client.HttpClient
import io.ktor.client.plugins.websocket.webSocketSession
import io.ktor.client.request.header
import io.ktor.http.URLBuilder
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
    private val environmentRepository: EnvironmentRepository,
) {
    /**
     * A 426 here surfaces only as a failed handshake, not as the forced-update prompt: the gate
     * rejects the upgrade before there is a session to report on. Acceptable because the app always
     * issues plain HTTP calls too, and those trip `UpgradeRequiredNotifier` first.
     */
    suspend fun openSession(accessToken: String): WebSocketSession {
        // Read per connect: a reconnect after an environment switch has to dial the new backend.
        val environment = environmentRepository.current
        return httpClient.webSocketSession(
            urlString =
                URLBuilder("${environment.wsBaseUrl}/group")
                    .apply {
                        // Browser WebSockets cannot carry custom headers, so the credentials
                        // also travel as query parameters; the server accepts either.
                        parameters.append("access_token", accessToken)
                        // Null on web (server allow-lists the Origin instead); real key on native.
                        environment.apiKey?.let { parameters.append("api_key", it) }
                        // The handshake is version-gated like any other request, and browsers
                        // cannot set headers on it — so the version has to travel as a parameter.
                        // The token deliberately does not: only native builds have one, and native
                        // already sends it as a header below. A query parameter would only add it
                        // to server access logs, proxy logs and browser history for nothing.
                        parameters.append("client_version", AppBuildInfo.clientVersionHeader)
                    }.buildString(),
        ) {
            header("Authorization", "Bearer $accessToken")
            environment.apiKey?.let { header("x-api-key", it) }
            header("X-Client-Version", AppBuildInfo.clientVersionHeader)
            AppBuildInfo.buildToken?.let { header("X-Client-Token", it) }
        }
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
        private const val TAG = "WebSocketTransport"
    }
}
