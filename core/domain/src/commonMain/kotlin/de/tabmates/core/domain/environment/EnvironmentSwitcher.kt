package de.tabmates.core.domain.environment

import de.tabmates.core.domain.util.EmptyResult
import de.tabmates.core.domain.util.Error

/**
 * Points the app at another backend, and puts the device in a state where that is safe: the switch
 * is only committed once the target answered, and everything the previous environment left behind
 * (session, groups, queued writes, cached currencies) is dropped.
 *
 * Reachable from the welcome screen only, so there is never a live socket or in-flight sync to
 * tear down — signing in is what starts those, and this runs while signed out.
 */
interface EnvironmentSwitcher {
    /**
     * Validates [httpBaseUrl], probes it with [apiKey], and switches on success. A failure changes
     * nothing at all: no stored environment, no wiped data.
     */
    suspend fun useCustom(
        httpBaseUrl: String,
        apiKey: String,
    ): EmptyResult<EnvironmentSwitchError>

    /** Returns to the build-time environment. Not probed — it is the one the app shipped with. */
    suspend fun useDefault()
}

enum class EnvironmentSwitchError : Error {
    /** Not a URL, or a URL without a host. */
    INVALID_URL,

    /** Plain `http://`, which the release build cannot reach anyway (no cleartext permission). */
    INSECURE_URL,

    MISSING_API_KEY,

    /** Nothing answered: wrong host, wrong port, server down, no connectivity. */
    UNREACHABLE,

    /** The host answered but rejected the api-key. */
    KEY_REJECTED,

    /** The host answered but refuses this app version (426) — usually a foreign build token. */
    VERSION_REJECTED,
}
