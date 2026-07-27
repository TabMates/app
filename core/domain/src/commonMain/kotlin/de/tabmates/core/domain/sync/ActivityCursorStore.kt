package de.tabmates.core.domain.sync

/**
 * Persists the highest activity-log `seq` mirrored so far. Echoed back as the `since` query param,
 * which the server treats as **exclusive** (`seq > since`), so the stored value is the last `seq`
 * actually seen — never decremented.
 *
 * Absent (null) means "nothing mirrored yet" → the next sync pulls each group's full history.
 */
interface ActivityCursorStore {
    fun get(): Long?

    fun set(cursor: Long)

    /** Drops the cursor (e.g. on logout), where the mirrored rows go with the deleted groups. */
    fun clear()
}
