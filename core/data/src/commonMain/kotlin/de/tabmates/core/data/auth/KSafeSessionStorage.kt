package de.tabmates.core.data.auth

import de.tabmates.core.data.security.SecureStore
import de.tabmates.core.domain.auth.AuthInfo
import de.tabmates.core.domain.auth.SessionStorage
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.koin.core.annotation.Single

@Single(binds = [SessionStorage::class])
class KSafeSessionStorage(
    secureStore: SecureStore,
) : SessionStorage {
    private var authInfo by secureStore<AuthInfo?>(null, key = "authInfo")
    private val _authState = MutableStateFlow(authInfo)

    override val authState: StateFlow<AuthInfo?> = _authState.asStateFlow()

    override fun get(): AuthInfo? = authInfo

    override fun set(info: AuthInfo?) {
        authInfo = info
        _authState.value = info
    }
}
