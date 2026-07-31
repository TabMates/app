package de.tabmates.core.data.networking

import de.tabmates.core.data.AppBuildInfo
import de.tabmates.core.data.BuildKonfig
import de.tabmates.core.data.dto.AuthInfoSerializable
import de.tabmates.core.data.dto.requests.RefreshRequest
import de.tabmates.core.data.mappers.toDomain
import de.tabmates.core.domain.auth.SessionInvalidationReason
import de.tabmates.core.domain.auth.SessionInvalidator
import de.tabmates.core.domain.auth.SessionStorage
import de.tabmates.core.domain.logging.TabMatesLogger
import de.tabmates.core.domain.update.UpgradeRequiredNotifier
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
import io.ktor.client.plugins.observer.ResponseObserver
import io.ktor.client.plugins.websocket.WebSockets
import io.ktor.client.request.header
import io.ktor.client.statement.request
import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
import io.ktor.http.contentType
import io.ktor.serialization.kotlinx.json.json
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.serialization.json.Json

private const val TAG = "HttpClientFactory"

class HttpClientFactory(
    private val tabMatesLogger: TabMatesLogger,
    private val sessionStorage: SessionStorage,
    private val json: Json,
    private val upgradeRequiredNotifier: UpgradeRequiredNotifier,
    private val sessionInvalidator: SessionInvalidator,
) {
    /** Serializes token refreshes; see the comment in `refreshTokens`. */
    private val refreshMutex = Mutex()

    fun create(engine: HttpClientEngine): HttpClient {
        return HttpClient(engine) {
            install(ContentNegotiation) {
                json(json = json)
            }
            // Catches the version gate's 426 for every endpoint at once, so a new call site cannot
            // forget it — same reasoning as the X-Client-Version header below.
            install(ResponseObserver) {
                onResponse { response ->
                    if (response.status == HttpStatusCode.UpgradeRequired) {
                        upgradeRequiredNotifier.notifyUpgradeRequired()
                    }
                }
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
                // Every /api/** and /ws/** call must declare its version or the server answers 426.
                // Set here rather than per call so a new endpoint cannot forget it.
                header("X-Client-Version", AppBuildInfo.clientVersionHeader)
                // Null on web, and on any build whose pipeline did not mint one.
                AppBuildInfo.buildToken?.let { header("X-Client-Token", it) }
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
                        // Snapshot the token this attempt is based on before queueing behind the
                        // mutex. Ktor does not serialize concurrent refreshes, so several 401s can
                        // arrive at once; the server rotates the refresh token, meaning whoever
                        // loses the race would present an already-spent token, get a 401, and
                        // invalidate a session that was just successfully renewed.
                        val attemptedWith = sessionStorage.get()?.refreshToken
                        if (attemptedWith.isNullOrBlank()) {
                            return@refreshTokens null
                        }

                        refreshMutex.withLock {
                            val current = sessionStorage.get() ?: return@refreshTokens null
                            if (current.refreshToken != attemptedWith) {
                                // Someone else rotated it while this call waited. Their tokens are
                                // the live ones.
                                return@refreshTokens BearerTokens(
                                    accessToken = current.accessToken,
                                    refreshToken = current.refreshToken,
                                )
                            }

                            var bearerTokens: BearerTokens? = null
                            client
                                .post<RefreshRequest, AuthInfoSerializable>(
                                    route = "/api/auth/refresh",
                                    body =
                                        RefreshRequest(
                                            refreshToken = current.refreshToken,
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
                                        // Records who just got logged out before dropping the
                                        // session, so the shell can keep their local data and ask
                                        // them back in.
                                        sessionInvalidator.invalidate(SessionInvalidationReason.TOKEN_REJECTED)
                                    }
                                }

                            bearerTokens
                        }
                    }
                }
            }
        }
    }

    private fun DataError.Remote.isAuthRejection(): Boolean =
        this == DataError.Remote.UNAUTHORIZED ||
            this == DataError.Remote.FORBIDDEN
}
