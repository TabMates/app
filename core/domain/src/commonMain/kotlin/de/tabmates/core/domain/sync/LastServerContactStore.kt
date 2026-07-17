package de.tabmates.core.domain.sync

import kotlinx.coroutines.flow.StateFlow
import kotlin.time.Instant

/**
 * Persists the last moment local data was known to match the server: bumped after a successful
 * `/api/sync` and when the realtime socket drops (data was live right up to the drop). Absent
 * (null) means "never synced". Powers the offline "last synced X ago" indicator.
 */
interface LastServerContactStore {
    /** The last moment local data was known fresh, or null if no sync has ever completed. */
    val lastContactAt: StateFlow<Instant?>

    /** Records "now" as the last server contact. */
    fun recordContactNow()

    /** Drops the timestamp (e.g. on logout) so a new session starts without a stale value. */
    fun clear()
}
