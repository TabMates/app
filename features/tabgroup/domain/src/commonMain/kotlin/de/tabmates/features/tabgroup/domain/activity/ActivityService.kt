package de.tabmates.features.tabgroup.domain.activity

import de.tabmates.core.domain.util.DataError
import de.tabmates.core.domain.util.Result

/**
 * Remote contract for the account-wide activity log (`GET /api/activity`).
 *
 * Pages ascend by `seq` and [since] is **exclusive** (`seq > since`), so the caller stores the last
 * `seq` it saw and passes it back verbatim. `null` starts from the beginning of every group the user
 * currently belongs to, including events that predate their join.
 */
interface ActivityService {
    suspend fun getActivityFeed(
        since: Long?,
        limit: Int,
    ): Result<ActivityFeedPage, DataError.Remote>
}

data class ActivityFeedPage(
    val events: List<ActivityEvent>,
    /** Highest `seq` in this page; null when the page is empty. */
    val nextCursor: Long?,
    val hasMore: Boolean,
)
