package de.tabmates.core.domain.auth

enum class SessionInvalidationReason {
    /** The server rejected the refresh token. The account's email is still valid to sign in with. */
    TOKEN_REJECTED,

    /**
     * The user confirmed an email change, which revokes all refresh tokens server-side. The stored
     * address is now the *old* one, so it must not be offered back.
     */
    EMAIL_CHANGED,
}

/**
 * Ends the session without losing track of whose it was.
 *
 * The only sanctioned way to clear an involuntarily-ended session: it records a [StaleSession]
 * *before* clearing [SessionStorage], which is what stops [de.tabmates.core.domain.sync.LocalDataResetter]
 * from wiping local data and lets the app ask the same account back in. Deliberate sign-out and
 * account deletion do **not** go through here — those should wipe.
 */
interface SessionInvalidator {
    fun invalidate(reason: SessionInvalidationReason)
}
