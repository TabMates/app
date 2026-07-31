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
}
