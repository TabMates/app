package de.tabmates.core.data.networking

import de.tabmates.core.data.BuildKonfig
import de.tabmates.core.domain.logging.TabMatesLogger
import io.ktor.client.HttpClient
import io.ktor.client.engine.HttpClientEngine
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.plugins.defaultRequest
import io.ktor.client.plugins.logging.LogLevel
import io.ktor.client.plugins.logging.Logger
import io.ktor.client.plugins.logging.Logging
import io.ktor.client.plugins.websocket.WebSockets
import io.ktor.client.request.header
import io.ktor.http.ContentType
import io.ktor.http.contentType
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.json.Json

private const val TAG = "HttpClientFactory"

class HttpClientFactory(
    private val tabMatesLogger: TabMatesLogger,
) {
    fun create(engine: HttpClientEngine): HttpClient {
        return HttpClient(engine) {
            install(ContentNegotiation) {
                json(
                    json =
                        Json {
                            ignoreUnknownKeys = true
                        },
                )
            }
            install(HttpTimeout) {
                socketTimeoutMillis = 20_000L
                requestTimeoutMillis = 20_000L
            }
            install(Logging) {
                logger =
                    object : Logger {
                        override fun log(message: String) {
                            tabMatesLogger.debug(TAG, message)
                        }
                    }
                level = if (BuildKonfig.IS_DEBUG) LogLevel.ALL else LogLevel.INFO
            }
            install(WebSockets) {
                pingIntervalMillis = 20_000L
            }
            defaultRequest {
                header("x-api-key", BuildKonfig.API_KEY)
                contentType(ContentType.Application.Json)
            }
        }
    }
}
