package de.tabmates.features.authentication.data

import de.tabmates.core.data.networking.HttpClientFactory
import de.tabmates.core.data.networking.get
import de.tabmates.core.domain.auth.AuthInfo
import de.tabmates.core.domain.auth.SessionInvalidationReason
import de.tabmates.core.domain.auth.SessionInvalidator
import de.tabmates.core.domain.auth.SessionStorage
import de.tabmates.core.domain.auth.User
import de.tabmates.core.domain.auth.UserType
import de.tabmates.core.domain.environment.CustomEnvironment
import de.tabmates.core.domain.environment.EnvironmentConfig
import de.tabmates.core.domain.environment.EnvironmentRepository
import de.tabmates.core.domain.logging.TabMatesLogger
import de.tabmates.core.domain.update.UpgradeRequiredNotifier
import de.tabmates.features.authentication.domain.TurnstileTokenProvider
import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respondError
import io.ktor.client.engine.mock.respondOk
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.Json
import kotlin.test.Test
import kotlin.test.assertEquals

class KtorAuthServiceTest {
    /**
     * Ktor keeps the bearer tokens in memory and only reads the session back once that cache is
     * empty, so a sign-out whose revoke never reached the server used to leave the previous
     * account authenticating every following request.
     */
    @Test
    fun `a failed logout still drops the token the next account would inherit`() =
        runTest {
            val sessionStorage = MutableSessionStorage(authInfo(userId = "user-a", token = "token-a"))
            val client = clientAuthenticatedBy(sessionStorage, logoutFails = true)
            val service = authService(client, sessionStorage)
            // Puts token-a in the client's cache, which is what outlives the session.
            client.get<String>(SOME_ROUTE)

            service.logout("refresh-a")
            // What the sign-out does next, and what signing in as someone else does after that.
            sessionStorage.set(null)
            sessionStorage.set(authInfo(userId = "user-b", token = "token-b"))
            client.get<String>(SOME_ROUTE)

            assertEquals("Bearer token-b", authorizationHeaders.last())
        }

    @Test
    fun `a successful logout drops it too`() =
        runTest {
            val sessionStorage = MutableSessionStorage(authInfo(userId = "user-a", token = "token-a"))
            val client = clientAuthenticatedBy(sessionStorage, logoutFails = false)
            val service = authService(client, sessionStorage)
            client.get<String>(SOME_ROUTE)

            service.logout("refresh-a")
            sessionStorage.set(null)
            sessionStorage.set(authInfo(userId = "user-b", token = "token-b"))
            client.get<String>(SOME_ROUTE)

            assertEquals("Bearer token-b", authorizationHeaders.last())
        }

    private val authorizationHeaders = mutableListOf<String?>()

    private fun clientAuthenticatedBy(
        sessionStorage: SessionStorage,
        logoutFails: Boolean,
    ): HttpClient {
        val engine =
            MockEngine { request ->
                authorizationHeaders += request.headers[HttpHeaders.Authorization]
                if (logoutFails && request.url.encodedPath.endsWith("/api/auth/logout")) {
                    respondError(HttpStatusCode.ServiceUnavailable)
                } else {
                    respondOk("")
                }
            }
        return HttpClientFactory(
            tabMatesLogger = NoOpLogger(),
            sessionStorage = sessionStorage,
            json = Json { ignoreUnknownKeys = true },
            upgradeRequiredNotifier = UpgradeRequiredNotifier(),
            sessionInvalidator = NoOpSessionInvalidator(),
            environmentRepository = FixedEnvironmentRepository(),
        ).create(engine)
    }

    private class FixedEnvironmentRepository : EnvironmentRepository {
        override val default: EnvironmentConfig =
            EnvironmentConfig(
                httpBaseUrl = "https://api.example.com",
                wsBaseUrl = "wss://api.example.com/ws",
                apiKey = "test-key",
                isCustom = false,
            )

        override val config: StateFlow<EnvironmentConfig> = MutableStateFlow(default)

        override val current: EnvironmentConfig = default

        override val storedCustom: CustomEnvironment? = null

        override val isSwitchSupported: Boolean = true

        override suspend fun useCustom(environment: CustomEnvironment) = Unit

        override suspend fun useDefault() = Unit
    }

    private fun authService(
        client: HttpClient,
        sessionStorage: SessionStorage,
    ) = KtorAuthService(
        httpClient = client,
        sessionStorage = sessionStorage,
        turnstileTokenProvider = NoTurnstileTokenProvider(),
    )

    private fun authInfo(
        userId: String,
        token: String,
    ) = AuthInfo(
        accessToken = token,
        refreshToken = "refresh-$userId",
        user =
            User(
                id = userId,
                email = "$userId@example.com",
                username = userId,
                hasVerifiedEmail = true,
                userType = UserType.REGISTERED,
            ),
    )

    private class MutableSessionStorage(
        initial: AuthInfo?,
    ) : SessionStorage {
        private val state = MutableStateFlow(initial)

        override val authState: StateFlow<AuthInfo?> = state

        override fun get(): AuthInfo? = state.value

        override fun set(info: AuthInfo?) {
            state.value = info
        }
    }

    private class NoTurnstileTokenProvider : TurnstileTokenProvider {
        override suspend fun getToken(): String? = null
    }

    private class NoOpSessionInvalidator : SessionInvalidator {
        override fun invalidate(reason: SessionInvalidationReason) = Unit
    }

    private class NoOpLogger : TabMatesLogger {
        override fun debug(
            tag: String,
            message: String,
        ) = Unit

        override fun info(
            tag: String,
            message: String,
        ) = Unit

        override fun warning(
            tag: String,
            message: String,
        ) = Unit

        override fun error(
            tag: String,
            message: String,
            throwable: Throwable?,
        ) = Unit
    }

    private companion object {
        private const val SOME_ROUTE = "/api/sync"
    }
}
