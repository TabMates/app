package de.tabmates.features.authentication.presentation.forgotpassword

import androidx.compose.runtime.snapshotFlow
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import de.tabmates.core.domain.util.onFailure
import de.tabmates.core.domain.util.onSuccess
import de.tabmates.core.presentation.util.toUiText
import de.tabmates.features.authentication.domain.AuthService
import de.tabmates.features.authentication.domain.EmailValidator
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class ForgotPasswordViewModel(
    private val authService: AuthService,
) : ViewModel() {
    private var hasLoadedInitialData = false

    private val isEmailValidFlow =
        snapshotFlow {
            state.value.emailTextFieldState.text
                .toString()
        }.map { email -> EmailValidator.validate(email) }
            .distinctUntilChanged()

    private val _state = MutableStateFlow(ForgotPasswordState())
    val state =
        _state
            .onStart {
                if (!hasLoadedInitialData) {
                    observeValidationState()
                    hasLoadedInitialData = true
                }
            }.stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(5_000L),
                initialValue = _state.value,
            )

    fun submitForgotPasswordRequest() {
        if (state.value.isLoading || !state.value.canSubmit) {
            return
        }

        viewModelScope.launch {
            _state.update {
                it.copy(
                    isLoading = true,
                    isEmailSentSuccessfully = false,
                    errorText = null,
                )
            }

            val email =
                state.value.emailTextFieldState.text
                    .toString()
            authService
                .forgotPassword(email)
                .onSuccess {
                    _state.update {
                        it.copy(
                            isEmailSentSuccessfully = true,
                            isLoading = false,
                        )
                    }
                }.onFailure { error ->
                    _state.update {
                        it.copy(
                            errorText = error.toUiText(),
                            isLoading = false,
                        )
                    }
                }
        }
    }

    private fun observeValidationState() {
        isEmailValidFlow
            .onEach { isEmailValid ->
                _state.update {
                    it.copy(
                        canSubmit = isEmailValid,
                    )
                }
            }.launchIn(viewModelScope)
    }
}
