package de.tabmates.composeapp.lock

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import de.tabmates.core.domain.auth.SessionStorage
import de.tabmates.core.domain.biometric.BiometricAuthenticator
import de.tabmates.core.domain.biometric.BiometricPromptStrings
import de.tabmates.core.domain.biometric.BiometricResult
import de.tabmates.core.domain.preferences.AppPreferencesRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import org.koin.core.annotation.KoinViewModel
import kotlin.time.Duration.Companion.seconds

/** What the lock gate should render. */
enum class AppLockUiState {
    /** Enabled-flag not yet read; render a neutral splash to avoid flashing content. */
    RESOLVING,

    /** Lock is enforced — cover the app and require authentication. */
    LOCKED,

    /** App is accessible (lock disabled, not signed in, or already authenticated). */
    UNLOCKED,
}

@KoinViewModel
class AppLockViewModel(
    appPreferencesRepository: AppPreferencesRepository,
    private val sessionStorage: SessionStorage,
    private val controller: AppLockController,
    private val biometricAuthenticator: BiometricAuthenticator,
) : ViewModel() {
    // Nullable until the first stored value arrives, so the gate can show a splash meanwhile.
    private val enabled: StateFlow<Boolean?> =
        appPreferencesRepository
            .biometricUnlockEnabled()
            .stateIn(viewModelScope, SharingStarted.Eagerly, null)

    private val loggedIn: StateFlow<Boolean> =
        sessionStorage.authState
            .map { it != null }
            .stateIn(viewModelScope, SharingStarted.Eagerly, sessionStorage.get() != null)

    val uiState: StateFlow<AppLockUiState> =
        combine(enabled, loggedIn, controller.unlocked) { isEnabled, isLoggedIn, isUnlocked ->
            when {
                isEnabled == null -> AppLockUiState.RESOLVING
                // Nothing to protect when the lock is off or there is no active session.
                !isEnabled || !isLoggedIn || isUnlocked -> AppLockUiState.UNLOCKED
                else -> AppLockUiState.LOCKED
            }
        }.stateIn(viewModelScope, SharingStarted.Eagerly, AppLockUiState.RESOLVING)

    private val _authInFlight = MutableStateFlow(false)
    val authInFlight: StateFlow<Boolean> = _authInFlight.asStateFlow()

    private val _authError = MutableStateFlow(false)
    val authError: StateFlow<Boolean> = _authError.asStateFlow()

    fun authenticate(strings: BiometricPromptStrings) {
        if (_authInFlight.value) return
        viewModelScope.launch {
            _authInFlight.value = true
            _authError.value = false
            controller.markAuthStarted()
            try {
                when (biometricAuthenticator.authenticate(strings)) {
                    BiometricResult.Success -> controller.markUnlocked()
                    BiometricResult.Cancelled -> Unit // Stay locked; user can retry via the button.
                    is BiometricResult.Error -> _authError.value = true
                }
            } finally {
                controller.markAuthEnded()
                _authInFlight.value = false
            }
        }
    }

    /** Called on a fresh sign-in/guest entry so the just-authenticated session isn't re-locked. */
    fun onSignedIn() = controller.markUnlocked()

    fun onEnteredBackground() = controller.onEnteredBackground()

    fun onEnteredForeground() = controller.onEnteredForeground(GRACE_PERIOD)

    /** Escape hatch from the lock screen: clear the local session and drop the lock. */
    fun signOut() {
        viewModelScope.launch {
            sessionStorage.set(null)
            controller.markUnlocked()
        }
    }

    private companion object {
        // Short app-switches shouldn't re-prompt; longer absences re-lock.
        val GRACE_PERIOD = 30.seconds
    }
}
