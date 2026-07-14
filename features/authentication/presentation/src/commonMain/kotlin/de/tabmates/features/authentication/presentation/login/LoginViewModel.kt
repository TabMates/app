package de.tabmates.features.authentication.presentation.login

import androidx.compose.runtime.snapshotFlow
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import de.tabmates.core.domain.util.DataError
import de.tabmates.core.domain.util.onFailure
import de.tabmates.core.domain.util.onSuccess
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
import org.koin.core.annotation.KoinViewModel
import tabmatesapp.features.authentication.presentation.generated.resources.Res
import tabmatesapp.features.authentication.presentation.generated.resources.error_email_not_verified
import tabmatesapp.features.authentication.presentation.generated.resources.error_invalid_credentials
import kotlin.time.Duration.Companion.seconds

@KoinViewModel
class LoginViewModel(
    private val authService: AuthService,
) : ViewModel() {
    private var hasLoadedInitialData = false

    private val _state = MutableStateFlow(LoginState())
    val state =
        _state
            .onStart {
                if (!hasLoadedInitialData) {
                    observeTextStates()
                    hasLoadedInitialData = true
                }
            }.stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(5.seconds),
                initialValue = _state.value,
            )

    private val eventChannel = Channel<LoginEvent>()
    val events = eventChannel.receiveAsFlow()

    private val isEmailValidFlow =
        snapshotFlow {
            _state.value.emailTextFieldState.text
                .toString()
                .trim()
        }.map { email -> EmailValidator.validate(email) }
            .distinctUntilChanged()

    private val isPasswordNotBlankFlow =
        snapshotFlow {
            _state.value.passwordTextFieldState.text
                .toString()
        }.map { it.isNotBlank() }.distinctUntilChanged()

    private val isLoggingInFlow = _state.map { it.isLoggingIn }.distinctUntilChanged()

    private fun observeTextStates() {
        combine(
            isEmailValidFlow,
            isPasswordNotBlankFlow,
            isLoggingInFlow,
        ) { isEmailValid, isPasswordNotBlank, isRegistering ->
            _state.update {
                it.copy(
                    canLogin = !isRegistering && isEmailValid && isPasswordNotBlank,
                )
            }
        }.launchIn(viewModelScope)
    }

    fun onTogglePasswordVisibility() {
        _state.value = _state.value.copy(isPasswordVisible = !_state.value.isPasswordVisible)
    }

    fun onLogin() {
        if (!state.value.canLogin) {
            return
        }

        viewModelScope.launch {
            _state.update {
                it.copy(
                    isLoggingIn = true,
                    isEmailNotVerified = false,
                )
            }

            val email =
                state.value.emailTextFieldState.text
                    .toString()
                    .trim()
            val password =
                state.value.passwordTextFieldState.text
                    .toString()

            authService
                .login(
                    email = email,
                    password = password,
                ).onSuccess {
                    _state.update {
                        it.copy(
                            isLoggingIn = false,
                        )
                    }
                    eventChannel.send(LoginEvent.LoginSuccess)
                }.onFailure { error ->
                    val errorMessage =
                        when (error) {
                            DataError.Remote.UNAUTHORIZED -> UiText.Resource(Res.string.error_invalid_credentials)
                            DataError.Remote.FORBIDDEN -> UiText.Resource(Res.string.error_email_not_verified)
                            else -> error.toUiText()
                        }

                    _state.update {
                        it.copy(
                            isLoggingIn = false,
                            isEmailNotVerified = error == DataError.Remote.FORBIDDEN,
                        )
                    }
                    eventChannel.send(LoginEvent.LoginFailure(errorMessage))
                }
        }
    }

    fun resendVerification() {
        if (_state.value.isResendingVerificationEmail) {
            return
        }

        viewModelScope.launch {
            _state.update {
                it.copy(
                    isResendingVerificationEmail = true,
                )
            }

            val email =
                state.value.emailTextFieldState.text
                    .toString()
                    .trim()

            authService
                .resendVerificationEmail(email)
                .onSuccess {
                    _state.update {
                        it.copy(
                            isResendingVerificationEmail = false,
                        )
                    }
                    eventChannel.send(LoginEvent.ResendVerificationEmailSuccess)
                }.onFailure { error ->
                    _state.update {
                        it.copy(
                            isResendingVerificationEmail = false,
                            resendVerificationError = error.toUiText(),
                        )
                    }
                    eventChannel.send(LoginEvent.ResendVerificationEmailError)
                }
        }
    }
}
