package de.tabmates.features.tabgroup.data.sync

import de.tabmates.core.domain.util.DataError
import de.tabmates.core.domain.util.Result
import de.tabmates.features.tabgroup.data.mappers.toDomain
import de.tabmates.features.tabgroup.data.mappers.toEntity
import de.tabmates.features.tabgroup.database.TabMatesDatabase
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
    ): OfflineFirstSyncRepository = OfflineFirstSyncRepository(service, database, cursorStore)

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
