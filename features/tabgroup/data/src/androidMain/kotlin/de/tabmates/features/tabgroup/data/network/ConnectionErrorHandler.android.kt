package de.tabmates.features.tabgroup.data.network

import io.ktor.client.plugins.ClientRequestException
import io.ktor.client.plugins.websocket.WebSocketException
import io.ktor.network.sockets.SocketTimeoutException
import kotlinx.io.EOFException
import java.net.SocketException
import java.net.UnknownHostException
import javax.net.ssl.SSLException

actual class ConnectionErrorHandler {
    actual fun getConnectionStateForError(cause: Throwable): ConnectionState =
        when (cause) {
            is ClientRequestException,
            is WebSocketException,
            is SocketException,
            is SocketTimeoutException,
            is UnknownHostException,
            is SSLException,
            is EOFException,
            -> ConnectionState.ERROR_NETWORK

            else -> ConnectionState.ERROR_UNKNOWN
        }

    actual fun transformException(exception: Throwable): Throwable = exception

    actual fun isRetriableError(cause: Throwable): Boolean =
        when (cause) {
            is SocketTimeoutException,
            is WebSocketException,
            is SocketException,
            is UnknownHostException,
            is EOFException,
            -> true

            else -> false
        }
}
