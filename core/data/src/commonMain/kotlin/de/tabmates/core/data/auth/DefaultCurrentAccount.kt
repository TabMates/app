package de.tabmates.core.data.auth

import de.tabmates.core.domain.auth.CurrentAccount
import de.tabmates.core.domain.auth.SessionStorage
import de.tabmates.core.domain.auth.StaleSessionStore
import org.koin.core.annotation.Single

@Single(binds = [CurrentAccount::class])
class DefaultCurrentAccount(
    private val sessionStorage: SessionStorage,
    private val staleSessionStore: StaleSessionStore,
) : CurrentAccount {
    // The stale record is the fallback, not the primary: while a session is live it is the only
    // thing that can have changed accounts.
    override fun userId(): String? = sessionStorage.get()?.user?.id ?: staleSessionStore.get()?.userId
}
