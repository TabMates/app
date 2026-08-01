package de.tabmates.core.testing.environment

import de.tabmates.core.data.environment.EnvironmentUrls
import de.tabmates.core.domain.environment.CustomEnvironment
import de.tabmates.core.domain.environment.EnvironmentConfig
import de.tabmates.core.domain.environment.EnvironmentRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

class FakeEnvironmentRepository(
    override val default: EnvironmentConfig = defaultEnvironmentConfig(),
    initial: EnvironmentConfig = default,
    override val isSwitchSupported: Boolean = true,
    /**
     * Prefilled to stand in for a custom environment the user entered earlier — what the real
     * repository keeps so the api-key does not have to be typed again.
     */
    storedCustom: CustomEnvironment? = null,
) : EnvironmentRepository {
    private val _config = MutableStateFlow(initial)

    override val config: StateFlow<EnvironmentConfig> = _config

    override val current: EnvironmentConfig get() = _config.value

    override var storedCustom: CustomEnvironment? = storedCustom
        private set

    override suspend fun useCustom(environment: CustomEnvironment) {
        storedCustom = environment
        _config.value =
            EnvironmentConfig(
                httpBaseUrl = environment.httpBaseUrl,
                wsBaseUrl = EnvironmentUrls.toWebSocketBaseUrl(environment.httpBaseUrl),
                apiKey = environment.apiKey,
                isCustom = true,
            )
    }

    override suspend fun useDefault() {
        _config.value = default
    }
}

fun defaultEnvironmentConfig(): EnvironmentConfig =
    EnvironmentConfig(
        httpBaseUrl = "https://default.example.com",
        wsBaseUrl = "wss://default.example.com/ws",
        apiKey = "default-key",
        isCustom = false,
    )

fun customEnvironmentConfig(): EnvironmentConfig =
    EnvironmentConfig(
        httpBaseUrl = "https://custom.example.com",
        wsBaseUrl = "wss://custom.example.com/ws",
        apiKey = "custom-key",
        isCustom = true,
    )
