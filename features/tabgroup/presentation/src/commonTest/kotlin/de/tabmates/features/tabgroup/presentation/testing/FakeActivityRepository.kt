package de.tabmates.features.tabgroup.presentation.testing

import de.tabmates.core.domain.util.DataError
import de.tabmates.core.domain.util.EmptyResult
import de.tabmates.core.domain.util.Result
import de.tabmates.features.tabgroup.domain.activity.ActivityFeedItem
import de.tabmates.features.tabgroup.domain.activity.ActivityRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map

class FakeActivityRepository(
    initialFeed: List<ActivityFeedItem> = emptyList(),
) : ActivityRepository {
    private val feed = MutableStateFlow(initialFeed)

    /** Every limit the subject has asked for, in order — lets a test assert `loadMore()` took effect. */
    val requestedLimits: MutableList<Int> = mutableListOf()
    var syncCount: Int = 0
        private set
    var syncResult: EmptyResult<DataError.Remote> = Result.Success(Unit)

    fun emit(items: List<ActivityFeedItem>) {
        feed.value = items
    }

    override suspend fun sync(): EmptyResult<DataError.Remote> {
        syncCount++
        return syncResult
    }

    override fun observeAccountFeed(limit: Int): Flow<List<ActivityFeedItem>> {
        requestedLimits += limit
        return feed.map { items -> items.take(limit) }
    }

    override fun observeGroupFeed(
        groupId: String,
        limit: Int,
    ): Flow<List<ActivityFeedItem>> {
        requestedLimits += limit
        return feed.map { items -> items.filter { it.groupId == groupId }.take(limit) }
    }
}
