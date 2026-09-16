package de.tabmates.features.tabgroup.data.sync

import de.tabmates.core.domain.util.DataError
import de.tabmates.core.domain.util.Result
import de.tabmates.features.tabgroup.data.mappers.toDomain
import de.tabmates.features.tabgroup.data.mappers.toEntity
import de.tabmates.features.tabgroup.database.TabMatesDatabase
import de.tabmates.features.tabgroup.database.entities.types.ParticipantTypeDatabase
import kotlinx.coroutines.test.runTest
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertTrue
import kotlin.time.Instant

class OfflineFirstSyncRepositoryTest {
    private lateinit var database: TabMatesDatabase

    @BeforeTest
    fun setUp() {
        database = createInMemoryDatabase()
    }

    @AfterTest
    fun tearDown() {
        database.close()
    }

    private fun repository(
        service: FakeSyncService,
        cursorStore: FakeSyncCursorStore,
        tabEntryService: FakeTabEntryService = FakeTabEntryService(),
        pendingBackfillStore: FakePendingTabEntryBackfillStore = FakePendingTabEntryBackfillStore(),
        lastServerContactStore: FakeLastServerContactStore = FakeLastServerContactStore(),
        recurringSeriesRepository: FakeRecurringSeriesRepository = FakeRecurringSeriesRepository(),
    ): OfflineFirstSyncRepository =
        OfflineFirstSyncRepository(
            syncService = service,
            database = database,
            cursorStore = cursorStore,
            lastServerContactStore = lastServerContactStore,
            tabEntryBackfiller =
                GroupTabEntryBackfiller(tabEntryService, database, pendingBackfillStore, NoopLogger),
            pendingBackfillStore = pendingBackfillStore,
            recurringSeriesLocalWriter = RecurringSeriesLocalWriter(database),
            recurringSeriesRepository = recurringSeriesRepository,
        )

    private suspend fun localGroupIds() = database.groupDao.getAllGroupIds().toSet()

    private suspend fun localEntryIds() = database.tabEntryDao.getAllTabEntryIds().toSet()

    private suspend fun pendingEntryIds() = database.tabEntryDao.getPendingSyncIds().toSet()

    private suspend fun entryAmount(id: String): Double? =
        database.tabEntryDao
            .getTabEntryById(id)
            ?.toDomain()
            ?.amount

    @Test
    fun initialFullSyncPersistsGroupsAndEntriesAndAdvancesCursor() =
        runTest {
            val service =
                FakeSyncService(
                    Result.Success(
                        snapshot(
                            serverTime = instant(1000),
                            groups = listOf(group("g1")),
                            tabEntries = listOf(expense("e1", "g1")),
                        ),
                    ),
                )
            val cursorStore = FakeSyncCursorStore(cursor = null)

            val result = repository(service, cursorStore).sync()

            assertIs<Result.Success<Unit>>(result)
            assertEquals(listOf<Instant?>(null), service.receivedSince.toList())
            assertEquals(setOf("g1"), localGroupIds())
            assertEquals(setOf("e1"), localEntryIds())
            assertEquals(instant(1000), cursorStore.get())
        }

    @Test
    fun deltaSyncSendsStoredCursorAsSince() =
        runTest {
            val service = FakeSyncService(Result.Success(snapshot(serverTime = instant(2000))))
            val cursorStore = FakeSyncCursorStore(cursor = instant(1000))

            repository(service, cursorStore).sync()

            assertEquals(listOf<Instant?>(instant(1000)), service.receivedSince.toList())
            assertEquals(instant(2000), cursorStore.get())
        }

    @Test
    fun groupMissingFromActiveGroupIdsIsPrunedWithItsEntries() =
        runTest {
            val cursorStore = FakeSyncCursorStore(cursor = null)
            val service =
                FakeSyncService(
                    Result.Success(
                        snapshot(
                            serverTime = instant(1000),
                            groups = listOf(group("g1"), group("g2")),
                            tabEntries = listOf(expense("e1", "g1"), expense("e2", "g2")),
                        ),
                    ),
                )
            val repository = repository(service, cursorStore)
            repository.sync()

            // g2 is no longer in activeGroupIds → dropped; its entry cascades away.
            service.result =
                Result.Success(
                    snapshot(serverTime = instant(2000), groups = emptyList(), activeGroupIds = listOf("g1")),
                )
            repository.sync()

            assertEquals(setOf("g1"), localGroupIds())
            assertEquals(setOf("e1"), localEntryIds())
        }

    @Test
    fun softDeletedEntryIsHardDeletedLocally() =
        runTest {
            val cursorStore = FakeSyncCursorStore(cursor = null)
            val service =
                FakeSyncService(
                    Result.Success(
                        snapshot(
                            serverTime = instant(1000),
                            groups = listOf(group("g1")),
                            tabEntries = listOf(expense("e1", "g1")),
                        ),
                    ),
                )
            val repository = repository(service, cursorStore)
            repository.sync()
            assertEquals(setOf("e1"), localEntryIds())

            service.result =
                Result.Success(
                    snapshot(
                        serverTime = instant(2000),
                        activeGroupIds = listOf("g1"),
                        tabEntries = listOf(expense("e1", "g1", deletedAt = instant(1500))),
                    ),
                )
            repository.sync()

            assertTrue(localEntryIds().isEmpty())
        }

    @Test
    fun pendingLocalEntryIsNotOverwrittenOrDeletedBySync() =
        runTest {
            val cursorStore = FakeSyncCursorStore(cursor = null)
            val service =
                FakeSyncService(
                    Result.Success(
                        snapshot(serverTime = instant(1000), groups = listOf(group("g1"))),
                    ),
                )
            val repository = repository(service, cursorStore)
            repository.sync()

            // Optimistic local write not yet confirmed by the server.
            database.tabEntryDao.upsertTabEntry(
                expense("e1", "g1", amount = 10.0, pendingSync = true).toEntity(pendingSync = true),
            )

            // A delta carrying the server's version (different amount) plus a tombstone for the same id
            // must leave the pending row untouched.
            service.result =
                Result.Success(
                    snapshot(
                        serverTime = instant(2000),
                        activeGroupIds = listOf("g1"),
                        tabEntries = listOf(expense("e1", "g1", amount = 99.0, deletedAt = instant(1500))),
                    ),
                )
            repository.sync()

            assertEquals(setOf("e1"), localEntryIds())
            assertEquals(setOf("e1"), pendingEntryIds())
            assertEquals(10.0, entryAmount("e1"))
        }

    @Test
    fun fullSyncPrunesMissingEntriesButKeepsPending() =
        runTest {
            val cursorStore = FakeSyncCursorStore(cursor = null)
            val service =
                FakeSyncService(
                    Result.Success(
                        snapshot(
                            serverTime = instant(1000),
                            groups = listOf(group("g1")),
                            tabEntries = listOf(expense("e1", "g1"), expense("e2", "g1")),
                        ),
                    ),
                )
            val repository = repository(service, cursorStore)
            repository.sync()
            database.tabEntryDao.upsertTabEntry(
                expense("e3", "g1", pendingSync = true).toEntity(pendingSync = true),
            )

            // Force another full snapshot (cursor null) that no longer reports e2.
            cursorStore.clear()
            service.result =
                Result.Success(
                    snapshot(
                        serverTime = instant(2000),
                        groups = listOf(group("g1")),
                        tabEntries = listOf(expense("e1", "g1")),
                    ),
                )
            repository.sync()

            assertEquals(setOf("e1", "e3"), localEntryIds())
        }

    @Test
    fun deltaSyncDoesNotPruneEntriesMissingFromPayload() =
        runTest {
            val cursorStore = FakeSyncCursorStore(cursor = null)
            val service =
                FakeSyncService(
                    Result.Success(
                        snapshot(
                            serverTime = instant(1000),
                            groups = listOf(group("g1")),
                            tabEntries = listOf(expense("e1", "g1"), expense("e2", "g1")),
                        ),
                    ),
                )
            val repository = repository(service, cursorStore)
            repository.sync()

            service.result =
                Result.Success(snapshot(serverTime = instant(2000), activeGroupIds = listOf("g1")))
            repository.sync()

            assertEquals(setOf("e1", "e2"), localEntryIds())
        }

    @Test
    fun splitReferencingExMemberIsPersistedViaReferencedParticipants() =
        runTest {
            // "ghost" left the group (or deleted their account): not in any group's participant
            // list, but old splits still reference them. Regression test for the login crash
            // (FK 787 in TabEntrySplitDao.upsertSplits).
            val cursorStore = FakeSyncCursorStore(cursor = null)
            val service =
                FakeSyncService(
                    Result.Success(
                        snapshot(
                            serverTime = instant(1000),
                            groups = listOf(group("g1", participantIds = listOf("u1"))),
                            tabEntries =
                                listOf(
                                    expense(
                                        "e1",
                                        "g1",
                                        splits =
                                            listOf(
                                                split("s1", "e1", participantId = "u1"),
                                                split("s2", "e1", participantId = "ghost"),
                                            ),
                                    ),
                                ),
                            referencedParticipants = listOf(participant("u1"), participant("ghost")),
                        ),
                    ),
                )

            val result = repository(service, cursorStore).sync()

            assertIs<Result.Success<Unit>>(result)
            assertEquals(
                setOf("s1", "s2"),
                database.tabEntrySplitDao
                    .getSplitsByTabEntryIdOnce("e1")
                    .map { it.splitId }
                    .toSet(),
            )
            assertEquals("user-ghost", database.groupParticipantDao.getParticipantById("ghost")?.username)
        }

    @Test
    fun splitReferencingUnknownParticipantGetsPlaceholderRow() =
        runTest {
            // Worst case: the payload carries a split whose participant appears nowhere else in
            // the payload. Sync must synthesize a placeholder row instead of dying on the FK.
            val cursorStore = FakeSyncCursorStore(cursor = null)
            val service =
                FakeSyncService(
                    Result.Success(
                        snapshot(
                            serverTime = instant(1000),
                            groups = listOf(group("g1", participantIds = listOf("u1"))),
                            tabEntries =
                                listOf(
                                    expense(
                                        "e1",
                                        "g1",
                                        splits = listOf(split("s1", "e1", participantId = "orphan")),
                                    ),
                                ),
                        ),
                    ),
                )

            val result = repository(service, cursorStore).sync()

            assertIs<Result.Success<Unit>>(result)
            assertEquals(
                setOf("s1"),
                database.tabEntrySplitDao
                    .getSplitsByTabEntryIdOnce("e1")
                    .map { it.splitId }
                    .toSet(),
            )
            val placeholder = database.groupParticipantDao.getParticipantById("orphan")
            assertEquals(ParticipantTypeDatabase.PLACEHOLDER, placeholder?.participantType)
        }

    @Test
    fun placeholderInsertDoesNotOverwriteExistingParticipant() =
        runTest {
            val cursorStore = FakeSyncCursorStore(cursor = null)
            val service =
                FakeSyncService(
                    Result.Success(
                        snapshot(
                            serverTime = instant(1000),
                            groups = listOf(group("g1", participantIds = listOf("u1", "u2"))),
                        ),
                    ),
                )
            val repository = repository(service, cursorStore)
            repository.sync()

            // Delta: u2 left the group; an old entry's split still references them but the payload
            // carries no participant object for them. The existing local row must survive as-is.
            service.result =
                Result.Success(
                    snapshot(
                        serverTime = instant(2000),
                        groups = listOf(group("g1", participantIds = listOf("u1"))),
                        tabEntries =
                            listOf(
                                expense(
                                    "e1",
                                    "g1",
                                    splits = listOf(split("s1", "e1", participantId = "u2")),
                                ),
                            ),
                    ),
                )
            repository.sync()

            assertEquals("user-u2", database.groupParticipantDao.getParticipantById("u2")?.username)
        }

    @Test
    fun successfulSyncRecordsLastServerContact() =
        runTest {
            val service = FakeSyncService(Result.Success(snapshot(serverTime = instant(1000))))
            val contactStore = FakeLastServerContactStore()

            repository(service, FakeSyncCursorStore(), lastServerContactStore = contactStore).sync()

            assertEquals(1, contactStore.recordCallCount)
        }

    @Test
    fun failedSyncDoesNotRecordLastServerContact() =
        runTest {
            val service = FakeSyncService(Result.Failure(DataError.Remote.SERVER_ERROR))
            val contactStore = FakeLastServerContactStore()

            repository(service, FakeSyncCursorStore(), lastServerContactStore = contactStore).sync()

            assertEquals(0, contactStore.recordCallCount)
        }

    @Test
    fun deltaSyncBackfillsEntriesForNewlyKnownGroup() =
        runTest {
            val cursorStore = FakeSyncCursorStore(cursor = instant(1000))
            val service =
                FakeSyncService(
                    // Joined-elsewhere group arrives in the delta without its historical entries.
                    Result.Success(snapshot(serverTime = instant(2000), groups = listOf(group("g2")))),
                )
            val tabEntryService =
                FakeTabEntryService(Result.Success(history(listOf(expense("e9", "g2")))))
            val pendingStore = FakePendingTabEntryBackfillStore()
            val recurring = FakeRecurringSeriesRepository()

            repository(service, cursorStore, tabEntryService, pendingStore, recurringSeriesRepository = recurring)
                .sync()

            assertEquals(listOf("g2"), tabEntryService.receivedGroupIds)
            assertEquals(setOf("e9"), localEntryIds())
            assertTrue(pendingStore.getAll().isEmpty())
            // Schedules have the same cursor gap entries do, so every backfilled group is refreshed.
            assertEquals(setOf("g2"), recurring.refreshedGroupIds.toSet())
        }

    @Test
    fun fullSyncSkipsBackfillAndClearsPendingMarkers() =
        runTest {
            val cursorStore = FakeSyncCursorStore(cursor = null)
            val service =
                FakeSyncService(
                    Result.Success(
                        snapshot(
                            serverTime = instant(1000),
                            groups = listOf(group("g1")),
                            tabEntries = listOf(expense("e1", "g1")),
                        ),
                    ),
                )
            val tabEntryService = FakeTabEntryService()
            val pendingStore = FakePendingTabEntryBackfillStore(initial = setOf("g1"))

            repository(service, cursorStore, tabEntryService, pendingStore).sync()

            assertTrue(tabEntryService.receivedGroupIds.isEmpty())
            assertTrue(pendingStore.getAll().isEmpty())
        }

    @Test
    fun deltaSyncRetriesPendingMarkerForAlreadyKnownGroup() =
        runTest {
            val cursorStore = FakeSyncCursorStore(cursor = null)
            val service =
                FakeSyncService(
                    Result.Success(snapshot(serverTime = instant(1000), groups = listOf(group("g1")))),
                )
            val tabEntryService = FakeTabEntryService()
            val pendingStore = FakePendingTabEntryBackfillStore()
            val recurring = FakeRecurringSeriesRepository()
            val repository =
                repository(
                    service,
                    cursorStore,
                    tabEntryService,
                    pendingStore,
                    recurringSeriesRepository = recurring,
                )
            repository.sync()

            // Simulates a join whose entries fetch failed: group already local, only the marker left.
            pendingStore.add("g1")
            tabEntryService.groupEntriesResult = Result.Success(history(listOf(expense("e1", "g1"))))
            service.result =
                Result.Success(snapshot(serverTime = instant(2000), activeGroupIds = listOf("g1")))
            repository.sync()

            assertEquals(listOf("g1"), tabEntryService.receivedGroupIds)
            assertEquals(setOf("e1"), localEntryIds())
            assertTrue(pendingStore.getAll().isEmpty())
            // A retried group's schedules are refreshed alongside its entries.
            assertEquals(listOf("g1"), recurring.refreshedGroupIds)
        }

    @Test
    fun deltaSyncDropsPendingMarkerForDepartedGroup() =
        runTest {
            val cursorStore = FakeSyncCursorStore(cursor = instant(1000))
            val service =
                FakeSyncService(
                    Result.Success(snapshot(serverTime = instant(2000), activeGroupIds = emptyList())),
                )
            val tabEntryService = FakeTabEntryService()
            val pendingStore = FakePendingTabEntryBackfillStore(initial = setOf("gone"))

            repository(service, cursorStore, tabEntryService, pendingStore).sync()

            assertTrue(tabEntryService.receivedGroupIds.isEmpty())
            assertTrue(pendingStore.getAll().isEmpty())
        }

    @Test
    fun backfillFailureKeepsSyncSuccessfulAndRetriesNextDelta() =
        runTest {
            val cursorStore = FakeSyncCursorStore(cursor = instant(1000))
            val service =
                FakeSyncService(
                    Result.Success(snapshot(serverTime = instant(2000), groups = listOf(group("g2")))),
                )
            val tabEntryService =
                FakeTabEntryService(Result.Failure(DataError.Remote.SERVER_ERROR))
            val pendingStore = FakePendingTabEntryBackfillStore()
            val repository = repository(service, cursorStore, tabEntryService, pendingStore)

            val result = repository.sync()

            assertIs<Result.Success<Unit>>(result)
            assertEquals(instant(2000), cursorStore.get())
            assertEquals(setOf("g2"), pendingStore.getAll())
            assertTrue(localEntryIds().isEmpty())

            // Next delta: group no longer "new", marker drives the retry.
            tabEntryService.groupEntriesResult = Result.Success(history(listOf(expense("e9", "g2"))))
            service.result =
                Result.Success(snapshot(serverTime = instant(3000), activeGroupIds = listOf("g2")))
            repository.sync()

            assertEquals(setOf("e9"), localEntryIds())
            assertTrue(pendingStore.getAll().isEmpty())
        }

    @Test
    fun cursorIsNotAdvancedWhenSyncFails() =
        runTest {
            val cursorStore = FakeSyncCursorStore(cursor = instant(1000))
            val service = FakeSyncService(Result.Failure(DataError.Remote.SERVER_ERROR))

            val result = repository(service, cursorStore).sync()

            assertIs<Result.Failure<DataError.Remote>>(result)
            assertEquals(instant(1000), cursorStore.get())
            assertTrue(localGroupIds().isEmpty())
        }
}
