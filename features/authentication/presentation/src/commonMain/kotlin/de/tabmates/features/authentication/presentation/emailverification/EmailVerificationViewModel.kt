package de.tabmates.features.authentication.presentation.emailverification

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import de.tabmates.core.domain.auth.SessionInvalidationReason
import de.tabmates.core.domain.auth.SessionInvalidator
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
import org.koin.core.annotation.InjectedParam
import org.koin.core.annotation.KoinViewModel
import kotlin.time.Duration.Companion.seconds

@KoinViewModel
class EmailVerificationViewModel(
    private val authService: AuthService,
    private val sessionInvalidator: SessionInvalidator,
    @InjectedParam private val token: String,
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
                    // Confirming an email change revokes all refresh tokens server-side, so force
                    // a fresh login. Routed through the invalidator so this counts as an *expired*
                    // session rather than a sign-out: local data stays put and the user is asked
                    // back into the same account — under their new address, hence EMAIL_CHANGED.
                    sessionInvalidator.invalidate(SessionInvalidationReason.EMAIL_CHANGED)
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
