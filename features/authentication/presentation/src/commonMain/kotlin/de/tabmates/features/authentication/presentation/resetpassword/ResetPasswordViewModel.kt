package de.tabmates.features.authentication.presentation.resetpassword

import androidx.compose.runtime.snapshotFlow
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import de.tabmates.core.domain.util.DataError
import de.tabmates.core.domain.util.onFailure
import de.tabmates.core.domain.util.onSuccess
import de.tabmates.core.domain.validation.PasswordValidator
import de.tabmates.core.presentation.util.UiText
import de.tabmates.core.presentation.util.toUiText
import de.tabmates.features.authentication.domain.AuthService
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
import tabmatesapp.features.authentication.presentation.generated.resources.Res
import tabmatesapp.features.authentication.presentation.generated.resources.error_reset_password_token_invalid
import tabmatesapp.features.authentication.presentation.generated.resources.error_same_password

class ResetPasswordViewModel(
    private val authService: AuthService,
    private val token: String,
) : ViewModel() {
    private var hasLoadedInitialData = false

    private val isPasswordValidFlow =
        snapshotFlow {
            state.value.passwordTextState.text
                .toString()
        }.map { password -> PasswordValidator.validate(password) }
            .distinctUntilChanged()

    private val _state = MutableStateFlow(ResetPasswordState())
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

    fun onTogglePasswordVisibilityClick() {
        _state.value =
            _state.value.copy(
                isPasswordVisible = !_state.value.isPasswordVisible,
            )
    }

    fun submitResetPasswordRequest() {
        if (state.value.isLoading || !state.value.canSubmit) {
            return
        }

        viewModelScope.launch {
            _state.update {
                it.copy(
                    isLoading = true,
                    isResetSuccessful = false,
                )
            }

            val newPassword =
                state.value.passwordTextState.text
                    .toString()
            authService
                .resetPassword(
                    newPassword = newPassword,
                    token = token,
                ).onSuccess {
                    _state.update {
                        it.copy(
                            isLoading = false,
                            isResetSuccessful = true,
                            errorText = null,
                        )
                    }
                }.onFailure { error ->
                    val errorText =
                        when (error) {
                            DataError.Remote.UNAUTHORIZED -> {
                                UiText.Resource(
                                    Res.string.error_reset_password_token_invalid,
                                )
                            }

                            DataError.Remote.CONFLICT -> {
                                UiText.Resource(Res.string.error_same_password)
                            }

                            else -> {
                                error.toUiText()
                            }
                        }
                    _state.update {
                        it.copy(
                            errorText = errorText,
                            isLoading = false,
                        )
                    }
                }
        }
    }

    private fun observeValidationState() {
        isPasswordValidFlow
            .onEach { isPasswordValid ->
                _state.update {
                    it.copy(
                        canSubmit = isPasswordValid,
                    )
                }
            }.launchIn(viewModelScope)
    }
}
