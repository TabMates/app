package de.tabmates.core.data.environment

import de.tabmates.core.data.AppBuildInfo
import de.tabmates.core.data.clientPlatform
import de.tabmates.core.domain.environment.EnvironmentSwitchError
import de.tabmates.core.domain.util.Result
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.client.engine.mock.respondError
import io.ktor.client.request.HttpRequestData
import io.ktor.http.HttpStatusCode
import io.ktor.http.isSuccess
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class KtorEnvironmentProbeTest {
    private val requests = mutableListOf<HttpRequestData>()

    private fun probeAgainst(status: HttpStatusCode): KtorEnvironmentProbe =
        KtorEnvironmentProbe(
            MockEngine { request ->
                requests += request
                if (status.isSuccess()) {
                    respond(content = "{}", status = status)
                } else {
                    respondError(status)
                }
            },
        )

    @Test
    fun `a reachable server accepting the key succeeds`() =
        runTest {
            val result = probeAgainst(HttpStatusCode.OK).probe("https://api.example.com", "key")

            assertTrue(result is Result.Success)
        }

    @Test
    fun `the probe asks the candidate host with the entered key`() =
        runTest {
            probeAgainst(HttpStatusCode.OK).probe("https://api.example.com", "custom-key")

            val request = requests.single()
            assertEquals(
                "https://api.example.com/api/app-version?platform=$clientPlatform",
                request.url.toString(),
            )
            assertEquals("custom-key", request.headers["x-api-key"])
            assertEquals(AppBuildInfo.clientVersionHeader, request.headers["X-Client-Version"])
        }

    @Test
    fun `a rejected key is reported as such rather than as unreachable`() =
        runTest {
            val result = probeAgainst(HttpStatusCode.Unauthorized).probe("https://api.example.com", "key")

            assertEquals(EnvironmentSwitchError.KEY_REJECTED, (result as Result.Failure).error)
        }

    @Test
    fun `a version gate rejection is reported as such`() =
        runTest {
            val result = probeAgainst(HttpStatusCode.UpgradeRequired).probe("https://api.example.com", "key")

            assertEquals(EnvironmentSwitchError.VERSION_REJECTED, (result as Result.Failure).error)
        }

    @Test
    fun `any other failure is unreachable`() =
        runTest {
            val result = probeAgainst(HttpStatusCode.InternalServerError).probe("https://api.example.com", "key")

            assertEquals(EnvironmentSwitchError.UNREACHABLE, (result as Result.Failure).error)
        }
}
