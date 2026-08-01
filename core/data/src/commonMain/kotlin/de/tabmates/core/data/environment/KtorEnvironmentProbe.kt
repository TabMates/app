package de.tabmates.core.data.environment

import de.tabmates.core.data.AppBuildInfo
import de.tabmates.core.data.clientPlatform
import de.tabmates.core.data.networking.platformSafeCall
import de.tabmates.core.data.networking.responseToResult
import de.tabmates.core.domain.environment.EnvironmentSwitchError
import de.tabmates.core.domain.util.DataError
import de.tabmates.core.domain.util.EmptyResult
import de.tabmates.core.domain.util.Result
import io.ktor.client.HttpClient
import io.ktor.client.engine.HttpClientEngine
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.request.parameter
import io.ktor.http.isSuccess
import org.koin.core.annotation.Single

/**
 * Runs the probe on its own short-lived client rather than the shared one, for two reasons. The
 * shared client stamps the *current* environment's api-key onto every request, so what it sends is
 * not the request the server would see after the switch. More importantly its `Auth` plugin would
 * attach the signed-in user's bearer — handing a live token to a host the user has only just typed
 * and that has proven nothing yet. Overriding a header cannot take that back.
 */
@Single(binds = [EnvironmentProbe::class])
class KtorEnvironmentProbe(
    private val engine: HttpClientEngine,
) : EnvironmentProbe {
    override suspend fun probe(
        httpBaseUrl: String,
        apiKey: String,
    ): EmptyResult<EnvironmentSwitchError> {
        // Passing an existing engine leaves it under its owner's control, so closing this client
        // does not take the app's shared client down with it.
        val client =
            HttpClient(engine) {
                install(HttpTimeout) {
                    socketTimeoutMillis = PROBE_TIMEOUT_MS
                    requestTimeoutMillis = PROBE_TIMEOUT_MS
                }
            }

        val result =
            try {
                platformSafeCall(
                    execute = {
                        client.get("$httpBaseUrl$ROUTE") {
                            parameter("platform", clientPlatform)
                            header("x-api-key", apiKey)
                            header("X-Client-Version", AppBuildInfo.clientVersionHeader)
                            AppBuildInfo.buildToken?.let { header("X-Client-Token", it) }
                        }
                    },
                    // Only the status matters: the body is this build's update info, and decoding
                    // it would need a content negotiator this client deliberately does not have.
                    handleResponse = { response ->
                        if (response.status.isSuccess()) {
                            Result.Success(Unit)
                        } else {
                            responseToResult<Unit>(response)
                        }
                    },
                )
            } finally {
                client.close()
            }

        return when (result) {
            is Result.Success -> Result.Success(Unit)
            is Result.Failure -> Result.Failure(result.error.toSwitchError())
        }
    }

    private fun DataError.Remote.toSwitchError(): EnvironmentSwitchError =
        when (this) {
            DataError.Remote.UNAUTHORIZED, DataError.Remote.FORBIDDEN -> EnvironmentSwitchError.KEY_REJECTED

            // The gate answers 426 to a client it does not recognize — typically a build token
            // minted from a different secret than this backend validates against.
            DataError.Remote.UPGRADE_REQUIRED -> EnvironmentSwitchError.VERSION_REJECTED

            else -> EnvironmentSwitchError.UNREACHABLE
        }

    private companion object {
        private const val ROUTE = "/api/app-version"
        private const val PROBE_TIMEOUT_MS = 10_000L
    }
}
