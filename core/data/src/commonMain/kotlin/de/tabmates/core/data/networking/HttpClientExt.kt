package de.tabmates.core.data.networking

import de.tabmates.core.domain.util.DataError
import de.tabmates.core.domain.util.Result
import io.ktor.client.HttpClient
import io.ktor.client.call.NoTransformationFoundException
import io.ktor.client.call.body
import io.ktor.client.request.HttpRequestBuilder
import io.ktor.client.request.delete
import io.ktor.client.request.get
import io.ktor.client.request.parameter
import io.ktor.client.request.patch
import io.ktor.client.request.post
import io.ktor.client.request.put
import io.ktor.client.request.setBody
import io.ktor.client.request.url
import io.ktor.client.statement.HttpResponse

expect suspend fun <T> platformSafeCall(
    execute: suspend () -> HttpResponse,
    handleResponse: suspend (HttpResponse) -> Result<T, DataError.Remote>,
): Result<T, DataError.Remote>

suspend inline fun <reified Request, reified Response : Any> HttpClient.post(
    route: String,
    body: Request,
    queryParams: Map<String, Any> = mapOf(),
    // Optional per-call error mapper inspected before the generic status handling. When it
    // returns non-null the call short-circuits to that failure (e.g. the auth turnstile 403,
    // which needs the response body to be distinguished from a plain FORBIDDEN).
    noinline mapKnownError: (suspend (HttpResponse) -> DataError.Remote?)? = null,
    crossinline builder: HttpRequestBuilder.() -> Unit = {},
): Result<Response, DataError.Remote> {
    return safeCall(mapKnownError = mapKnownError) {
        post {
            url(routeForRequest(route))
            queryParams.forEach { (key, value) ->
                parameter(key, value)
            }
            setBody(body)
            builder()
        }
    }
}

suspend inline fun <reified Response : Any> HttpClient.get(
    route: String,
    queryParams: Map<String, Any> = mapOf(),
    crossinline builder: HttpRequestBuilder.() -> Unit = {},
): Result<Response, DataError.Remote> {
    return safeCall {
        get {
            url(routeForRequest(route))
            queryParams.forEach { (key, value) ->
                parameter(key, value)
            }
            builder()
        }
    }
}

suspend inline fun <reified Response : Any> HttpClient.delete(
    route: String,
    queryParams: Map<String, Any> = mapOf(),
    // See [post]: inspected before the generic status handling, for calls whose failures can only
    // be told apart by the response body (e.g. removing a group participant).
    noinline mapKnownError: (suspend (HttpResponse) -> DataError.Remote?)? = null,
    crossinline builder: HttpRequestBuilder.() -> Unit = {},
): Result<Response, DataError.Remote> {
    return safeCall(mapKnownError = mapKnownError) {
        delete {
            url(routeForRequest(route))
            queryParams.forEach { (key, value) ->
                parameter(key, value)
            }
            builder()
        }
    }
}

suspend inline fun <reified Request, reified Response : Any> HttpClient.put(
    route: String,
    queryParams: Map<String, Any> = mapOf(),
    body: Request,
    crossinline builder: HttpRequestBuilder.() -> Unit = {},
): Result<Response, DataError.Remote> {
    return safeCall {
        put {
            url(routeForRequest(route))
            queryParams.forEach { (key, value) ->
                parameter(key, value)
            }
            setBody(body)
            builder()
        }
    }
}

suspend inline fun <reified Request, reified Response : Any> HttpClient.patch(
    route: String,
    body: Request,
    queryParams: Map<String, Any> = mapOf(),
    // See post: per-call error mapper inspected before the generic status handling.
    noinline mapKnownError: (suspend (HttpResponse) -> DataError.Remote?)? = null,
    crossinline builder: HttpRequestBuilder.() -> Unit = {},
): Result<Response, DataError.Remote> {
    return safeCall(mapKnownError = mapKnownError) {
        patch {
            url(routeForRequest(route))
            queryParams.forEach { (key, value) ->
                parameter(key, value)
            }
            setBody(body)
            builder()
        }
    }
}

suspend inline fun <reified T> safeCall(
    noinline mapKnownError: (suspend (HttpResponse) -> DataError.Remote?)? = null,
    noinline execute: suspend () -> HttpResponse,
): Result<T, DataError.Remote> {
    return platformSafeCall(
        execute = execute,
    ) { response ->
        val knownError = mapKnownError?.invoke(response)
        if (knownError != null) {
            Result.Failure(knownError)
        } else {
            responseToResult(response)
        }
    }
}

suspend inline fun <reified T> responseToResult(response: HttpResponse): Result<T, DataError.Remote> {
    return when (response.status.value) {
        in 200..299 -> {
            try {
                Result.Success(response.body<T>())
            } catch (e: NoTransformationFoundException) {
                Result.Failure(DataError.Remote.SERIALIZATION)
            }
        }

        400 -> {
            Result.Failure(DataError.Remote.BAD_REQUEST)
        }

        401 -> {
            Result.Failure(DataError.Remote.UNAUTHORIZED)
        }

        403 -> {
            Result.Failure(DataError.Remote.FORBIDDEN)
        }

        404 -> {
            Result.Failure(DataError.Remote.NOT_FOUND)
        }

        408 -> {
            Result.Failure(DataError.Remote.REQUEST_TIMEOUT)
        }

        409 -> {
            Result.Failure(DataError.Remote.CONFLICT)
        }

        413 -> {
            Result.Failure(DataError.Remote.PAYLOAD_TOO_LARGE)
        }

        426 -> {
            Result.Failure(DataError.Remote.UPGRADE_REQUIRED)
        }

        429 -> {
            Result.Failure(DataError.Remote.TOO_MANY_REQUESTS)
        }

        500 -> {
            Result.Failure(DataError.Remote.SERVER_ERROR)
        }

        503 -> {
            Result.Failure(DataError.Remote.SERVICE_UNAVAILABLE)
        }

        else -> {
            Result.Failure(DataError.Remote.UNKNOWN)
        }
    }
}

/**
 * Prepares a route for [io.ktor.client.request.HttpRequestBuilder.url], which treats it one of two
 * ways:
 *
 * - A path (`/api/groups`, `api/groups`) has its leading slash stripped so it stays *relative* and
 *   Ktor's `DefaultRequest` resolves it against the active environment's base URL (see
 *   [HttpClientFactory]) — that indirection is what lets the backend be switched at runtime
 *   without rebuilding the client. A leading slash would resolve against the host root instead and
 *   drop any path prefix the base URL carries.
 * - A route that already carries a scheme and host is passed through unchanged: an absolute URL
 *   always wins over the default, which is what a caller asking for a foreign host wants.
 */
@PublishedApi
internal fun routeForRequest(route: String): String = route.removePrefix("/")
