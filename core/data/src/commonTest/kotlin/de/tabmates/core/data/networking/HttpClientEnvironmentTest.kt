package de.tabmates.core.data.networking

import de.tabmates.core.domain.auth.AuthInfo
import de.tabmates.core.domain.auth.SessionInvalidationReason
import de.tabmates.core.domain.auth.SessionInvalidator
import de.tabmates.core.domain.auth.SessionStorage
import de.tabmates.core.domain.environment.CustomEnvironment
import de.tabmates.core.domain.environment.EnvironmentConfig
import de.tabmates.core.domain.logging.TabMatesLogger
import de.tabmates.core.domain.update.UpgradeRequiredNotifier
import de.tabmates.core.testing.environment.FakeEnvironmentRepository
import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.client.request.HttpRequestData
import io.ktor.http.HttpStatusCode
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.Json
import kotlin.test.Test
import kotlin.test.assertEquals

/**
 * Guards the indirection the environment switch rests on: routes are passed relative and resolved
 * against whatever base URL the environment repository currently reports.
 */
class HttpClientEnvironmentTest {
    private val requests = mutableListOf<HttpRequestData>()

    private fun clientFor(environmentRepository: FakeEnvironmentRepository): HttpClient =
        HttpClientFactory(
            tabMatesLogger = NoOpLogger(),
            sessionStorage = EmptySessionStorage(),
            json = Json { ignoreUnknownKeys = true },
            upgradeRequiredNotifier = UpgradeRequiredNotifier(),
            sessionInvalidator = NoOpSessionInvalidator(),
            environmentRepository = environmentRepository,
        ).create(
            MockEngine { request ->
                requests += request
                respond(content = "ok", status = HttpStatusCode.OK)
            },
        )

    @Test
    fun `route resolves against the active base url`() =
        runTest {
            val client = clientFor(FakeEnvironmentRepository())

            client.get<String>("/api/sync")

            assertEquals("https://default.example.com/api/sync", requests.single().url.toString())
        }

    @Test
    fun `query parameters survive the resolution`() =
        runTest {
            val client = clientFor(FakeEnvironmentRepository())

            client.get<String>("/api/app-version", mapOf("platform" to "android"))

            assertEquals(
                "https://default.example.com/api/app-version?platform=android",
                requests.single().url.toString(),
            )
        }

    @Test
    fun `a base url with a path keeps that path`() =
        runTest {
            val client =
                clientFor(
                    FakeEnvironmentRepository(
                        default =
                            EnvironmentConfig(
                                httpBaseUrl = "https://default.example.com/gateway",
                                wsBaseUrl = "wss://default.example.com/gateway/ws",
                                apiKey = "default-key",
                                isCustom = false,
                            ),
                    ),
                )

            client.get<String>("/api/sync")

            assertEquals("https://default.example.com/gateway/api/sync", requests.single().url.toString())
        }

    @Test
    fun `an absolute route is left untouched`() =
        runTest {
            val client = clientFor(FakeEnvironmentRepository())

            client.get<String>("https://other.example.com/api/sync")

            assertEquals("https://other.example.com/api/sync", requests.single().url.toString())
        }

    @Test
    fun `switching the environment moves the next request without rebuilding the client`() =
        runTest {
            val environmentRepository = FakeEnvironmentRepository()
            val client = clientFor(environmentRepository)
            client.get<String>("/api/sync")

            environmentRepository.useCustom(
                CustomEnvironment(httpBaseUrl = "https://custom.example.com", apiKey = "custom-key"),
            )
            client.get<String>("/api/sync")

            assertEquals("https://default.example.com/api/sync", requests.first().url.toString())
            assertEquals("https://custom.example.com/api/sync", requests.last().url.toString())
            assertEquals("default-key", requests.first().headers["x-api-key"])
            assertEquals("custom-key", requests.last().headers["x-api-key"])
        }

    @Test
    fun `an environment without an api key sends no api key header`() =
        runTest {
            val client =
                clientFor(
                    FakeEnvironmentRepository(
                        default =
                            EnvironmentConfig(
                                httpBaseUrl = "https://default.example.com",
                                wsBaseUrl = "wss://default.example.com/ws",
                                apiKey = null,
                                isCustom = false,
                            ),
                    ),
                )

            client.get<String>("/api/sync")

            assertEquals(null, requests.single().headers["x-api-key"])
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

    private class EmptySessionStorage : SessionStorage {
        override val authState: StateFlow<AuthInfo?> = MutableStateFlow(null)

        override fun get(): AuthInfo? = null

        override fun set(info: AuthInfo?) = Unit
    }

    private class NoOpSessionInvalidator : SessionInvalidator {
        override fun invalidate(reason: SessionInvalidationReason) = Unit
    }
}
