package de.tabmates.features.notifications.domain

import de.tabmates.core.domain.util.DataError
import de.tabmates.core.domain.util.EmptyResult

/**
 * Backend contract for associating a push-notification device token with the
 * currently authenticated user. The bearer token attached by the shared
 * [io.ktor.client.HttpClient] identifies the user server-side.
 */
interface NotificationService {
    /** Register (upsert) a device token so the backend can target this device. */
    suspend fun registerDevice(
        token: String,
        platform: DevicePlatform,
    ): EmptyResult<DataError.Remote>

    /** Remove a device token, e.g. on logout, so the backend stops targeting it. */
    suspend fun unregisterDevice(token: String): EmptyResult<DataError.Remote>
}
