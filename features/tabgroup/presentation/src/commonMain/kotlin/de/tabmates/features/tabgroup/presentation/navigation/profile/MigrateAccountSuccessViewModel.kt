package de.tabmates.features.tabgroup.presentation.navigation.profile

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import de.tabmates.core.domain.util.onFailure
import de.tabmates.core.domain.util.onSuccess
import de.tabmates.core.presentation.util.UiText
import de.tabmates.core.presentation.util.toUiText
import de.tabmates.features.authentication.domain.AuthService
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import org.koin.core.annotation.InjectedParam
import org.koin.core.annotation.KoinViewModel

data class MigrateAccountSuccessState(
    val email: String = "",
    val isResending: Boolean = false,
)

sealed interface MigrateAccountSuccessEvent {
    data object ResendSuccess : MigrateAccountSuccessEvent

    data class ResendError(val message: UiText) : MigrateAccountSuccessEvent
}

@KoinViewModel
class MigrateAccountSuccessViewModel(
    private val authService: AuthService,
    @InjectedParam private val email: String,
) : ViewModel() {
    private val _state = MutableStateFlow(MigrateAccountSuccessState(email = email))
    val state = _state.asStateFlow()

    private val eventChannel = Channel<MigrateAccountSuccessEvent>()
    val events = eventChannel.receiveAsFlow()

    fun resendVerification() {
        if (_state.value.isResending) return

        viewModelScope.launch {
            _state.update { it.copy(isResending = true) }
            authService
                .resendVerificationEmail(email)
                .onSuccess {
                    _state.update { it.copy(isResending = false) }
                    eventChannel.send(MigrateAccountSuccessEvent.ResendSuccess)
                }.onFailure { error ->
                    _state.update { it.copy(isResending = false) }
                    eventChannel.send(MigrateAccountSuccessEvent.ResendError(error.toUiText()))
                }
        }
    }
}
