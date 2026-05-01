package de.tabmates.features.tabgroup.data.network

expect class ConnectionErrorHandler {
    fun getConnectionStateForError(cause: Throwable): ConnectionState

    fun transformException(exception: Throwable): Throwable

    fun isRetriableError(cause: Throwable): Boolean
}
