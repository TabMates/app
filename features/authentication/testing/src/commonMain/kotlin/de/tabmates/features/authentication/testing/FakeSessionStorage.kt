package de.tabmates.features.authentication.testing

import de.tabmates.core.domain.auth.AuthInfo
import de.tabmates.core.domain.auth.SessionStorage
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

class FakeSessionStorage(
    initial: AuthInfo? = null,
) : SessionStorage {
    private val state = MutableStateFlow(initial)

    override val authState: StateFlow<AuthInfo?> = state

    override fun get(): AuthInfo? = state.value

    override fun set(info: AuthInfo?) {
        state.value = info
    }
}
