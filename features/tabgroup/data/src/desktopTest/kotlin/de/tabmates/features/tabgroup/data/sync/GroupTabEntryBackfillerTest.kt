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
import kotlin.test.assertTrue

class GroupTabEntryBackfillerTest {
    private lateinit var database: TabMatesDatabase
    private lateinit var tabEntryService: FakeTabEntryService
    private lateinit var pendingStore: FakePendingTabEntryBackfillStore
    private lateinit var backfiller: GroupTabEntryBackfiller

    @BeforeTest
    fun setUp() {
        database = createInMemoryDatabase()
        tabEntryService = FakeTabEntryService()
        pendingStore = FakePendingTabEntryBackfillStore()
        backfiller = GroupTabEntryBackfiller(tabEntryService, database, pendingStore, NoopLogger)
    }

    @AfterTest
    fun tearDown() {
        database.close()
    }

    private suspend fun localEntryIds() = database.tabEntryDao.getAllTabEntryIds().toSet()

    @Test
    fun successPersistsEntriesAndClearsMarker() =
        runTest {
            database.insertGroup(group("g1"))
            tabEntryService.groupEntriesResult =
                Result.Success(history(listOf(expense("e1", "g1"), expense("e2", "g1"))))

            backfiller.backfill("g1")

            assertEquals(listOf("g1"), tabEntryService.receivedGroupIds)
            assertEquals(setOf("e1", "e2"), localEntryIds())
            assertTrue(pendingStore.getAll().isEmpty())
        }

    @Test
    fun entryReferencingExMemberIsPersistedViaReferencedParticipants() =
        runTest {
            // A group's full history can include entries from members who have since left. This
            // is a regression test mirroring the FK 787 crash fixed for `/api/sync` (see
            // OfflineFirstSyncRepositoryTest.splitReferencingExMemberIsPersistedViaReferencedParticipants).
            database.insertGroup(group("g1", participantIds = listOf("u1")))
            tabEntryService.groupEntriesResult =
                Result.Success(
                    history(
                        entries =
                            listOf(
                                expense(
                                    "e1",
                                    "g1",
                                    splits = listOf(split("s1", "e1", participantId = "ghost")),
                                ),
                            ),
                        referencedParticipants = listOf(participant("ghost")),
                    ),
                )

            backfiller.backfill("g1")

            assertEquals(setOf("e1"), localEntryIds())
            assertEquals(
                setOf("s1"),
                database.tabEntrySplitDao
                    .getSplitsByTabEntryIdOnce("e1")
                    .map { it.splitId }
                    .toSet(),
            )
            assertEquals("user-ghost", database.groupParticipantDao.getParticipantById("ghost")?.username)
            assertTrue(pendingStore.getAll().isEmpty())
        }

    @Test
    fun failureKeepsMarkerAndPersistsNothing() =
        runTest {
            database.insertGroup(group("g1"))
            tabEntryService.groupEntriesResult = Result.Failure(DataError.Remote.SERVER_ERROR)

            backfiller.backfill("g1")

            assertEquals(setOf("g1"), pendingStore.getAll())
            assertTrue(localEntryIds().isEmpty())
        }

    @Test
    fun softDeletedEntryInDumpIsNotPersisted() =
        runTest {
            database.insertGroup(group("g1"))
            tabEntryService.groupEntriesResult =
                Result.Success(history(listOf(expense("e1", "g1"), expense("e2", "g1", deletedAt = instant(500)))))

            backfiller.backfill("g1")

            assertEquals(setOf("e1"), localEntryIds())
            assertTrue(pendingStore.getAll().isEmpty())
        }

    @Test
    fun pendingLocalEntryIsNotOverwritten() =
        runTest {
            database.insertGroup(group("g1"))
            database.tabEntryDao.upsertTabEntry(
                expense("e1", "g1", amount = 10.0, pendingSync = true).toEntity(pendingSync = true),
            )
            tabEntryService.groupEntriesResult =
                Result.Success(history(listOf(expense("e1", "g1", amount = 99.0))))

            backfiller.backfill("g1")

            assertEquals(setOf("e1"), database.tabEntryDao.getPendingSyncIds().toSet())
            assertEquals(
                10.0,
                database.tabEntryDao
                    .getTabEntryById("e1")
                    ?.toDomain()
                    ?.amount,
            )
        }
}
