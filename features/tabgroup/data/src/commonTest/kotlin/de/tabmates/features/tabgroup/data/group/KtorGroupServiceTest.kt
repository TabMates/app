package de.tabmates.features.tabgroup.data.group

import de.tabmates.core.data.networking.HttpClientFactory
import de.tabmates.core.domain.auth.AuthInfo
import de.tabmates.core.domain.auth.SessionInvalidationReason
import de.tabmates.core.domain.auth.SessionInvalidator
import de.tabmates.core.domain.auth.SessionStorage
import de.tabmates.core.domain.environment.CustomEnvironment
import de.tabmates.core.domain.environment.EnvironmentConfig
import de.tabmates.core.domain.environment.EnvironmentRepository
import de.tabmates.core.domain.logging.TabMatesLogger
import de.tabmates.core.domain.update.UpgradeRequiredNotifier
import de.tabmates.core.domain.util.DataError
import de.tabmates.core.domain.util.Result
import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.client.request.HttpRequestData
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpMethod
import io.ktor.http.HttpStatusCode
import io.ktor.http.headersOf
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.Json
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs

/**
 * Wire-level guard for `DELETE /api/group/{groupId}/participants/{userId}`. The server states two
 * of its refusals by code in the body, and both share a status with a refusal that means something
 * else — so reading the body is the only thing keeping them apart.
 */
class KtorGroupServiceTest {
    @Test
    fun `removing a participant calls the endpoint and succeeds on an empty body`() =
        runTest {
            val service = groupService(clientResponding(body = "", status = HttpStatusCode.OK))

            val result = service.removeParticipant(groupId = "g1", userId = "user-2")

            assertIs<Result.Success<Unit>>(result)
            val request = requests.single()
            assertEquals(HttpMethod.Delete, request.method)
            assertEquals("/api/group/g1/participants/user-2", request.url.encodedPath)
        }

    @Test
    fun `a coded 403 maps to the creator refusal`() =
        runTest {
            val service =
                groupService(
                    clientResponding(
                        body = """{"code":"CANNOT_REMOVE_GROUP_CREATOR"}""",
                        status = HttpStatusCode.Forbidden,
                    ),
                )

            val result = service.removeParticipant(groupId = "g1", userId = "creator")

            assertEquals(DataError.Remote.CANNOT_REMOVE_GROUP_CREATOR, failureOf(result))
        }

    @Test
    fun `a coded 400 maps to the self refusal`() =
        runTest {
            val service =
                groupService(
                    clientResponding(
                        body = """{"code":"CANNOT_REMOVE_SELF"}""",
                        status = HttpStatusCode.BadRequest,
                    ),
                )

            val result = service.removeParticipant(groupId = "g1", userId = "me")

            assertEquals(DataError.Remote.CANNOT_REMOVE_SELF, failureOf(result))
        }

    @Test
    fun `a 403 without a code still means forbidden`() =
        runTest {
            val service = groupService(clientResponding(body = "", status = HttpStatusCode.Forbidden))

            val result = service.removeParticipant(groupId = "g1", userId = "user-2")

            assertEquals(DataError.Remote.FORBIDDEN, failureOf(result))
        }

    // The server folds an unknown group and an unknown target into the same coded 404.
    @Test
    fun `a 404 stays generic`() =
        runTest {
            val service =
                groupService(
                    clientResponding(body = """{"code":"NOT_FOUND"}""", status = HttpStatusCode.NotFound),
                )

            val result = service.removeParticipant(groupId = "g1", userId = "user-2")

            assertEquals(DataError.Remote.NOT_FOUND, failureOf(result))
        }

    private val requests = mutableListOf<HttpRequestData>()

    private fun failureOf(result: Result<*, DataError.Remote>): DataError.Remote =
        assertIs<Result.Failure<DataError.Remote>>(result).error

    private fun groupService(client: HttpClient) = KtorGroupService(httpClient = client)

    private fun clientResponding(
        body: String,
        status: HttpStatusCode,
    ): HttpClient {
        val engine =
            MockEngine { request ->
                requests += request
                respond(
                    content = body,
                    status = status,
                    headers = headersOf(HttpHeaders.ContentType, ContentType.Application.Json.toString()),
                )
            }
        return HttpClientFactory(
            tabMatesLogger = NoOpLogger(),
            sessionStorage = EmptySessionStorage(),
            json = Json { ignoreUnknownKeys = true },
            upgradeRequiredNotifier = UpgradeRequiredNotifier(),
            sessionInvalidator = NoOpSessionInvalidator(),
            environmentRepository = FixedEnvironmentRepository(),
        ).create(engine)
    }

    private class EmptySessionStorage : SessionStorage {
        override val authState: StateFlow<AuthInfo?> = MutableStateFlow(null)

        override fun get(): AuthInfo? = null

        override fun set(info: AuthInfo?) = Unit
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
}
