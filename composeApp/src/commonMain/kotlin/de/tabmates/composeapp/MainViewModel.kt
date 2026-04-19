package de.tabmates.composeapp

import androidx.lifecycle.ViewModel
import de.tabmates.core.domain.auth.SessionStorage
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.SharingStarted
import androidx.lifecycle.viewModelScope

class MainViewModel(
    sessionStorage: SessionStorage,
) : ViewModel() {
    val isLoggedIn: StateFlow<Boolean> = sessionStorage.authState.map { it != null }
        .stateIn(viewModelScope, SharingStarted.Eagerly, sessionStorage.get() != null)
}
