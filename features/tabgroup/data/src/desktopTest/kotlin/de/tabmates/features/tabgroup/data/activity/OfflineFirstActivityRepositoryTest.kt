package de.tabmates.features.tabgroup.data.activity

import de.tabmates.core.domain.util.DataError
import de.tabmates.core.domain.util.Result
import de.tabmates.features.tabgroup.data.network.dto.NewTabEntryWsPayload
import de.tabmates.features.tabgroup.data.network.dto.WebSocketMessageDto
import de.tabmates.features.tabgroup.data.network.dto.WsMessageType
import de.tabmates.features.tabgroup.data.sync.NoopLogger
import de.tabmates.features.tabgroup.data.sync.createInMemoryDatabase
import de.tabmates.features.tabgroup.data.sync.group
import de.tabmates.features.tabgroup.data.sync.insertGroup
import de.tabmates.features.tabgroup.data.tabentry.PendingDeletePayload
import de.tabmates.features.tabgroup.data.tabentry.TabEntryOutbox
import de.tabmates.features.tabgroup.database.TabMatesDatabase
import de.tabmates.features.tabgroup.database.entities.PendingOutboxEntity
import de.tabmates.features.tabgroup.database.entities.types.ActivityEventTypeDatabase
import de.tabmates.features.tabgroup.domain.activity.ActivityEventType
import de.tabmates.features.tabgroup.domain.activity.ActivityFeedItem
import de.tabmates.features.tabgroup.domain.activity.ActivityFeedPage
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import kotlinx.datetime.LocalDate
import kotlinx.serialization.json.Json
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertNull
import kotlin.test.assertTrue

class OfflineFirstActivityRepositoryTest {
    private val json = Json { ignoreUnknownKeys = true }

    @Test
    fun syncWalksEveryPageAndCommitsTheCursorPerPage() =
        runTest {
            val database = createInMemoryDatabase()
            database.insertGroup(group(id = "g1"))
            val service =
                FakeActivityService(
                    mutableListOf(
                        Result.Success(
                            ActivityFeedPage(
                                events = listOf(activityEvent("a1", seq = 1, groupId = "g1")),
                                nextCursor = 1,
                                hasMore = true,
                            ),
                        ),
                        Result.Success(
                            ActivityFeedPage(
                                events = listOf(activityEvent("a2", seq = 2, groupId = "g1")),
                                nextCursor = 2,
                                hasMore = false,
                            ),
                        ),
                    ),
                )
            val cursorStore = FakeActivityCursorStore()

            val result = repository(database, service, cursorStore).sync()

            assertIs<Result.Success<Unit>>(result)
            assertEquals(listOf(null, 1L), service.receivedSince)
            assertEquals(2L, cursorStore.get())
            assertEquals(2L, database.activityEventDao.getMaxSeq())
        }

    @Test
    fun syncKeepsTheCursorFromTheLastGoodPageWhenALaterPageFails() =
        runTest {
            val database = createInMemoryDatabase()
            database.insertGroup(group(id = "g1"))
            val service =
                FakeActivityService(
                    mutableListOf(
                        Result.Success(
                            ActivityFeedPage(
                                events = listOf(activityEvent("a1", seq = 1, groupId = "g1")),
                                nextCursor = 1,
                                hasMore = true,
                            ),
                        ),
                        Result.Failure(DataError.Remote.SERVER_ERROR),
                    ),
                )
            val cursorStore = FakeActivityCursorStore()

            val result = repository(database, service, cursorStore).sync()

            assertIs<Result.Failure<DataError.Remote>>(result)
            // The first page is committed, so the retry resumes from it rather than from scratch.
            assertEquals(1L, cursorStore.get())
        }

    @Test
    fun syncStopsWhenTheServerReportsMoreButHandsBackNoCursor() =
        runTest {
            val database = createInMemoryDatabase()
            database.insertGroup(group(id = "g1"))
            val service =
                FakeActivityService(
                    mutableListOf(
                        Result.Success(
                            ActivityFeedPage(
                                events = listOf(activityEvent("a1", seq = 1, groupId = "g1")),
                                nextCursor = null,
                                hasMore = true,
                            ),
                        ),
                    ),
                )
            val cursorStore = FakeActivityCursorStore()

            repository(database, service, cursorStore).sync()

            assertEquals(1, service.receivedSince.size)
            assertNull(cursorStore.get())
        }

    @Test
    fun accountFeedPutsPendingWritesAboveConfirmedEvents() =
        runTest {
            val database = createInMemoryDatabase()
            database.insertGroup(group(id = "g1"))
            database.activityEventDao.upsertPage(
                events = listOf(activityEventEntity(id = "a1", seq = 1, groupId = "g1")),
                changes = emptyList(),
            )
            database.insertPendingCreate(tabEntryId = "e2", groupId = "g1", expectedVersion = 0)

            val feed = repository(database).observeAccountFeed(limit = 10).first()

            assertEquals(2, feed.size)
            val pending = assertIs<ActivityFeedItem.Pending>(feed.first())
            assertEquals("e2", pending.tabEntryId)
            assertEquals(ActivityEventType.ENTRY_CREATED, pending.type)
            assertIs<ActivityFeedItem.Persisted>(feed[1])
        }

    @Test
    fun aPendingWriteDisappearsOnceTheServerConfirmsItsVersion() =
        runTest {
            val database = createInMemoryDatabase()
            database.insertGroup(group(id = "g1"))
            database.insertPendingUpdate(tabEntryId = "e1", groupId = "g1", expectedVersion = 1)

            val repository = repository(database)
            assertTrue(repository.observeAccountFeed(limit = 10).first().any { it is ActivityFeedItem.Pending })

            // The server records the version *after* the mutation, so the first edit confirms as 1.
            database.activityEventDao.upsertPage(
                events =
                    listOf(
                        activityEventEntity(
                            id = "a1",
                            seq = 1,
                            groupId = "g1",
                            tabEntryId = "e1",
                            entryVersion = 1,
                        ),
                    ),
                changes = emptyList(),
            )

            val feed = repository.observeAccountFeed(limit = 10).first()
            assertTrue(feed.none { it is ActivityFeedItem.Pending })
        }

    @Test
    fun anOlderConfirmedVersionDoesNotRetireAPendingEdit() =
        runTest {
            val database = createInMemoryDatabase()
            database.insertGroup(group(id = "g1"))
            database.insertPendingUpdate(tabEntryId = "e1", groupId = "g1", expectedVersion = 2)
            database.activityEventDao.upsertPage(
                events =
                    listOf(
                        activityEventEntity(
                            id = "a1",
                            seq = 1,
                            groupId = "g1",
                            tabEntryId = "e1",
                            entryVersion = 1,
                        ),
                    ),
                changes = emptyList(),
            )

            val feed = repository(database).observeAccountFeed(limit = 10).first()

            assertTrue(feed.any { it is ActivityFeedItem.Pending })
        }

    @Test
    fun aPendingDeleteCarriesItsSnapshotAndIsRetiredByAnyDeletion() =
        runTest {
            val database = createInMemoryDatabase()
            database.insertGroup(group(id = "g1"))
            database.insertPendingDelete(tabEntryId = "e1", groupId = "g1")

            val repository = repository(database)
            val pending =
                assertIs<ActivityFeedItem.Pending>(
                    repository.observeAccountFeed(limit = 10).first().first(),
                )
            assertEquals(ActivityEventType.ENTRY_DELETED, pending.type)
            assertEquals("Dinner", pending.entryTitle)
            assertEquals(12.5, pending.amount)

            database.activityEventDao.upsertPage(
                events =
                    listOf(
                        activityEventEntity(
                            id = "a1",
                            seq = 1,
                            groupId = "g1",
                            tabEntryId = "e1",
                            type = ActivityEventTypeDatabase.ENTRY_DELETED,
                            entryVersion = null,
                        ),
                    ),
                changes = emptyList(),
            )

            assertTrue(repository.observeAccountFeed(limit = 10).first().none { it is ActivityFeedItem.Pending })
        }

    @Test
    fun aLegacyDeleteRowWithABareIdPayloadStillRenders() =
        runTest {
            val database = createInMemoryDatabase()
            database.insertGroup(group(id = "g1"))
            database.pendingOutboxDao.upsert(
                PendingOutboxEntity(
                    id = "${TabEntryOutbox.DELETE_ID_PREFIX}e1",
                    type = TabEntryOutbox.OUTBOX_TYPE_TAB_ENTRY_DELETE,
                    payload = "e1",
                    createdAt = 1_000,
                ),
            )

            val pending =
                assertIs<ActivityFeedItem.Pending>(
                    repository(database).observeAccountFeed(limit = 10).first().single(),
                )

            assertEquals("e1", pending.tabEntryId)
            assertNull(pending.entryTitle)
        }

    @Test
    fun groupFeedExcludesOtherGroupsPendingAndConfirmedRows() =
        runTest {
            val database = createInMemoryDatabase()
            database.insertGroup(group(id = "g1"))
            database.insertGroup(group(id = "g2"))
            database.activityEventDao.upsertPage(
                events =
                    listOf(
                        activityEventEntity(id = "a1", seq = 1, groupId = "g1"),
                        activityEventEntity(id = "a2", seq = 2, groupId = "g2"),
                    ),
                changes = emptyList(),
            )
            database.insertPendingCreate(tabEntryId = "e9", groupId = "g2", expectedVersion = 0)

            val feed = repository(database).observeGroupFeed("g1", limit = 10).first()

            assertEquals(1, feed.size)
            assertEquals("g1", feed.single().groupId)
        }

    private fun repository(
        database: TabMatesDatabase,
        service: FakeActivityService = FakeActivityService(),
        cursorStore: FakeActivityCursorStore = FakeActivityCursorStore(),
    ) = OfflineFirstActivityRepository(
        activityService = service,
        database = database,
        cursorStore = cursorStore,
        json = json,
        logger = NoopLogger,
    )

    private suspend fun TabMatesDatabase.insertPendingCreate(
        tabEntryId: String,
        groupId: String,
        expectedVersion: Int?,
    ) = insertPendingUpsert(
        outboxId = tabEntryId,
        type = TabEntryOutbox.OUTBOX_TYPE_NEW_TAB_ENTRY,
        wsType = WsMessageType.NEW_TAB_ENTRY,
        tabEntryId = tabEntryId,
        groupId = groupId,
        expectedVersion = expectedVersion,
    )

    private suspend fun TabMatesDatabase.insertPendingUpdate(
        tabEntryId: String,
        groupId: String,
        expectedVersion: Int?,
    ) = insertPendingUpsert(
        outboxId = "${TabEntryOutbox.UPDATE_ID_PREFIX}$tabEntryId",
        type = TabEntryOutbox.OUTBOX_TYPE_TAB_ENTRY_UPDATE,
        wsType = WsMessageType.UPDATED_TAB_ENTRY,
        tabEntryId = tabEntryId,
        groupId = groupId,
        expectedVersion = expectedVersion,
    )

    private suspend fun TabMatesDatabase.insertPendingUpsert(
        outboxId: String,
        type: String,
        wsType: String,
        tabEntryId: String,
        groupId: String,
        expectedVersion: Int?,
    ) {
        val payload =
            NewTabEntryWsPayload.Expense(
                id = tabEntryId,
                groupId = groupId,
                paidByUserId = "u1",
                title = "Dinner",
                description = "",
                amount = 12.5,
                currency = "EUR",
                exchangeRate = null,
                entryDate = LocalDate(2026, 1, 1),
                splits = emptyList(),
            )
        val envelope =
            WebSocketMessageDto(
                type = wsType,
                payload = json.encodeToString(NewTabEntryWsPayload.serializer(), payload),
            )
        pendingOutboxDao.upsert(
            PendingOutboxEntity(
                id = outboxId,
                type = type,
                payload = json.encodeToString(WebSocketMessageDto.serializer(), envelope),
                createdAt = 1_000,
                expectedVersion = expectedVersion,
            ),
        )
    }

    private suspend fun TabMatesDatabase.insertPendingDelete(
        tabEntryId: String,
        groupId: String,
    ) {
        val payload =
            PendingDeletePayload(
                tabEntryId = tabEntryId,
                groupId = groupId,
                title = "Dinner",
                amount = 12.5,
                currencyCode = "EUR",
                entryType = "EXPENSE",
            )
        pendingOutboxDao.upsert(
            PendingOutboxEntity(
                id = "${TabEntryOutbox.DELETE_ID_PREFIX}$tabEntryId",
                type = TabEntryOutbox.OUTBOX_TYPE_TAB_ENTRY_DELETE,
                payload = json.encodeToString(PendingDeletePayload.serializer(), payload),
                createdAt = 1_000,
            ),
        )
    }
}
