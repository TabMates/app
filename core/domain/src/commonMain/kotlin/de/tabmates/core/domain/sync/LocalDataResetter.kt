package de.tabmates.core.domain.sync

/**
 * Wipes every trace of the signed-in account's data from the device: groups (and everything that
 * FK-cascades off them), sync cursors, and queued outbox writes.
 *
 * The single wipe path, so that "what does signing out actually delete" has one answer. Lives in
 * `core.domain` because the re-auth screen needs it and `:features:authentication:presentation`
 * must not depend on `:features:tabgroup:*`.
 */
interface LocalDataResetter {
    suspend fun resetLocalData()

    /**
     * Additionally drops the account-independent server data (currencies, exchange rates).
     *
     * Deliberately not part of [resetLocalData]: signing out does not invalidate that data, it is
     * the same on the next sign-in. Switching backends does — the new one has its own currency
     * list and rates, and the cached ones would otherwise be shown until the daily refresh.
     */
    suspend fun resetReferenceData()
}
