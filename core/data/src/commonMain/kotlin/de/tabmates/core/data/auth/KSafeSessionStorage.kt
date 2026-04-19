package de.tabmates.core.data.auth

import de.tabmates.core.domain.auth.AuthInfo
import de.tabmates.core.domain.auth.SessionStorage
import eu.anifantakis.lib.ksafe.KSafe
import eu.anifantakis.lib.ksafe.invoke
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class KSafeSessionStorage(vault: KSafe) : SessionStorage {
    private var authInfo by vault<AuthInfo?>(null, key = "authInfo")
    private val _authState = MutableStateFlow(authInfo)

    override val authState: StateFlow<AuthInfo?> = _authState.asStateFlow()

    override fun get(): AuthInfo? = authInfo

    override fun set(info: AuthInfo?) {
        authInfo = info
        _authState.value = info
    }
}
