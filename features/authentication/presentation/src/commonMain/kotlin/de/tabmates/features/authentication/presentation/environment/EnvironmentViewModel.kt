package de.tabmates.features.authentication.presentation.environment

import androidx.compose.foundation.text.input.TextFieldState
import androidx.compose.runtime.snapshotFlow
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import de.tabmates.core.domain.environment.EnvironmentRepository
import de.tabmates.core.domain.environment.EnvironmentSwitchError
import de.tabmates.core.domain.environment.EnvironmentSwitcher
import de.tabmates.core.domain.util.onFailure
import de.tabmates.core.domain.util.onSuccess
import de.tabmates.core.presentation.util.UiText
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.WhileSubscribed
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import org.koin.core.annotation.KoinViewModel
import tabmatesapp.features.authentication.presentation.generated.resources.Res
import tabmatesapp.features.authentication.presentation.generated.resources.environment_error_insecure_url
import tabmatesapp.features.authentication.presentation.generated.resources.environment_error_invalid_url
import tabmatesapp.features.authentication.presentation.generated.resources.environment_error_key_rejected
import tabmatesapp.features.authentication.presentation.generated.resources.environment_error_missing_api_key
import tabmatesapp.features.authentication.presentation.generated.resources.environment_error_unreachable
import tabmatesapp.features.authentication.presentation.generated.resources.environment_error_version_rejected
import kotlin.time.Duration.Companion.seconds

@KoinViewModel
class EnvironmentViewModel(
    private val environmentSwitcher: EnvironmentSwitcher,
    private val environmentRepository: EnvironmentRepository,
) : ViewModel() {
    private var hasLoadedInitialData = false

    private val _state =
        MutableStateFlow(
            // Prefilled from the last custom environment — including the api-key, which the user
            // would otherwise have to dig out again every time they switch back and forth.
            EnvironmentState(
                urlTextFieldState = TextFieldState(environmentRepository.storedCustom?.httpBaseUrl.orEmpty()),
                apiKeyTextFieldState = TextFieldState(environmentRepository.storedCustom?.apiKey.orEmpty()),
                isCustomActive = environmentRepository.current.isCustom,
                defaultUrl = environmentRepository.default.httpBaseUrl,
                storedCustomUrl = environmentRepository.storedCustom?.httpBaseUrl,
                // Opens on the row that is live, so the screen answers "which server?" before
                // asking the user to choose one.
                selectedMode =
                    if (environmentRepository.current.isCustom) {
                        EnvironmentMode.CUSTOM
                    } else {
                        EnvironmentMode.DEFAULT
                    },
            ),
        )

    val state =
        _state
            .onStart {
                if (!hasLoadedInitialData) {
                    observeActiveEnvironment()
                    observeInput()
                    hasLoadedInitialData = true
                }
            }.stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(5.seconds),
                initialValue = _state.value,
            )

    private val eventChannel = Channel<EnvironmentEvent>()
    val events = eventChannel.receiveAsFlow()

    private fun observeActiveEnvironment() {
        environmentRepository.config
            .onEach { config ->
                _state.update {
                    it.copy(
                        isCustomActive = config.isCustom,
                        // Keep the custom row's subtitle honest when a switch lands.
                        storedCustomUrl = if (config.isCustom) config.httpBaseUrl else it.storedCustomUrl,
                    )
                }
            }.launchIn(viewModelScope)
    }

    private fun observeInput() {
        val hasUrl =
            snapshotFlow {
                _state.value.urlTextFieldState.text
                    .toString()
                    .trim()
            }.map { it.isNotBlank() }
                .distinctUntilChanged()
        val hasApiKey =
            snapshotFlow {
                _state.value.apiKeyTextFieldState.text
                    .toString()
                    .trim()
            }.map { it.isNotBlank() }
                .distinctUntilChanged()
        val isApplying = _state.map { it.isApplying }.distinctUntilChanged()

        combine(hasUrl, hasApiKey, isApplying) { url, apiKey, applying -> url && apiKey && !applying }
            .distinctUntilChanged()
            .onEach { canApply -> _state.update { it.copy(canApply = canApply) } }
            .launchIn(viewModelScope)
    }

    fun onModeSelected(mode: EnvironmentMode) {
        if (_state.value.isApplying || _state.value.selectedMode == mode) return

        // The form collapses with the pick, and an error left under a hidden field is noise.
        _state.update { it.copy(selectedMode = mode, urlError = null, apiKeyError = null) }
    }

    fun onToggleApiKeyVisibility() {
        _state.update { it.copy(isApiKeyVisible = !it.isApiKeyVisible) }
    }

    /** One button for both rows — the pick decides which switch it performs. */
    fun onSubmit() {
        when (_state.value.selectedMode) {
            EnvironmentMode.CUSTOM -> onApplyCustom()
            EnvironmentMode.DEFAULT -> onUseDefault()
        }
    }

    fun onApplyCustom() {
        if (!_state.value.canApply) return

        viewModelScope.launch {
            _state.update { it.copy(isApplying = true, urlError = null, apiKeyError = null) }

            val url =
                _state.value.urlTextFieldState.text
                    .toString()
                    .trim()
            val apiKey =
                _state.value.apiKeyTextFieldState.text
                    .toString()
                    .trim()

            environmentSwitcher
                .useCustom(httpBaseUrl = url, apiKey = apiKey)
                .onSuccess {
                    _state.update { it.copy(isApplying = false) }
                    eventChannel.send(EnvironmentEvent.Switched)
                }.onFailure { error ->
                    val message = error.toUiText()
                    val blamesApiKey = error.blamesApiKey
                    _state.update {
                        it.copy(
                            isApplying = false,
                            urlError = if (blamesApiKey) null else message,
                            apiKeyError = if (blamesApiKey) message else null,
                        )
                    }
                }
        }
    }

    fun onUseDefault() {
        if (_state.value.isApplying || !_state.value.isCustomActive) return

        viewModelScope.launch {
            _state.update { it.copy(isApplying = true, urlError = null, apiKeyError = null) }
            environmentSwitcher.useDefault()
            _state.update { it.copy(isApplying = false) }
            eventChannel.send(EnvironmentEvent.Switched)
        }
    }

    /**
     * Which field the user has to change. Only a key problem is the api-key's fault — everything
     * else (bad, insecure, unreachable URL, or a server that refuses this build) is about the host
     * they typed.
     */
    private val EnvironmentSwitchError.blamesApiKey: Boolean
        get() =
            this == EnvironmentSwitchError.MISSING_API_KEY ||
                this == EnvironmentSwitchError.KEY_REJECTED

    private fun EnvironmentSwitchError.toUiText(): UiText =
        UiText.Resource(
            when (this) {
                EnvironmentSwitchError.INVALID_URL -> Res.string.environment_error_invalid_url
                EnvironmentSwitchError.INSECURE_URL -> Res.string.environment_error_insecure_url
                EnvironmentSwitchError.MISSING_API_KEY -> Res.string.environment_error_missing_api_key
                EnvironmentSwitchError.UNREACHABLE -> Res.string.environment_error_unreachable
                EnvironmentSwitchError.KEY_REJECTED -> Res.string.environment_error_key_rejected
                EnvironmentSwitchError.VERSION_REJECTED -> Res.string.environment_error_version_rejected
            },
        )
}
