package de.tabmates.core.domain.sync

import kotlin.time.Instant

/**
 * Persists the delta-sync watermark (`serverTime`) returned by the last successful `/api/sync`
 * call. The stored value is echoed back as the `since` query param on the next sync so the server
 * returns only changes since then. Absent (null) means "no successful sync yet" → next sync is a
 * full snapshot.
 */
interface SyncCursorStore {
    /** The cursor from the last successful sync, or null if none has completed. */
    fun get(): Instant?

    /** Stores [cursor] as the new watermark. */
    fun set(cursor: Instant)

    /** Drops the watermark (e.g. on logout) so the next sync starts from a full snapshot. */
    fun clear()
}
