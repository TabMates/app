package de.tabmates.features.authentication.presentation.emailverification

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import de.tabmates.core.domain.auth.SessionInvalidationReason
import de.tabmates.core.domain.auth.SessionInvalidator
import de.tabmates.core.domain.auth.SessionStorage
import de.tabmates.core.domain.auth.UserType
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
    private val sessionStorage: SessionStorage,
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
            // Read before the token is spent: succeeding empties the storage in every branch but
            // the guest upgrade, and both the outcome copy and the failure hint need to know which
            // flow the user came from.
            val origin =
                when (sessionStorage.get()?.user?.userType) {
                    null -> VerificationOrigin.NoSession
                    UserType.ANONYMOUS -> VerificationOrigin.Guest
                    UserType.REGISTERED -> VerificationOrigin.Registered
                }
            _state.update {
                it.copy(origin = origin, status = VerificationStatus.Verifying)
            }

            authService
                .verifyEmail(token)
                .onSuccess {
                    // An anonymous account has no address to change and no registration to confirm,
                    // so a token redeemed under one can only be its upgrade to a registered
                    // account. That path keeps the session alive on purpose — the account had no
                    // password to sign back in with until this very moment — so it must not go
                    // through the invalidator below.
                    if (origin == VerificationOrigin.Guest) {
                        adoptRegisteredUser()
                    } else {
                        // Confirming an email change revokes all refresh tokens server-side, so
                        // force a fresh login. Routed through the invalidator so this counts as an
                        // *expired* session rather than a sign-out: local data stays put and the
                        // user is asked back into the same account — under their new address,
                        // hence EMAIL_CHANGED.
                        sessionInvalidator.invalidate(SessionInvalidationReason.EMAIL_CHANGED)
                    }
                    _state.update {
                        it.copy(status = VerificationStatus.Succeeded)
                    }
                }.onFailure {
                    _state.update {
                        it.copy(status = VerificationStatus.Failed)
                    }
                }
        }
    }

    /**
     * Replaces the cached anonymous user with the registered one the migration just produced.
     *
     * Normally that is the server's own copy. When it cannot be fetched the account type is still
     * known — a redeemed token under an anonymous session *is* the migration — so it is corrected
     * locally instead of leaving the app treating a registered user as a guest, which would offer
     * them the upgrade again and warn them that signing out destroys their groups. The address
     * catches up on the next refresh; ending the session over a failed GET would undo the one
     * thing this flow exists to guarantee.
     */
    private suspend fun adoptRegisteredUser() {
        authService.refreshAccount().onFailure {
            sessionStorage.get()?.let { current ->
                sessionStorage.set(
                    current.copy(
                        user =
                            current.user.copy(
                                hasVerifiedEmail = true,
                                userType = UserType.REGISTERED,
                            ),
                    ),
                )
            }
        }
    }
}
