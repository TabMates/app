package de.tabmates.core.data.auth

import de.tabmates.core.domain.auth.SessionInvalidationReason
import de.tabmates.core.domain.auth.SessionInvalidator
import de.tabmates.core.domain.auth.SessionStorage
import de.tabmates.core.domain.auth.StaleSession
import de.tabmates.core.domain.auth.StaleSessionStore
import org.koin.core.annotation.Single

@Single(binds = [SessionInvalidator::class])
class DefaultSessionInvalidator(
    private val sessionStorage: SessionStorage,
    private val staleSessionStore: StaleSessionStore,
) : SessionInvalidator {
    override fun invalidate(reason: SessionInvalidationReason) {
        // Read before clearing: the session is the only place the account identity lives.
        sessionStorage.get()?.user?.let { user ->
            staleSessionStore.set(
                StaleSession(
                    userId = user.id,
                    email = user.email.takeIf { reason != SessionInvalidationReason.EMAIL_CHANGED },
                    username = user.username,
                    userType = user.userType,
                ),
            )
        }
        sessionStorage.set(null)
    }
}
