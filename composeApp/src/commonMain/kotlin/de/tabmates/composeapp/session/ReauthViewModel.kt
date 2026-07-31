package de.tabmates.composeapp.session

import androidx.compose.foundation.text.input.TextFieldState
import androidx.compose.runtime.snapshotFlow
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import de.tabmates.core.domain.auth.SessionStorage
import de.tabmates.core.domain.auth.StaleSessionStore
import de.tabmates.core.domain.auth.UserType
import de.tabmates.core.domain.sync.LocalDataResetter
import de.tabmates.core.domain.sync.PendingWrites
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
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import org.koin.core.annotation.KoinViewModel
import tabmatesapp.composeapp.generated.resources.Res
import tabmatesapp.composeapp.generated.resources.reauth_error_different_account
import tabmatesapp.composeapp.generated.resources.reauth_error_invalid_credentials
import kotlin.time.Duration.Companion.seconds

@KoinViewModel
class ReauthViewModel(
    private val authService: AuthService,
    private val sessionStorage: SessionStorage,
    private val staleSessionStore: StaleSessionStore,
    private val localDataResetter: LocalDataResetter,
    private val pendingWrites: PendingWrites,
) : ViewModel() {
    private var hasLoadedInitialData = false

    /**
     * Captured once, up front: the coordinator clears the record as soon as a matching account
     * signs in, so the id has to be held here to still be comparable afterwards.
     */
    private val staleSession = staleSessionStore.get()

    private val _state =
        MutableStateFlow(
            ReauthState(
                emailTextFieldState = TextFieldState(staleSession?.email.orEmpty()),
                isEmailLocked = staleSession?.email != null,
                isGuest = staleSession?.userType == UserType.ANONYMOUS,
            ),
        )
    val state =
        _state
            .onStart {
                if (!hasLoadedInitialData) {
                    observeTextStates()
                    observePendingWrites()
                    hasLoadedInitialData = true
                }
            }.stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(5.seconds),
                initialValue = _state.value,
            )

    private val eventChannel = Channel<ReauthEvent>()
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

    private val isSigningInFlow = _state.map { it.isSigningIn }.distinctUntilChanged()

    private fun observeTextStates() {
        combine(
            isEmailValidFlow,
            isPasswordNotBlankFlow,
            isSigningInFlow,
        ) { isEmailValid, isPasswordNotBlank, isSigningIn ->
            _state.update {
                it.copy(canSubmit = !isSigningIn && isEmailValid && isPasswordNotBlank)
            }
        }.launchIn(viewModelScope)
    }

    private fun observePendingWrites() {
        pendingWrites
            .observeCount()
            .distinctUntilChanged()
            .onEach { count -> _state.update { it.copy(pendingWriteCount = count) } }
            .launchIn(viewModelScope)
    }

    fun onTogglePasswordVisibility() {
        _state.update { it.copy(isPasswordVisible = !it.isPasswordVisible) }
    }

    fun onSignIn() {
        if (!state.value.canSubmit) return

        viewModelScope.launch {
            _state.update { it.copy(isSigningIn = true) }

            val email =
                state.value.emailTextFieldState.text
                    .toString()
                    .trim()
            val password =
                state.value.passwordTextFieldState.text
                    .toString()

            authService
                .login(email = email, password = password)
                .onSuccess { authInfo ->
                    _state.update { it.copy(isSigningIn = false) }
                    onSignedIn(authInfo.user.id)
                }.onFailure { error ->
                    _state.update { it.copy(isSigningIn = false) }
                    val message =
                        when (error) {
                            DataError.Remote.UNAUTHORIZED ->
                                UiText.Resource(Res.string.reauth_error_invalid_credentials)
                            else -> error.toUiText()
                        }
                    eventChannel.send(ReauthEvent.ReauthFailed(message))
                }
        }
    }

    /**
     * Takes the id from the login response rather than re-reading the session: the sync coordinator
     * reacts to the same sign-in, and reading back afterwards would race it.
     */
    private suspend fun onSignedIn(signedInUserId: String) {
        val expectedUserId = staleSession?.userId

        if (expectedUserId != null && signedInUserId != expectedUserId) {
            // A different account. Drop the session straight away rather than let it adopt the
            // previous account's groups and queued writes; the device stays in the expired state
            // with everything intact, and switching for real goes through the warned wipe.
            sessionStorage.set(null)
            eventChannel.send(ReauthEvent.ReauthFailed(UiText.Resource(Res.string.reauth_error_different_account)))
            return
        }

        staleSessionStore.clear()
        eventChannel.send(ReauthEvent.ReauthSucceeded)
    }

    fun onSwitchAccountClick() {
        _state.update { it.copy(showSwitchAccountDialog = true) }
    }

    fun onDismissSwitchAccountDialog() {
        _state.update { it.copy(showSwitchAccountDialog = false) }
    }

    fun onConfirmSwitchAccount() {
        viewModelScope.launch {
            _state.update { it.copy(showSwitchAccountDialog = false) }
            // Reset first: clearing the record is what tells the rest of the app this is a real
            // sign-out, so it must not happen while the data is still there to be adopted.
            localDataResetter.resetLocalData()
            // This path never calls logout, so nothing else drops the token the client still holds
            // for the account being left behind. An expired session does not always mean an expired
            // access token — an email change revokes the refresh token while the access token lives
            // on — and a token that outlives its session authenticates the next account's requests
            // as the previous one.
            authService.clearCachedTokens()
            staleSessionStore.clear()
            // No navigation here — dropping the record leaves the app with no session at all,
            // which the shell already handles by returning to the welcome screen.
        }
    }
}
