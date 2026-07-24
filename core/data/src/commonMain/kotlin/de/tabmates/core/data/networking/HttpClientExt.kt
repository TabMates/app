package de.tabmates.core.data.networking

import de.tabmates.core.data.BuildKonfig
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
            url(constructRoute(route))
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
            url(constructRoute(route))
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
    crossinline builder: HttpRequestBuilder.() -> Unit = {},
): Result<Response, DataError.Remote> {
    return safeCall {
        delete {
            url(constructRoute(route))
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
            url(constructRoute(route))
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
    crossinline builder: HttpRequestBuilder.() -> Unit = {},
): Result<Response, DataError.Remote> {
    return safeCall {
        patch {
            url(constructRoute(route))
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

fun constructRoute(route: String): String {
    return when {
        route.contains(BuildKonfig.BASE_URL_HTTP) -> route
        route.startsWith("/") -> "${BuildKonfig.BASE_URL_HTTP}$route"
        else -> "${BuildKonfig.BASE_URL_HTTP}/$route"
    }
}
