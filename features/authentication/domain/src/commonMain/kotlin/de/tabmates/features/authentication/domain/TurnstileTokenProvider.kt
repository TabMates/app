package de.tabmates.features.authentication.domain

/**
 * Produces a Cloudflare Turnstile token for the abuse-prone auth endpoints.
 *
 * Only the web build runs a real (invisible) widget; every native target is Turnstile-exempt and
 * binds a no-op that returns `null`, so the `cf-turnstile-response` header is simply omitted there.
 */
interface TurnstileTokenProvider {
    suspend fun getToken(): String?
}
