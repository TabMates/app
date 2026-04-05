package de.tabmates.features.authentication.presentation.register

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
import de.tabmates.features.authentication.domain.EmailValidator
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.WhileSubscribed
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import tabmatesapp.features.authentication.presentation.generated.resources.Res
import tabmatesapp.features.authentication.presentation.generated.resources.error_account_exists
import tabmatesapp.features.authentication.presentation.generated.resources.error_username_invalid
import tabmatesapp.features.authentication.presentation.generated.resources.register_email_invalid
import tabmatesapp.features.authentication.presentation.generated.resources.register_password_mismatch
import tabmatesapp.features.authentication.presentation.generated.resources.register_password_requirements
import kotlin.time.Duration.Companion.seconds

class RegisterViewModel(
    private val authService: AuthService,
) : ViewModel() {
    private val eventChannel = Channel<RegisterEvent>()
    val events = eventChannel.receiveAsFlow()

    private var hasLoadedInitialData = false

    private val _state = MutableStateFlow(RegisterState())
    val state =
        _state
            .onStart {
                if (!hasLoadedInitialData) {
                    observeFieldClearOnEdit()
                    hasLoadedInitialData = true
                }
            }.stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(5.seconds),
                initialValue = _state.value,
            )

    private fun observeFieldClearOnEdit() {
        snapshotFlow {
            _state.value.usernameTextState.text
                .toString()
        }.onEach {
            _state.update {
                if (it.usernameError != null) {
                    it.copy(usernameError = null)
                } else {
                    it
                }
            }
        }.launchIn(viewModelScope)

        snapshotFlow {
            _state.value.emailTextState.text
                .toString()
        }.onEach {
            _state.update {
                if (it.emailError != null) {
                    it.copy(emailError = null)
                } else {
                    it
                }
            }
        }.launchIn(viewModelScope)

        snapshotFlow {
            _state.value.passwordTextState.text
                .toString()
        }.onEach {
            _state.update {
                if (it.passwordError != null) {
                    it.copy(passwordError = null)
                } else {
                    it
                }
            }
        }.launchIn(viewModelScope)

        snapshotFlow {
            _state.value.confirmPasswordTextState.text
                .toString()
        }.onEach {
            _state.update {
                if (it.confirmPasswordError != null) {
                    it.copy(confirmPasswordError = null)
                } else {
                    it
                }
            }
        }.launchIn(viewModelScope)
    }

    fun validateUsernameOnBlur() {
        val username =
            state.value.usernameTextState.text
                .toString()
        if (username.isEmpty()) return
        val error =
            if (username.length !in 3..20) {
                UiText.Resource(Res.string.error_username_invalid)
            } else {
                null
            }
        _state.update { it.copy(usernameError = error) }
    }

    fun validateEmailOnBlur() {
        val email =
            state.value.emailTextState.text
                .toString()
        if (email.isEmpty()) return
        val error =
            if (!EmailValidator.validate(email)) {
                UiText.Resource(Res.string.register_email_invalid)
            } else {
                null
            }
        _state.update { it.copy(emailError = error) }
    }

    fun validatePasswordOnBlur() {
        val password =
            state.value.passwordTextState.text
                .toString()
        if (password.isEmpty()) return
        val error =
            if (!PasswordValidator.validate(password)) {
                UiText.Resource(Res.string.register_password_requirements)
            } else {
                null
            }
        _state.update { it.copy(passwordError = error) }
    }

    fun validateConfirmPasswordOnBlur() {
        val confirmPassword =
            state.value.confirmPasswordTextState.text
                .toString()
        if (confirmPassword.isEmpty()) return

        val password =
            state.value.passwordTextState.text
                .toString()
        val error =
            if (confirmPassword != password) {
                UiText.Resource(Res.string.register_password_mismatch)
            } else {
                null
            }
        _state.update { it.copy(confirmPasswordError = error) }
    }

    fun togglePasswordVisibility() {
        _state.update {
            it.copy(
                isPasswordVisible = !it.isPasswordVisible,
            )
        }
    }

    fun toggleConfirmPasswordVisibility() {
        _state.update {
            it.copy(
                isConfirmPasswordVisible = !it.isConfirmPasswordVisible,
            )
        }
    }

    fun register() {
        if (!validateFormInputs()) {
            return
        }

        viewModelScope.launch {
            _state.update {
                it.copy(
                    isRegistering = true,
                )
            }

            val email =
                state.value.emailTextState.text
                    .toString()
            val username =
                state.value.usernameTextState.text
                    .toString()
            val password =
                state.value.passwordTextState.text
                    .toString()

            authService
                .register(
                    email = email,
                    username = username,
                    password = password,
                ).onSuccess {
                    _state.update { it.copy(isRegistering = false) }
                    eventChannel.send(RegisterEvent.Success(email))
                }.onFailure { error ->
                    val registrationError =
                        when (error) {
                            DataError.Remote.CONFLICT -> UiText.Resource(Res.string.error_account_exists)
                            else -> error.toUiText()
                        }
                    _state.update { it.copy(isRegistering = false) }
                    eventChannel.send(RegisterEvent.RegistrationError(registrationError))
                }
        }
    }

    private fun validateFormInputs(): Boolean {
        val currentState = state.value
        val email = currentState.emailTextState.text.toString()
        val username = currentState.usernameTextState.text.toString()
        val password = currentState.passwordTextState.text.toString()
        val confirmPassword = currentState.confirmPasswordTextState.text.toString()

        val isEmailValid = EmailValidator.validate(email)
        val isPasswordValid = PasswordValidator.validate(password)
        val isConfirmPasswordValid = confirmPassword == password
        val isUsernameValid = username.length in 3..20

        _state.update {
            it.copy(
                emailError = if (!isEmailValid) UiText.Resource(Res.string.register_email_invalid) else null,
                usernameError = if (!isUsernameValid) UiText.Resource(Res.string.error_username_invalid) else null,
                passwordError =
                    if (!isPasswordValid) {
                        UiText.Resource(
                            Res.string.register_password_requirements,
                        )
                    } else {
                        null
                    },
                confirmPasswordError =
                    if (!isConfirmPasswordValid) {
                        UiText.Resource(Res.string.register_password_mismatch)
                    } else {
                        null
                    },
            )
        }

        return isUsernameValid && isEmailValid && isPasswordValid && isConfirmPasswordValid
    }
}
