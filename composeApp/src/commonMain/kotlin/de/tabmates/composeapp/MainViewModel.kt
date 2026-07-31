package de.tabmates.composeapp

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.navigation3.runtime.NavKey
import de.tabmates.core.domain.auth.AuthInfo
import de.tabmates.core.domain.auth.SessionStorage
import de.tabmates.core.domain.auth.StaleSession
import de.tabmates.core.domain.auth.StaleSessionStore
import de.tabmates.core.domain.preferences.AppPreferencesRepository
import de.tabmates.core.domain.preferences.ThemeMode
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import org.koin.core.annotation.KoinViewModel

@KoinViewModel
class MainViewModel(
    sessionStorage: SessionStorage,
    staleSessionStore: StaleSessionStore,
    appPreferencesRepository: AppPreferencesRepository,
) : ViewModel() {
    val sessionState: StateFlow<SessionShellState> =
        combine(sessionStorage.authState, staleSessionStore.state, ::shellStateOf)
            .stateIn(
                viewModelScope,
                SharingStarted.Eagerly,
                // Seeded synchronously so the first frame picks the right start destination
                // instead of flashing the welcome screen.
                shellStateOf(sessionStorage.get(), staleSessionStore.get()),
            )

    val themeMode: StateFlow<ThemeMode> =
        appPreferencesRepository
            .themeMode()
            .stateIn(viewModelScope, SharingStarted.Eagerly, ThemeMode.SYSTEM)

    private var pendingPostAuthNavKey: NavKey? = null

    fun setPendingPostAuthNavKey(navKey: NavKey?) {
        pendingPostAuthNavKey = navKey
    }

    fun consumePendingPostAuthNavKey(): NavKey? {
        val value = pendingPostAuthNavKey
        pendingPostAuthNavKey = null
        return value
    }
}

private fun shellStateOf(
    authInfo: AuthInfo?,
    staleSession: StaleSession?,
): SessionShellState =
    when {
        authInfo != null -> SessionShellState.Active
        staleSession != null -> SessionShellState.Stale
        else -> SessionShellState.SignedOut
    }
