@file:OptIn(ExperimentalWasmJsInterop::class)

package de.tabmates.features.authentication.data

import de.tabmates.features.authentication.domain.TurnstileTokenProvider
import kotlinx.coroutines.await
import kotlin.js.Promise

/**
 * Web Cloudflare Turnstile provider. The real Turnstile calls live in `turnstile.js` (loaded by
 * index.html); this class drives them through a thin JS glue. It renders an invisible/managed
 * widget once at startup with [BuildKonfig.TURNSTILE_SITE_KEY], then on each auth submit runs a
 * fresh (single-use) challenge and returns its token.
 *
 * Guarded against the Turnstile api.js being absent (e.g. offline PWA launch or CSP block): the JS
 * glue falls back to a null token, so [getToken] simply returns null and the auth call omits the
 * `cf-turnstile-response` header instead of throwing.
 */
class WebTurnstileTokenProvider : TurnstileTokenProvider {
    init {
        BuildKonfig.TURNSTILE_SITE_KEY?.let { turnstileInit(it) }
    }

    override suspend fun getToken(): String? = turnstileExecute().await()?.toString()
}

private fun turnstileInit(sitekey: String) {
    js("(typeof window.tabmatesTurnstileInit === 'function') && window.tabmatesTurnstileInit(sitekey)")
}

private fun turnstileExecute(): Promise<JsString?> =
    js(
        "(typeof window.tabmatesTurnstileExecute === 'function' ? window.tabmatesTurnstileExecute() : Promise.resolve(null))",
    )
