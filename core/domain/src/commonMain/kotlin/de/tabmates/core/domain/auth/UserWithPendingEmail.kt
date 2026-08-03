package de.tabmates.core.domain.auth

/**
 * A [User] together with an address they have been mailed a confirmation link for and not opened
 * yet: the migration target for an anonymous user, the change-email target for a registered one.
 * Read [pendingEmail] together with [User.userType] to tell the two apart — it is null when there
 * is nothing outstanding.
 *
 * Unlike the [User] cached in [SessionStorage], this is always re-read from the server, so it also
 * reports what was confirmed on another device.
 */
data class UserWithPendingEmail(
    val user: User,
    val pendingEmail: String?,
)
