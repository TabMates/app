package de.tabmates.features.authentication.presentation.registersuccess

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import de.tabmates.core.domain.util.onFailure
import de.tabmates.core.domain.util.onSuccess
import de.tabmates.core.presentation.util.toUiText
import de.tabmates.features.authentication.domain.AuthService
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class RegisterSuccessViewModel(
    private val authService: AuthService,
    private val email: String,
) : ViewModel() {
    private var hasLoadedInitialData = false

    private val eventChannel = Channel<RegisterSuccessEvent>()
    val events = eventChannel.receiveAsFlow()

    private val _state =
        MutableStateFlow(
            RegisterSuccessState(
                registeredEmail = email,
            ),
        )
    val state =
        _state
            .onStart {
                if (!hasLoadedInitialData) {
                    hasLoadedInitialData = true
                }
            }.stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(5_000L),
                initialValue = _state.value,
            )

    fun resendVerification() {
        if (state.value.isResendingVerificationEmail) {
            return
        }

        viewModelScope.launch {
            _state.update {
                it.copy(
                    isResendingVerificationEmail = true,
                )
            }

            authService
                .resendVerificationEmail(email)
                .onSuccess {
                    _state.update {
                        it.copy(
                            isResendingVerificationEmail = false,
                        )
                    }
                    eventChannel.send(RegisterSuccessEvent.ResendVerificationEmailSuccess)
                }.onFailure { error ->
                    _state.update {
                        it.copy(
                            isResendingVerificationEmail = false,
                            resendVerificationError = error.toUiText(),
                        )
                    }
                    eventChannel.send(RegisterSuccessEvent.ResendVerificationEmailError)
                }
        }
    }
}
