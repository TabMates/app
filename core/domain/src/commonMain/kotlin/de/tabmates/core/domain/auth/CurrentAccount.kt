package de.tabmates.core.domain.auth

/**
 * Who the data on this device belongs to.
 *
 * Deliberately not the same question as [SessionStorage], which only knows whether there are
 * *usable credentials*. An expired session has none, but the groups, entries and queued writes are
 * still there and every balance is computed relative to their owner. Deriving the id from the
 * session alone makes an expired session look like a stranger picked up the phone: no split
 * matches the viewer, so every group reads as settled and every expense as not involved.
 */
interface CurrentAccount {
    /** The owning account's id, or null when the device holds no account at all. */
    fun userId(): String?
}
