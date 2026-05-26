package de.tabmates.features.tabgroup.data.network

import kotlinx.coroutines.delay
import org.koin.core.annotation.Single
import kotlin.math.pow

@Single
class ConnectionRetryHandler(
    private val connectionErrorHandler: ConnectionErrorHandler,
) {
    private var shouldSkipBackoff = false

    fun shouldRetry(cause: Throwable): Boolean = connectionErrorHandler.isRetriableError(cause)

    fun transformException(exception: Throwable): Throwable = connectionErrorHandler.transformException(exception)

    fun getConnectionStateForError(cause: Throwable): ConnectionState =
        connectionErrorHandler.getConnectionStateForError(cause)

    suspend fun applyRetryDelay(attempt: Long) {
        if (!shouldSkipBackoff) {
            val delayMs = createBackoffDelay(attempt)
            delay(delayMs)
        } else {
            shouldSkipBackoff = false
        }
    }

    fun resetDelay() {
        shouldSkipBackoff = true
    }

    private fun createBackoffDelay(attempt: Long): Long {
        val delayTime = (2f.pow(attempt.toInt()) * 2000L).toLong()
        val maxDelay = 30_000L
        return minOf(delayTime, maxDelay)
    }
}
