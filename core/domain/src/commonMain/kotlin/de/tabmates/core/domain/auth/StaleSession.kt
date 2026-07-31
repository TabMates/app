package de.tabmates.core.domain.auth

import kotlinx.serialization.Serializable

/**
 * The account whose session died while local data was still unsynced.
 *
 * Recorded just before [SessionStorage] is cleared so the app knows *who* to ask back in: the
 * outbox can only drain under the same account, and the local database is only safe to keep while
 * that account is the one signing in again.
 *
 * [email] is null when the address is no longer known to be current — confirming an email change
 * revokes the refresh tokens server-side, so the stored address is the old one. The re-auth screen
 * then asks for the address instead of locking it, and falls back to matching on [userId].
 */
@Serializable
data class StaleSession(
    val userId: String,
    val email: String?,
    val username: String,
    val userType: UserType,
)
