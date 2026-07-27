package de.tabmates.features.tabgroup.database.entities

/**
 * What the server has confirmed about one tab entry, aggregated from its activity events.
 *
 * Used to retire a pending outbox row once the server's own event for that write has arrived. Deletion
 * is tracked separately from [maxVersion] rather than folded into it: a delete retires a pending
 * delete whatever version it carried, and conflating the two would need a sentinel version.
 */
data class ConfirmedEntryVersion(
    val tabEntryId: String,
    /** Highest `entryVersion` seen for this entry; null if every event for it carried no version. */
    val maxVersion: Int?,
    val deleted: Boolean,
)
