package de.tabmates.core.data.environment

import de.tabmates.core.data.BuildKonfig
import de.tabmates.core.data.WEB_PLATFORM
import de.tabmates.core.data.clientPlatform
import de.tabmates.core.data.security.SecureStore
import de.tabmates.core.domain.environment.CustomEnvironment
import de.tabmates.core.domain.environment.EnvironmentConfig
import de.tabmates.core.domain.environment.EnvironmentRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.koin.core.annotation.Single

/**
 * Stores the custom environment encrypted (it holds an api-key) and mirrors the active
 * configuration into a [StateFlow], the same shape [de.tabmates.core.data.auth.KSafeSessionStorage]
 * uses. The initial read is synchronous so the first request of the process already goes to the
 * right host — there is no window in which the app talks to the default backend by accident.
 *
 * Web always runs on the build-time environment: its CSP `connect-src` is baked at deploy time to
 * one API origin, and it deliberately carries no api-key (the server allow-lists the Origin), so a
 * stored override could only produce requests the browser blocks.
 */
@Single(binds = [EnvironmentRepository::class])
class KSafeEnvironmentRepository(
    secureStore: SecureStore,
) : EnvironmentRepository {
    private var storedEnvironment: CustomEnvironment? by secureStore<CustomEnvironment?>(null, key = KEY_CUSTOM)
    private var customActive: Boolean by secureStore(false, key = KEY_CUSTOM_ACTIVE)

    override val default: EnvironmentConfig =
        EnvironmentConfig(
            httpBaseUrl = BuildKonfig.BASE_URL_HTTP,
            // Derived, not a second build property: the two always addressed the same host, and
            // keeping them independent only allowed a build to point HTTP and WS at different
            // backends by accident.
            wsBaseUrl = EnvironmentUrls.toWebSocketBaseUrl(BuildKonfig.BASE_URL_HTTP),
            apiKey = BuildKonfig.API_KEY,
            isCustom = false,
        )

    private val _config = MutableStateFlow(resolveInitialConfig())

    override val config: StateFlow<EnvironmentConfig> = _config.asStateFlow()

    override val current: EnvironmentConfig get() = _config.value

    override val storedCustom: CustomEnvironment?
        get() = if (isWeb) null else storedEnvironment

    override val isSwitchSupported: Boolean = !isWeb

    override suspend fun useCustom(environment: CustomEnvironment) {
        if (isWeb) return
        storedEnvironment = environment
        customActive = true
        _config.value = environment.toConfig()
    }

    /** Keeps [storedCustom] so the user can re-activate it without typing the api-key again. */
    override suspend fun useDefault() {
        customActive = false
        _config.value = default
    }

    private fun resolveInitialConfig(): EnvironmentConfig {
        // Checked before touching the delegates: on web they would only read a value that is
        // never allowed to win anyway.
        if (isWeb) return default
        if (!customActive) return default
        return storedEnvironment?.toConfig() ?: default
    }

    private fun CustomEnvironment.toConfig(): EnvironmentConfig =
        EnvironmentConfig(
            httpBaseUrl = httpBaseUrl,
            wsBaseUrl = EnvironmentUrls.toWebSocketBaseUrl(httpBaseUrl),
            apiKey = apiKey,
            isCustom = true,
        )

    private companion object {
        private val isWeb = clientPlatform == WEB_PLATFORM
        private const val KEY_CUSTOM = "customEnvironment"
        private const val KEY_CUSTOM_ACTIVE = "customEnvironmentActive"
    }
}
