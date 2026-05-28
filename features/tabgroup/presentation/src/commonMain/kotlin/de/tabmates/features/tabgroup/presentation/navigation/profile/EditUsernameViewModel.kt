package de.tabmates.features.tabgroup.presentation.navigation.profile

import androidx.compose.foundation.text.input.TextFieldState
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import de.tabmates.core.domain.auth.SessionStorage
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
import tabmatesapp.features.tabgroup.presentation.generated.resources.edit_username_error_too_short

@KoinViewModel
class EditUsernameViewModel(
    private val sessionStorage: SessionStorage,
    private val authService: AuthService,
) : ViewModel() {
    val usernameState =
        TextFieldState(
            sessionStorage
                .get()
                ?.user
                ?.username
                .orEmpty(),
        )

    private val _state = MutableStateFlow(AccountEditState())
    val state: StateFlow<AccountEditState> = _state.asStateFlow()

    private val eventChannel = Channel<AccountEditEvent>()
    val events = eventChannel.receiveAsFlow()

    fun onSave() {
        if (_state.value.isSubmitting) return
        val newUsername = usernameState.text.toString().trim()
        if (newUsername.length < MIN_USERNAME_LENGTH) {
            send(AccountEditEvent.Error(UiText.Resource(Res.string.edit_username_error_too_short)))
            return
        }
        viewModelScope.launch {
            _state.update { it.copy(isSubmitting = true) }
            authService
                .changeUsername(newUsername)
                .onSuccess {
                    _state.update { it.copy(isSubmitting = false) }
                    eventChannel.send(AccountEditEvent.Saved)
                }.onFailure { error ->
                    _state.update { it.copy(isSubmitting = false) }
                    eventChannel.send(AccountEditEvent.Error(error.toUiText()))
                }
        }
    }

    private fun send(event: AccountEditEvent) {
        viewModelScope.launch { eventChannel.send(event) }
    }

    private companion object {
        private const val MIN_USERNAME_LENGTH = 3
    }
}
