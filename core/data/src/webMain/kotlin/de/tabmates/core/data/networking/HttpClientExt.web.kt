package de.tabmates.core.data.networking

import de.tabmates.core.domain.util.DataError
import de.tabmates.core.domain.util.Result
import io.ktor.client.plugins.HttpRequestTimeoutException
import io.ktor.client.statement.HttpResponse
import io.ktor.util.network.UnresolvedAddressException
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.serialization.SerializationException

actual suspend fun <T> platformSafeCall(
    execute: suspend () -> HttpResponse,
    handleResponse: suspend (HttpResponse) -> Result<T, DataError.Remote>,
): Result<T, DataError.Remote> {
    return try {
        val response = execute()
        handleResponse(response)
    } catch (e: UnresolvedAddressException) {
        Result.Failure(DataError.Remote.NO_INTERNET)
    } catch (e: HttpRequestTimeoutException) {
        Result.Failure(DataError.Remote.REQUEST_TIMEOUT)
    } catch (e: SerializationException) {
        Result.Failure(DataError.Remote.SERIALIZATION)
    } catch (e: Exception) {
        currentCoroutineContext().ensureActive()
        if (e.isNetworkError()) {
            Result.Failure(DataError.Remote.NO_INTERNET)
        } else {
            Result.Failure(DataError.Remote.UNKNOWN)
        }
    }
}

/**
 * On JS/WASM, the Ktor engine surfaces network-unavailable and DNS failures as a plain
 * [Exception] whose message (or its cause's message) contains browser-level fetch error
 * strings rather than typed platform exceptions (e.g. UnknownHostException on JVM).
 */
private fun Exception.isNetworkError(): Boolean {
    val messages =
        buildList {
            var current: Throwable? = this@isNetworkError
            while (current != null) {
                current.message?.let { add(it) }
                current = current.cause
            }
        }
    return messages.any { msg ->
        msg.contains("Failed to fetch", ignoreCase = true) ||
            msg.contains("NetworkError", ignoreCase = true) ||
            msg.contains("Network request failed", ignoreCase = true) ||
            msg.contains("Load failed", ignoreCase = true)
    }
}
