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
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import tabmatesapp.features.authentication.presentation.generated.resources.Res
import tabmatesapp.features.authentication.presentation.generated.resources.error_account_exists
import tabmatesapp.features.authentication.presentation.generated.resources.error_username_invalid
import tabmatesapp.features.authentication.presentation.generated.resources.register_email_invalid
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
                    observeValidationStates()
                    hasLoadedInitialData = true
                }
            }.stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(5.seconds),
                initialValue = RegisterState(),
            )

    private val isEmailValidFlow =
        snapshotFlow {
            state.value.emailTextState.text
                .toString()
        }.map { email -> EmailValidator.validate(email) }
            .distinctUntilChanged()

    private val isUsernameValidFlow =
        snapshotFlow {
            state.value.usernameTextState.text
                .toString()
        }.map { username -> username.length in 3..20 }
            .distinctUntilChanged()

    private val isPasswordValidFlow =
        snapshotFlow {
            state.value.passwordTextState.text
                .toString()
        }.map { password -> PasswordValidator.validate(password).isValidPassword }
            .distinctUntilChanged()

    private val isRegisteringFlow =
        state
            .map { it.isRegistering }
            .distinctUntilChanged()

    private fun observeValidationStates() {
        combine(
            isEmailValidFlow,
            isUsernameValidFlow,
            isPasswordValidFlow,
            isRegisteringFlow,
        ) { isEmailValid, isUsernameValid, isPasswordValid, isRegistering ->
            val allValid = isEmailValid && isUsernameValid && isPasswordValid
            _state.update {
                it.copy(
                    canRegister = !isRegistering && allValid,
                )
            }
        }.launchIn(viewModelScope)
    }

    fun togglePasswordVisibility() {
        _state.update {
            it.copy(
                isPasswordVisible = !it.isPasswordVisible,
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
                    println("DIAS_D: onSuccess: $email")
                    _state.update {
                        it.copy(
                            isRegistering = false,
                        )
                    }
                    eventChannel.send(RegisterEvent.Success(email))
                }.onFailure { error ->
                    println("DIAS_D: onFailure: $email, error: $error")
                    val registrationError =
                        when (error) {
                            DataError.Remote.CONFLICT -> UiText.Resource(Res.string.error_account_exists)
                            else -> error.toUiText()
                        }
                    _state.update {
                        it.copy(
                            isRegistering = false,
                            registrationError = registrationError,
                        )
                    }
                }
        }
    }

    private fun clearAllTextFieldErrors() {
        _state.update {
            it.copy(
                emailError = null,
                usernameError = null,
                passwordError = null,
                registrationError = null,
            )
        }
    }

    private fun validateFormInputs(): Boolean {
        clearAllTextFieldErrors()

        val currentState = state.value
        val email = currentState.emailTextState.text.toString()
        val username = currentState.usernameTextState.text.toString()
        val password = currentState.passwordTextState.text.toString()

        val isEmailValid = EmailValidator.validate(email)
        val passwordValidationState = PasswordValidator.validate(password)
        val isUsernameValid = username.length in 3..20

        val emailError =
            if (!isEmailValid) {
                UiText.Resource(Res.string.register_email_invalid)
            } else {
                null
            }
        val usernameError =
            if (!isUsernameValid) {
                UiText.Resource(Res.string.error_username_invalid)
            } else {
                null
            }
        val passwordError =
            if (!passwordValidationState.isValidPassword) {
                UiText.Resource(Res.string.register_password_requirements)
            } else {
                null
            }

        _state.update {
            it.copy(
                emailError = emailError,
                usernameError = usernameError,
                passwordError = passwordError,
            )
        }

        return isUsernameValid && isEmailValid && passwordValidationState.isValidPassword
    }
}
