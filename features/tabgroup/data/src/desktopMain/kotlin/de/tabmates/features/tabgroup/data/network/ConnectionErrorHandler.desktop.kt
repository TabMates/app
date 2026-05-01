package de.tabmates.features.tabgroup.data.network

actual class ConnectionErrorHandler {
    actual fun getConnectionStateForError(cause: Throwable): ConnectionState = ConnectionState.ERROR_NETWORK

    actual fun transformException(exception: Throwable): Throwable = exception

    actual fun isRetriableError(cause: Throwable): Boolean = true
}
