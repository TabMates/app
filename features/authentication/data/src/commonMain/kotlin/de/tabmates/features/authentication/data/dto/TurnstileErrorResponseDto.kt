package de.tabmates.features.authentication.data.dto

import de.tabmates.core.domain.util.DataError
import io.ktor.client.call.body
import io.ktor.client.statement.HttpResponse
import kotlinx.serialization.Serializable

@Serializable
internal data class TurnstileErrorResponseDto(val code: String? = null)

/**
 * Maps the auth-only Cloudflare Turnstile rejection to [DataError.Remote.TURNSTILE_FAILED].
 *
 * Only a `403` whose body is `{ "code": "TURNSTILE_VERIFICATION_FAILED" }` qualifies. Any other
 * status, a different code, or an empty/non-JSON 403 body returns `null`, so the generic status
 * handling (`403 -> FORBIDDEN`, used by every other service) is left untouched. `runCatching`
 * tolerates non-JSON bodies; the shared `Json` already sets `ignoreUnknownKeys = true`.
 */
internal suspend fun HttpResponse.turnstileErrorOrNull(): DataError.Remote? {
    if (status.value != 403) return null
    val code = runCatching { body<TurnstileErrorResponseDto>().code }.getOrNull()
    return if (code == "TURNSTILE_VERIFICATION_FAILED") DataError.Remote.TURNSTILE_FAILED else null
}
