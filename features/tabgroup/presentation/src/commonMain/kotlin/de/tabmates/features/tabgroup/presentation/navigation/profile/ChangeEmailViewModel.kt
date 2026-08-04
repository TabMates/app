package de.tabmates.features.tabgroup.presentation.navigation.profile

import androidx.compose.foundation.text.input.TextFieldState
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import de.tabmates.core.domain.auth.SessionStorage
import de.tabmates.core.domain.util.DataError
import de.tabmates.core.domain.util.onFailure
import de.tabmates.core.domain.util.onSuccess
import de.tabmates.core.presentation.util.UiText
import de.tabmates.core.presentation.util.toUiText
import de.tabmates.features.authentication.domain.AuthService
import de.tabmates.features.authentication.domain.EmailValidator
import de.tabmates.features.authentication.domain.normalizeEmail
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import org.koin.core.annotation.KoinViewModel
import tabmatesapp.features.tabgroup.presentation.generated.resources.Res
import tabmatesapp.features.tabgroup.presentation.generated.resources.change_email_error_in_use
import tabmatesapp.features.tabgroup.presentation.generated.resources.change_email_error_invalid
import tabmatesapp.features.tabgroup.presentation.generated.resources.change_email_error_password_required
import tabmatesapp.features.tabgroup.presentation.generated.resources.change_email_error_same
import tabmatesapp.features.tabgroup.presentation.generated.resources.change_email_error_wrong_password
import tabmatesapp.features.tabgroup.presentation.generated.resources.change_email_verification_sent

@KoinViewModel
class ChangeEmailViewModel(
    private val authService: AuthService,
    private val sessionStorage: SessionStorage,
) : ViewModel() {
    val newEmailState = TextFieldState()
    val passwordState = TextFieldState()

    private val _state = MutableStateFlow(ChangeEmailState())
    val state: StateFlow<ChangeEmailState> = _state.asStateFlow()

    private val eventChannel = Channel<ChangeEmailEvent>()
    val events = eventChannel.receiveAsFlow()

    fun onTogglePasswordVisibility() {
        _state.update { it.copy(isPasswordVisible = !it.isPasswordVisible) }
    }

    fun onSave() {
        if (_state.value.isSubmitting) return
        val newEmail = newEmailState.text.toString().normalizeEmail()
        val password = passwordState.text.toString()
        if (!EmailValidator.validate(newEmail)) {
            send(ChangeEmailEvent.Error(UiText.Resource(Res.string.change_email_error_invalid)))
            return
        }
        val currentEmail = sessionStorage.get()?.user?.email
        if (newEmail == currentEmail?.normalizeEmail()) {
            send(ChangeEmailEvent.Error(UiText.Resource(Res.string.change_email_error_same)))
            return
        }
        if (password.isBlank()) {
            send(ChangeEmailEvent.Error(UiText.Resource(Res.string.change_email_error_password_required)))
            return
        }
        viewModelScope.launch {
            _state.update { it.copy(isSubmitting = true) }
            authService
                .changeEmail(newEmail = newEmail, password = password)
                .onSuccess {
                    _state.update { it.copy(isSubmitting = false) }
                    eventChannel.send(
                        ChangeEmailEvent.Saved(
                            UiText.Resource(Res.string.change_email_verification_sent, arrayOf(newEmail)),
                        ),
                    )
                }.onFailure { error ->
                    _state.update { it.copy(isSubmitting = false) }
                    eventChannel.send(ChangeEmailEvent.Error(error.toScreenUiText()))
                }
        }
    }

    private fun DataError.Remote.toScreenUiText(): UiText {
        return when (this) {
            DataError.Remote.CONFLICT -> UiText.Resource(Res.string.change_email_error_in_use)
            DataError.Remote.UNAUTHORIZED -> UiText.Resource(Res.string.change_email_error_wrong_password)
            else -> toUiText()
        }
    }

    private fun send(event: ChangeEmailEvent) {
        viewModelScope.launch { eventChannel.send(event) }
    }
}
