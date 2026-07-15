package de.tabmates.features.tabgroup.presentation.navigation.profile

import androidx.compose.foundation.text.input.TextFieldState
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import de.tabmates.core.domain.auth.SessionStorage
import de.tabmates.core.domain.auth.UserType
import de.tabmates.core.domain.util.DataError
import de.tabmates.core.domain.util.onFailure
import de.tabmates.core.domain.util.onSuccess
import de.tabmates.core.presentation.util.UiText
import de.tabmates.core.presentation.util.toUiText
import de.tabmates.features.authentication.domain.AuthService
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import org.koin.core.annotation.KoinViewModel
import tabmatesapp.features.tabgroup.presentation.generated.resources.Res
import tabmatesapp.features.tabgroup.presentation.generated.resources.delete_account_error_password_required
import tabmatesapp.features.tabgroup.presentation.generated.resources.delete_account_error_wrong_password

@KoinViewModel
class DeleteAccountViewModel(
    private val authService: AuthService,
    private val sessionStorage: SessionStorage,
) : ViewModel() {
    val passwordState = TextFieldState()

    private val _state =
        MutableStateFlow(
            DeleteAccountState(
                isRegistered = sessionStorage.get()?.user?.userType == UserType.REGISTERED,
            ),
        )
    val state: StateFlow<DeleteAccountState> = _state.asStateFlow()

    private val eventChannel = Channel<DeleteAccountEvent>()
    val events = eventChannel.receiveAsFlow()

    fun onTogglePasswordVisibility() {
        _state.update { it.copy(isPasswordVisible = !it.isPasswordVisible) }
    }

    fun onDeleteClick() {
        val current = _state.value
        if (current.isSubmitting) return
        if (current.isRegistered && passwordState.text.toString().isBlank()) {
            send(DeleteAccountEvent.Error(UiText.Resource(Res.string.delete_account_error_password_required)))
            return
        }
        _state.update { it.copy(showConfirmDialog = true) }
    }

    fun onDismissDialog() {
        _state.update { it.copy(showConfirmDialog = false) }
    }

    fun onConfirmDelete() {
        val current = _state.value
        if (current.isSubmitting) return
        _state.update { it.copy(isSubmitting = true, showConfirmDialog = false) }
        viewModelScope.launch {
            // Anonymous users have no password to send.
            val password = if (current.isRegistered) passwordState.text.toString() else null
            authService
                .deleteAccount(password = password)
                .onSuccess {
                    // Clearing the session makes the app shell return to Welcome.
                    sessionStorage.set(null)
                    eventChannel.send(DeleteAccountEvent.Deleted)
                }.onFailure { error ->
                    _state.update { it.copy(isSubmitting = false) }
                    eventChannel.send(DeleteAccountEvent.Error(error.toScreenUiText()))
                }
        }
    }

    private fun DataError.Remote.toScreenUiText(): UiText {
        return when (this) {
            DataError.Remote.UNAUTHORIZED -> UiText.Resource(Res.string.delete_account_error_wrong_password)
            else -> toUiText()
        }
    }

    private fun send(event: DeleteAccountEvent) {
        viewModelScope.launch { eventChannel.send(event) }
    }
}
