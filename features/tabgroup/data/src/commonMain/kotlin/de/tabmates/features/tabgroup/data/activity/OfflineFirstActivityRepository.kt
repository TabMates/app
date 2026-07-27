package de.tabmates.features.tabgroup.data.activity

import de.tabmates.core.domain.logging.TabMatesLogger
import de.tabmates.core.domain.sync.ActivityCursorStore
import de.tabmates.core.domain.util.DataError
import de.tabmates.core.domain.util.EmptyResult
import de.tabmates.core.domain.util.Result
import de.tabmates.features.tabgroup.data.mappers.toActivityEntryType
import de.tabmates.features.tabgroup.data.mappers.toChangeEntities
import de.tabmates.features.tabgroup.data.mappers.toDomain
import de.tabmates.features.tabgroup.data.mappers.toEntity
import de.tabmates.features.tabgroup.data.network.dto.NewTabEntryWsPayload
import de.tabmates.features.tabgroup.data.network.dto.WebSocketMessageDto
import de.tabmates.features.tabgroup.data.tabentry.TabEntryOutbox
import de.tabmates.features.tabgroup.data.tabentry.decodePendingDeletePayload
import de.tabmates.features.tabgroup.database.TabMatesDatabase
import de.tabmates.features.tabgroup.database.entities.ConfirmedEntryVersion
import de.tabmates.features.tabgroup.database.entities.PendingOutboxEntity
import de.tabmates.features.tabgroup.domain.activity.ActivityEntryType
import de.tabmates.features.tabgroup.domain.activity.ActivityEventType
import de.tabmates.features.tabgroup.domain.activity.ActivityFeedItem
import de.tabmates.features.tabgroup.domain.activity.ActivityRepository
import de.tabmates.features.tabgroup.domain.activity.ActivityService
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.serialization.json.Json
import org.koin.core.annotation.Single
import kotlin.time.Instant

@Single(binds = [ActivityRepository::class])
class OfflineFirstActivityRepository(
    private val activityService: ActivityService,
    private val database: TabMatesDatabase,
    private val cursorStore: ActivityCursorStore,
    private val json: Json,
    private val logger: TabMatesLogger,
) : ActivityRepository {
    // Serializes the login and reconnect triggers so two runs can't interleave on the cursor.
    private val mutex = Mutex()

    /**
     * Walks every page past the stored cursor.
     *
     * The cursor is committed **after each page**, not once at the end: a first login pulls each
     * group's full pre-join history, so a crash or a dropped connection mid-run must cost one page
     * rather than the entire account. Replay is harmless either way — `upsertPage` is idempotent —
     * but re-downloading everything is not free.
     */
    override suspend fun sync(): EmptyResult<DataError.Remote> =
        mutex.withLock {
            while (true) {
                val since = cursorStore.get()
                val page =
                    when (val result = activityService.getActivityFeed(since = since, limit = PAGE_SIZE)) {
                        is Result.Failure -> {
                            logger.warning(TAG, "Activity sync failed at since=$since: ${result.error}")
                            return@withLock Result.Failure(result.error)
                        }

                        is Result.Success -> {
                            result.data
                        }
                    }

                if (page.events.isEmpty()) break

                database.activityEventDao.upsertPage(
                    events = page.events.map { it.toEntity() },
                    changes = page.events.flatMap { it.toChangeEntities() },
                )
                page.nextCursor?.let { cursorStore.set(it) }

                // A server reporting more but handing back no cursor would spin on the same page
                // forever; stop and let the next trigger retry instead.
                if (!page.hasMore || page.nextCursor == null) break
            }
            Result.Success(Unit)
        }

    override fun observeAccountFeed(limit: Int): Flow<List<ActivityFeedItem>> =
        combine(
            database.activityEventDao.observeAccountFeed(limit),
            observePendingItems(),
        ) { persisted, pending ->
            pending + persisted.map { ActivityFeedItem.Persisted(it.toDomain()) }
        }

    override fun observeGroupFeed(
        groupId: String,
        limit: Int,
    ): Flow<List<ActivityFeedItem>> =
        combine(
            database.activityEventDao.observeGroupFeed(groupId, limit),
            observePendingItems(),
        ) { persisted, pending ->
            pending.filter { it.groupId == groupId } +
                persisted.map { ActivityFeedItem.Persisted(it.toDomain()) }
        }

    /**
     * The outbox rendered as feed rows, minus the ones the server has already confirmed.
     *
     * Keyed on the pending ids rather than aggregating the whole log: there are never more than a
     * handful of pending writes, and re-running a full aggregate on every mirrored event would be
     * wasteful.
     */
    @OptIn(ExperimentalCoroutinesApi::class)
    private fun observePendingItems(): Flow<List<ActivityFeedItem.Pending>> =
        database.pendingOutboxDao
            .observeAll()
            .map { rows -> rows.mapNotNull { decodePendingWrite(it) } }
            .distinctUntilChanged()
            .flatMapLatest { writes ->
                if (writes.isEmpty()) {
                    flowOf(emptyList())
                } else {
                    database.activityEventDao
                        .observeConfirmedEntryVersions(writes.map { it.item.tabEntryId }.distinct())
                        .map { confirmed ->
                            val byEntryId = confirmed.associateBy { it.tabEntryId }
                            writes
                                .filterNot { it.isConfirmedBy(byEntryId[it.item.tabEntryId]) }
                                .sortedByDescending { it.item.occurredAt }
                                .map { it.item }
                        }
                }
            }

    /**
     * A queued write plus the version it will produce, which is what retires it: the server records
     * `entryVersion` *after* the mutation, so a create confirms as 0 and the first edit as 1.
     */
    private data class PendingWrite(
        val item: ActivityFeedItem.Pending,
        val expectedVersion: Int?,
    ) {
        fun isConfirmedBy(confirmed: ConfirmedEntryVersion?): Boolean {
            if (confirmed == null) return false
            return when (item.type) {
                // A delete is terminal: any ENTRY_DELETED for the entry retires it, whatever version.
                ActivityEventType.ENTRY_DELETED -> {
                    confirmed.deleted
                }

                // A server-side delete also retires a queued edit — that version can never arrive.
                else -> {
                    confirmed.deleted ||
                        when {
                            // Written before expectedVersion existed: any confirmed event will do.
                            expectedVersion == null -> confirmed.maxVersion != null

                            else -> (confirmed.maxVersion ?: -1) >= expectedVersion
                        }
                }
            }
        }
    }

    private fun decodePendingWrite(row: PendingOutboxEntity): PendingWrite? =
        try {
            when (row.type) {
                TabEntryOutbox.OUTBOX_TYPE_NEW_TAB_ENTRY -> decodeUpsert(row, ActivityEventType.ENTRY_CREATED)
                TabEntryOutbox.OUTBOX_TYPE_TAB_ENTRY_UPDATE -> decodeUpsert(row, ActivityEventType.ENTRY_UPDATED)
                TabEntryOutbox.OUTBOX_TYPE_TAB_ENTRY_DELETE -> decodeDelete(row)
                else -> null
            }
        } catch (e: Throwable) {
            // A row this build cannot read must not take the whole feed down with it.
            logger.warning(TAG, "Skipping unreadable outbox row id=${row.id}: ${e.message}")
            null
        }

    private fun decodeUpsert(
        row: PendingOutboxEntity,
        type: ActivityEventType,
    ): PendingWrite {
        val envelope = json.decodeFromString(WebSocketMessageDto.serializer(), row.payload)
        val payload = json.decodeFromString(NewTabEntryWsPayload.serializer(), envelope.payload)
        return PendingWrite(
            item =
                ActivityFeedItem.Pending(
                    outboxId = row.id,
                    tabEntryId = payload.id ?: row.id.removePrefix(TabEntryOutbox.UPDATE_ID_PREFIX),
                    type = type,
                    groupId = payload.groupId,
                    occurredAt = Instant.fromEpochMilliseconds(row.createdAt),
                    entryType = payload.activityEntryType(),
                    entryTitle = payload.title,
                    amount = payload.amount,
                    currencyCode = payload.currency,
                ),
            expectedVersion = row.expectedVersion,
        )
    }

    private fun decodeDelete(row: PendingOutboxEntity): PendingWrite {
        val payload = json.decodePendingDeletePayload(row.payload)
        return PendingWrite(
            item =
                ActivityFeedItem.Pending(
                    outboxId = row.id,
                    tabEntryId = payload.tabEntryId,
                    type = ActivityEventType.ENTRY_DELETED,
                    groupId = payload.groupId,
                    occurredAt = Instant.fromEpochMilliseconds(row.createdAt),
                    entryType = payload.entryType?.toActivityEntryType(),
                    entryTitle = payload.title,
                    amount = payload.amount,
                    currencyCode = payload.currencyCode,
                ),
            expectedVersion = row.expectedVersion,
        )
    }

    private fun NewTabEntryWsPayload.activityEntryType(): ActivityEntryType =
        when (this) {
            is NewTabEntryWsPayload.Expense -> ActivityEntryType.EXPENSE
            is NewTabEntryWsPayload.Income -> ActivityEntryType.INCOME
            is NewTabEntryWsPayload.Settlement -> ActivityEntryType.SETTLEMENT
        }

    private companion object {
        private const val TAG = "OfflineFirstActivityRepository"

        /** The server's own default; it clamps anything above 500. */
        private const val PAGE_SIZE = 200
    }
}
