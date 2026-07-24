package de.tabmates.features.authentication.data

import de.tabmates.features.authentication.domain.TurnstileTokenProvider

/**
 * No token on Turnstile-exempt platforms (Android, iOS, Desktop): [getToken] returns `null`, so the
 * `cf-turnstile-response` header is simply omitted on those targets.
 */
class NoOpTurnstileTokenProvider : TurnstileTokenProvider {
    override suspend fun getToken(): String? = null
}
