package de.tabmates.features.authentication.presentation.registerguest

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import de.tabmates.core.domain.util.DataError
import de.tabmates.core.domain.util.onFailure
import de.tabmates.core.domain.util.onSuccess
import de.tabmates.core.presentation.util.UiText
import de.tabmates.core.presentation.util.toUiText
import de.tabmates.features.authentication.domain.AuthService
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.WhileSubscribed
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import org.koin.core.annotation.KoinViewModel
import tabmatesapp.features.authentication.presentation.generated.resources.Res
import tabmatesapp.features.authentication.presentation.generated.resources.error_account_exists
import tabmatesapp.features.authentication.presentation.generated.resources.error_username_invalid
import kotlin.time.Duration.Companion.seconds

@KoinViewModel
class RegisterGuestViewModel(
    private val authService: AuthService,
) : ViewModel() {
    private var hasLoadedInitialData = false

    private val eventChannel = Channel<RegisterGuestEvent>()
    val events = eventChannel.receiveAsFlow()

    private val _state = MutableStateFlow(RegisterGuestState())
    val state =
        _state
            .onStart {
                if (!hasLoadedInitialData) {
                    hasLoadedInitialData = true
                }
            }.stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(5.seconds),
                initialValue = _state.value,
            )

    fun registerGuest() {
        if (!validateFormInputs()) {
            return
        }
        viewModelScope.launch {
            _state.update {
                it.copy(isRegistering = true)
            }

            authService
                .registerAnonymous(
                    _state.value.usernameTextState.text
                        .toString(),
                ).onSuccess {
                    _state.update { it.copy(isRegistering = false) }
                    eventChannel.send(RegisterGuestEvent.Success)
                }.onFailure { error ->
                    val registrationError =
                        when (error) {
                            DataError.Remote.CONFLICT -> UiText.Resource(Res.string.error_account_exists)
                            else -> error.toUiText()
                        }
                    _state.update { it.copy(isRegistering = false) }
                    eventChannel.send(RegisterGuestEvent.RegistrationError(registrationError))
                }
        }
    }

    private fun validateFormInputs(): Boolean {
        val currentState = state.value
        val username = currentState.usernameTextState.text.toString()

        val isUsernameValid = username.length in 3..20

        _state.update {
            it.copy(
                usernameError = if (!isUsernameValid) UiText.Resource(Res.string.error_username_invalid) else null,
            )
        }

        return isUsernameValid
    }
}
