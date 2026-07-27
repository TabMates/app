package de.tabmates.features.tabgroup.domain.activity

import de.tabmates.core.domain.util.DataError
import de.tabmates.core.domain.util.EmptyResult
import kotlinx.coroutines.flow.Flow

/**
 * Mirrors the server's activity log into the local database and reads it back merged with
 * not-yet-synced local writes.
 *
 * The mirror is complete rather than windowed, so the feed reads offline all the way back to a
 * group's creation; a group's rows are dropped only when the user leaves it.
 */
interface ActivityRepository {
    /** Pulls every page newer than the stored cursor. Idempotent — safe to call on every reconnect. */
    suspend fun sync(): EmptyResult<DataError.Remote>

    /**
     * The newest [limit] confirmed events across every group, preceded by every pending local write.
     * Pending rows ignore [limit]: there are few of them and they always sit on top.
     */
    fun observeAccountFeed(limit: Int): Flow<List<ActivityFeedItem>>

    fun observeGroupFeed(
        groupId: String,
        limit: Int,
    ): Flow<List<ActivityFeedItem>>
}
