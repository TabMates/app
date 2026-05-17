package de.tabmates.composeapp

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.navigation3.runtime.NavKey
import de.tabmates.core.domain.auth.SessionStorage
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import org.koin.core.annotation.KoinViewModel

@KoinViewModel
class MainViewModel(
    sessionStorage: SessionStorage,
) : ViewModel() {
    val isLoggedIn: StateFlow<Boolean> = sessionStorage.authState.map { it != null }
        .stateIn(viewModelScope, SharingStarted.Eagerly, sessionStorage.get() != null)

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
