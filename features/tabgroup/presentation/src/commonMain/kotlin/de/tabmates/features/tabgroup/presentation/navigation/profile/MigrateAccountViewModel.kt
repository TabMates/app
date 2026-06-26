package de.tabmates.features.tabgroup.presentation.navigation.profile

import androidx.compose.foundation.text.input.TextFieldState
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
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import org.koin.core.annotation.KoinViewModel
import tabmatesapp.features.tabgroup.presentation.generated.resources.Res
import tabmatesapp.features.tabgroup.presentation.generated.resources.migrate_account_error_email_invalid
import tabmatesapp.features.tabgroup.presentation.generated.resources.migrate_account_error_email_unavailable
import tabmatesapp.features.tabgroup.presentation.generated.resources.migrate_account_error_password_mismatch
import tabmatesapp.features.tabgroup.presentation.generated.resources.migrate_account_error_password_requirements

@KoinViewModel
class MigrateAccountViewModel(
    private val authService: AuthService,
) : ViewModel() {
    val emailState = TextFieldState()
    val passwordState = TextFieldState()
    val confirmPasswordState = TextFieldState()

    private val _state = MutableStateFlow(MigrateAccountState())
    val state = _state.asStateFlow()

    private val eventChannel = Channel<MigrateAccountEvent>()
    val events = eventChannel.receiveAsFlow()

    fun togglePasswordVisibility() {
        _state.update { it.copy(isPasswordVisible = !it.isPasswordVisible) }
    }

    fun toggleConfirmPasswordVisibility() {
        _state.update { it.copy(isConfirmPasswordVisible = !it.isConfirmPasswordVisible) }
    }

    fun onSave() {
        if (_state.value.isSubmitting) return
        if (!validate()) return

        val email = emailState.text.toString()
        val password = passwordState.text.toString()

        viewModelScope.launch {
            _state.update { it.copy(isSubmitting = true) }
            authService
                .migrateToRegistered(email = email, password = password)
                .onSuccess {
                    _state.update { it.copy(isSubmitting = false) }
                    eventChannel.send(MigrateAccountEvent.Migrated(email))
                }.onFailure { error ->
                    val message =
                        when (error) {
                            DataError.Remote.CONFLICT -> {
                                UiText.Resource(Res.string.migrate_account_error_email_unavailable)
                            }

                            else -> {
                                error.toUiText()
                            }
                        }
                    _state.update { it.copy(isSubmitting = false) }
                    eventChannel.send(MigrateAccountEvent.Error(message))
                }
        }
    }

    private fun validate(): Boolean {
        val email = emailState.text.toString()
        val password = passwordState.text.toString()
        val confirmPassword = confirmPasswordState.text.toString()

        val isEmailValid = EmailValidator.validate(email)
        val isPasswordValid = PasswordValidator.validate(password)
        val isConfirmValid = confirmPassword == password

        _state.update {
            it.copy(
                emailError =
                    if (!isEmailValid) UiText.Resource(Res.string.migrate_account_error_email_invalid) else null,
                passwordError =
                    if (!isPasswordValid) {
                        UiText.Resource(Res.string.migrate_account_error_password_requirements)
                    } else {
                        null
                    },
                confirmPasswordError =
                    if (!isConfirmValid) {
                        UiText.Resource(Res.string.migrate_account_error_password_mismatch)
                    } else {
                        null
                    },
            )
        }

        return isEmailValid && isPasswordValid && isConfirmValid
    }
}
