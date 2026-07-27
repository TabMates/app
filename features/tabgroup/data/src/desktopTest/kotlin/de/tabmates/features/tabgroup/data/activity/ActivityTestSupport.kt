package de.tabmates.features.tabgroup.data.activity

import de.tabmates.core.domain.sync.ActivityCursorStore
import de.tabmates.core.domain.util.DataError
import de.tabmates.core.domain.util.Result
import de.tabmates.features.tabgroup.database.entities.ActivityEventEntity
import de.tabmates.features.tabgroup.database.entities.ActivityFieldChangeEntity
import de.tabmates.features.tabgroup.database.entities.types.ActivityEventTypeDatabase
import de.tabmates.features.tabgroup.database.entities.types.ActivityFieldDatabase
import de.tabmates.features.tabgroup.domain.activity.ActivityEvent
import de.tabmates.features.tabgroup.domain.activity.ActivityEventType
import de.tabmates.features.tabgroup.domain.activity.ActivityFeedPage
import de.tabmates.features.tabgroup.domain.activity.ActivityService
import kotlin.time.Instant

class FakeActivityService(
    var pages: MutableList<Result<ActivityFeedPage, DataError.Remote>> = mutableListOf(),
) : ActivityService {
    val receivedSince: MutableList<Long?> = mutableListOf()

    override suspend fun getActivityFeed(
        since: Long?,
        limit: Int,
    ): Result<ActivityFeedPage, DataError.Remote> {
        receivedSince += since
        return if (pages.isEmpty()) {
            Result.Success(ActivityFeedPage(events = emptyList(), nextCursor = null, hasMore = false))
        } else {
            pages.removeAt(0)
        }
    }
}

class FakeActivityCursorStore(
    private var cursor: Long? = null,
) : ActivityCursorStore {
    override fun get(): Long? = cursor

    override fun set(cursor: Long) {
        this.cursor = cursor
    }

    override fun clear() {
        cursor = null
    }
}

fun activityEvent(
    id: String,
    seq: Long,
    groupId: String,
    type: ActivityEventType = ActivityEventType.ENTRY_CREATED,
    tabEntryId: String? = "e1",
    entryVersion: Int? = 0,
    occurredAt: Instant = Instant.fromEpochMilliseconds(seq * 1_000),
): ActivityEvent =
    ActivityEvent(
        id = id,
        seq = seq,
        groupId = groupId,
        occurredAt = occurredAt,
        actorUserId = "u1",
        type = type,
        tabEntryId = tabEntryId,
        entryTitle = "Entry $tabEntryId",
        amount = 10.0,
        currencyCode = "EUR",
        entryVersion = entryVersion,
    )

fun activityEventEntity(
    id: String,
    seq: Long,
    groupId: String,
    type: ActivityEventTypeDatabase = ActivityEventTypeDatabase.ENTRY_CREATED,
    tabEntryId: String? = "e1",
    entryVersion: Int? = 0,
): ActivityEventEntity =
    ActivityEventEntity(
        id = id,
        seq = seq,
        groupId = groupId,
        occurredAt = seq * 1_000,
        actorUserId = "u1",
        type = type,
        tabEntryId = tabEntryId,
        entryTitle = "Entry $tabEntryId",
        amount = 10.0,
        currencyCode = "EUR",
        entryVersion = entryVersion,
    )

fun changeEntity(
    eventId: String,
    field: ActivityFieldDatabase = ActivityFieldDatabase.TITLE,
    oldValue: String? = "before",
    newValue: String? = "after",
): ActivityFieldChangeEntity =
    ActivityFieldChangeEntity(
        activityEventId = eventId,
        field = field,
        oldValue = oldValue,
        newValue = newValue,
    )
