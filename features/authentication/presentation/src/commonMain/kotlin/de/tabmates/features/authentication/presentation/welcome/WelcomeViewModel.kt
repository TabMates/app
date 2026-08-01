package de.tabmates.features.authentication.presentation.welcome

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import de.tabmates.core.domain.environment.EnvironmentConfig
import de.tabmates.core.domain.environment.EnvironmentRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.WhileSubscribed
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import org.koin.core.annotation.KoinViewModel
import kotlin.time.Duration.Companion.seconds

/**
 * Carries only what the welcome screen needs to know about the environment switcher: whether the
 * platform supports it, and which backend is active — a custom one is named on the button so it is
 * obvious why an account that exists on the default server suddenly does not.
 */
data class WelcomeState(
    val isEnvironmentSwitchSupported: Boolean = false,
    /** Host and port of the active custom backend, null while the default one is in use. */
    val customEnvironmentHost: String? = null,
)

@KoinViewModel
class WelcomeViewModel(
    environmentRepository: EnvironmentRepository,
) : ViewModel() {
    private val isSwitchSupported = environmentRepository.isSwitchSupported

    val state: StateFlow<WelcomeState> =
        environmentRepository.config
            .map { it.toState() }
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(5.seconds),
                initialValue = environmentRepository.current.toState(),
            )

    private fun EnvironmentConfig.toState(): WelcomeState =
        WelcomeState(
            isEnvironmentSwitchSupported = isSwitchSupported,
            // Scheme and path are dropped, the port is not: it is what tells two local backends
            // apart, while a path prefix only makes the button label too long to read.
            customEnvironmentHost =
                httpBaseUrl
                    .substringAfter("://")
                    .substringBefore('/')
                    .takeIf { isCustom },
        )
}
