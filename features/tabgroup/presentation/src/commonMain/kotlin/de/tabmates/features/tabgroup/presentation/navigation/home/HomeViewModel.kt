package de.tabmates.features.tabgroup.presentation.navigation.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import de.tabmates.core.domain.auth.SessionStorage
import de.tabmates.core.domain.util.onSuccess
import de.tabmates.features.authentication.domain.AuthService
import kotlinx.coroutines.launch
import org.koin.core.annotation.KoinViewModel

@KoinViewModel
class HomeViewModel(
    private val authService: AuthService,
    private val sessionStorage: SessionStorage,
) : ViewModel() {
    fun onLogoutClick() {
        viewModelScope.launch {
            authService
                .logout(sessionStorage.get()?.refreshToken.orEmpty())
                .onSuccess {
                    sessionStorage.set(null)
                }
        }
    }
}
