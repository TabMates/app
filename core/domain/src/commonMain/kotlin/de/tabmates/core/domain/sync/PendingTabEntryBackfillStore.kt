package de.tabmates.core.domain.sync

/**
 * Persists the ids of groups whose tab-entry history still needs a backfill fetch. Delta sync
 * (`/api/sync?since=...`) filters entries by `lastModifiedAt >= since`, so a group joined after
 * the cursor was established arrives without its pre-existing entries; those are fetched
 * separately per group, and a marker here survives fetch failures so the next sync retries.
 */
interface PendingTabEntryBackfillStore {
    /** All group ids currently awaiting a (re)tried backfill. */
    fun getAll(): Set<String>

    /** Marks [groupId] as needing a backfill. */
    fun add(groupId: String)

    /** Clears the marker for [groupId] after a successful backfill. */
    fun remove(groupId: String)

    /** Drops markers for groups the user is no longer a member of. */
    fun retainAll(activeGroupIds: Set<String>)

    /** Drops all markers (e.g. on logout, or after a full sync made them moot). */
    fun clearAll()
}
