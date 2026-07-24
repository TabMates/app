package de.tabmates.core.data.networking

import de.tabmates.core.data.BuildKonfig
import de.tabmates.core.data.dto.AuthInfoSerializable
import de.tabmates.core.data.dto.requests.RefreshRequest
import de.tabmates.core.data.mappers.toDomain
import de.tabmates.core.domain.auth.SessionStorage
import de.tabmates.core.domain.logging.TabMatesLogger
import de.tabmates.core.domain.util.DataError
import de.tabmates.core.domain.util.onFailure
import de.tabmates.core.domain.util.onSuccess
import io.ktor.client.HttpClient
import io.ktor.client.engine.HttpClientEngine
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.plugins.auth.Auth
import io.ktor.client.plugins.auth.providers.BearerTokens
import io.ktor.client.plugins.auth.providers.bearer
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.plugins.defaultRequest
import io.ktor.client.plugins.logging.LogLevel
import io.ktor.client.plugins.logging.Logger
import io.ktor.client.plugins.logging.Logging
import io.ktor.client.plugins.websocket.WebSockets
import io.ktor.client.request.header
import io.ktor.client.statement.request
import io.ktor.http.ContentType
import io.ktor.http.contentType
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.json.Json

private const val TAG = "HttpClientFactory"

class HttpClientFactory(
    private val tabMatesLogger: TabMatesLogger,
    private val sessionStorage: SessionStorage,
    private val json: Json,
) {
    fun create(engine: HttpClientEngine): HttpClient {
        return HttpClient(engine) {
            install(ContentNegotiation) {
                json(json = json)
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
                // Null on web (server allow-lists the Origin instead); real key on native.
                BuildKonfig.API_KEY?.let { header("x-api-key", it) }
                contentType(ContentType.Application.Json)
            }

            install(Auth) {
                bearer {
                    loadTokens {
                        sessionStorage.get()?.let {
                            BearerTokens(
                                accessToken = it.accessToken,
                                refreshToken = it.refreshToken,
                            )
                        }
                    }
                    refreshTokens {
                        if (response.request.url.encodedPath
                                .contains("api/auth/")
                        ) {
                            return@refreshTokens null
                        }
                        val localAuthInfo = sessionStorage.get()
                        if (localAuthInfo?.refreshToken.isNullOrBlank()) {
                            return@refreshTokens null
                        }

                        var bearerTokens: BearerTokens? = null
                        client
                            .post<RefreshRequest, AuthInfoSerializable>(
                                route = "/api/auth/refresh",
                                body =
                                    RefreshRequest(
                                        refreshToken = localAuthInfo.refreshToken,
                                    ),
                                builder = {
                                    markAsRefreshTokenRequest()
                                },
                            ).onSuccess { newAuthInfo ->
                                sessionStorage.set(newAuthInfo.toDomain())
                                bearerTokens =
                                    BearerTokens(
                                        accessToken = newAuthInfo.accessToken,
                                        refreshToken = newAuthInfo.refreshToken,
                                    )
                            }.onFailure {
                                if (it.isAuthRejection()) {
                                    sessionStorage.set(null)
                                }
                            }

                        bearerTokens
                    }
                }
            }
        }
    }

    private fun DataError.Remote.isAuthRejection(): Boolean =
        this == DataError.Remote.UNAUTHORIZED ||
            this == DataError.Remote.FORBIDDEN
}
