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
    /**
     * Order matters and is not arbitrary. The session is the only place the account identity
     * lives, so it has to be read before it is dropped — and the record has to be *written* first
     * too: a crash between the two steps then leaves a stale record with no session, which reads
     * as an expired session and keeps the data. Clearing first would lose the record and make the
     * next launch wipe the device.
     */
    override fun invalidate(reason: SessionInvalidationReason) {
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
