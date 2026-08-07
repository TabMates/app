package de.tabmates.features.tabgroup.data.dto

import de.tabmates.core.domain.util.DataError
import io.ktor.client.call.body
import io.ktor.client.statement.HttpResponse
import kotlinx.coroutines.CancellationException
import kotlinx.serialization.Serializable

@Serializable
internal data class RemoveParticipantErrorDto(val code: String? = null)

/**
 * Maps the two removal refusals the server states by code.
 *
 * Everything else — including the `403` a non-member gets and the `404` covering both an unknown
 * group and an unknown target — returns `null` and falls through to the generic status handling.
 * The catch tolerates non-JSON bodies; the shared `Json` already sets `ignoreUnknownKeys`. It stops
 * short of cancellation, which has to reach the caller rather than be answered with a plain
 * `BAD_REQUEST` from the generic handling below.
 */
internal suspend fun HttpResponse.removeParticipantErrorOrNull(): DataError.Remote? {
    if (status.value != 400 && status.value != 403) return null
    val code =
        try {
            body<RemoveParticipantErrorDto>().code
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            null
        }
    return when (code) {
        "CANNOT_REMOVE_SELF" -> DataError.Remote.CANNOT_REMOVE_SELF
        "CANNOT_REMOVE_GROUP_CREATOR" -> DataError.Remote.CANNOT_REMOVE_GROUP_CREATOR
        else -> null
    }
}
