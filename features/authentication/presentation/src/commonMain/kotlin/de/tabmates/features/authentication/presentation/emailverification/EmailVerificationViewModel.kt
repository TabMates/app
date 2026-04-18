package de.tabmates.features.authentication.presentation.emailverification

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import de.tabmates.core.domain.util.onFailure
import de.tabmates.core.domain.util.onSuccess
import de.tabmates.features.authentication.domain.AuthService
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.WhileSubscribed
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlin.time.Duration.Companion.seconds

class EmailVerificationViewModel(
    private val authService: AuthService,
    private val token: String,
) : ViewModel() {
    private var hasLoadedInitialData = false

    private val _state = MutableStateFlow(EmailVerificationState())
    val state =
        _state
            .onStart {
                if (!hasLoadedInitialData) {
                    verifyEmail()
                    hasLoadedInitialData = true
                }
            }.stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(5.seconds),
                initialValue = _state.value,
            )

    private fun verifyEmail() {
        viewModelScope.launch {
            _state.update {
                it.copy(isVerifying = true)
            }

            authService
                .verifyEmail(token)
                .onSuccess {
                    _state.update {
                        it.copy(isVerifying = false, isVerified = true)
                    }
                }.onFailure {
                    _state.update {
                        it.copy(isVerified = false, isVerifying = false)
                    }
                }
        }
    }
}
