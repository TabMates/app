package de.tabmates.core.data.auth

import de.tabmates.core.data.security.SecureStore
import de.tabmates.core.domain.auth.StaleSession
import de.tabmates.core.domain.auth.StaleSessionStore
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.koin.core.annotation.Single

/**
 * Encrypted rather than plaintext (unlike the sync cursors) because the record holds the user's
 * email address. Mirrors the stored value into a [MutableStateFlow] exactly as
 * [KSafeSessionStorage] does, so the shell can observe it alongside the session.
 */
@Single(binds = [StaleSessionStore::class])
class KSafeStaleSessionStore(
    secureStore: SecureStore,
) : StaleSessionStore {
    private var staleSession by secureStore<StaleSession?>(null, key = "staleSession")
    private val _state = MutableStateFlow(staleSession)

    override val state: StateFlow<StaleSession?> = _state.asStateFlow()

    override fun get(): StaleSession? = staleSession

    override fun set(session: StaleSession?) {
        staleSession = session
        _state.value = session
    }

    override fun clear() = set(null)
}
