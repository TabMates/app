package de.tabmates.core.data.environment

import de.tabmates.core.domain.environment.EnvironmentSwitchError
import de.tabmates.core.domain.util.Result
import io.ktor.http.URLBuilder
import io.ktor.http.Url

/**
 * Turns the one URL the user types into the two the app needs.
 *
 * Kept as pure functions (no storage, no DI) so every input shape — ports, paths, trailing
 * slashes, junk — is covered by plain unit tests.
 */
object EnvironmentUrls {
    private const val WS_PATH_SUFFIX = "/ws"
    private const val HTTPS_SCHEME = "https://"
    private const val HTTP_SCHEME = "http://"
    private const val WSS_SCHEME = "wss://"
    private const val WS_SCHEME = "ws://"

    /**
     * Validates and canonicalizes what the user typed: trims, drops a trailing slash, and strips
     * query/fragment so a pasted link cannot smuggle parameters into every request.
     *
     * The scheme has to be spelled out — guessing one would silently downgrade a typo to `http` —
     * and it has to be `https`: the Android release build permits no cleartext traffic, so a
     * `http://` environment would be accepted here and then fail every request.
     */
    fun normalizeHttpBaseUrl(raw: String): Result<String, EnvironmentSwitchError> {
        val trimmed = raw.trim().trimEnd('/')
        if (trimmed.isBlank()) return Result.Failure(EnvironmentSwitchError.INVALID_URL)

        val isHttps = trimmed.startsWith(HTTPS_SCHEME, ignoreCase = true)
        val isHttp = trimmed.startsWith(HTTP_SCHEME, ignoreCase = true)
        if (isHttp) return Result.Failure(EnvironmentSwitchError.INSECURE_URL)
        if (!isHttps) return Result.Failure(EnvironmentSwitchError.INVALID_URL)

        val url =
            runCatching { Url(trimmed) }.getOrNull() ?: return Result.Failure(EnvironmentSwitchError.INVALID_URL)
        if (url.host.isBlank()) return Result.Failure(EnvironmentSwitchError.INVALID_URL)

        val normalized =
            URLBuilder(url)
                .apply {
                    parameters.clear()
                    fragment = ""
                }.buildString()
                .trimEnd('/')
        return Result.Success(normalized)
    }

    /**
     * `https://host` → `wss://host/ws`, `http://host:8080` → `ws://host:8080/ws`.
     *
     * The single source of the websocket base URL: there is no `BASE_URL_WS` build property, so
     * this derives the built-in environment's socket host from `BASE_URL_HTTP` as well as the
     * custom one's from what the user typed. `WebSocketTransport` appends `/group` to the result,
     * the desktop notification stream `/api/notifications/stream`.
     *
     * A plain scheme swap rather than URL parsing, because it also runs on the raw build property:
     * an unset or malformed `BASE_URL_HTTP` must not throw while the DI graph is being built (the
     * request it produces fails on its own, which is the same outcome an unset URL had before).
     */
    fun toWebSocketBaseUrl(httpBaseUrl: String): String {
        val trimmed = httpBaseUrl.trim().trimEnd('/')
        val wsUrl =
            when {
                trimmed.startsWith(HTTPS_SCHEME, true) -> WSS_SCHEME + trimmed.drop(HTTPS_SCHEME.length)
                trimmed.startsWith(HTTP_SCHEME, true) -> WS_SCHEME + trimmed.drop(HTTP_SCHEME.length)
                else -> trimmed
            }
        return wsUrl + WS_PATH_SUFFIX
    }
}
