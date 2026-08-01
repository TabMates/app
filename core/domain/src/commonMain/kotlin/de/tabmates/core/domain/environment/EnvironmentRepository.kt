package de.tabmates.core.domain.environment

import kotlinx.coroutines.flow.StateFlow

/**
 * The single source of truth for which backend the app is pointed at.
 *
 * Every runtime read of the API host / WebSocket host / api-key goes through here rather than
 * through `BuildKonfig`, so a switch takes effect on the next request without a restart. The
 * stored value is read synchronously when the repository is built: the first request must already
 * go to the right host.
 */
interface EnvironmentRepository {
    /** The active configuration. Synchronous because it is read on the request hot path. */
    val current: EnvironmentConfig

    val config: StateFlow<EnvironmentConfig>

    /** The build-time environment, restored by [useDefault]. */
    val default: EnvironmentConfig

    /** The last custom environment entered, kept after switching back so it can be re-activated. */
    val storedCustom: CustomEnvironment?

    /**
     * Whether this platform can be pointed at another backend at all. False on web, where the page
     * may only talk to the origins its deploy-time CSP allows — the switcher is hidden there.
     */
    val isSwitchSupported: Boolean

    suspend fun useCustom(environment: CustomEnvironment)

    suspend fun useDefault()
}
