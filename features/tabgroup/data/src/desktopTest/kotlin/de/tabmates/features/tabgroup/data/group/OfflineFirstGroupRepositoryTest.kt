package de.tabmates.features.tabgroup.data.group

import de.tabmates.core.domain.util.DataError
import de.tabmates.core.domain.util.Result
import de.tabmates.features.tabgroup.data.sync.FakePendingTabEntryBackfillStore
import de.tabmates.features.tabgroup.data.sync.FakeTabEntryService
import de.tabmates.features.tabgroup.data.sync.GroupTabEntryBackfiller
import de.tabmates.features.tabgroup.data.sync.NoopLogger
import de.tabmates.features.tabgroup.data.sync.createInMemoryDatabase
import de.tabmates.features.tabgroup.data.sync.expense
import de.tabmates.features.tabgroup.data.sync.group
import de.tabmates.features.tabgroup.data.sync.history
import de.tabmates.features.tabgroup.database.TabMatesDatabase
import de.tabmates.features.tabgroup.domain.models.Group
import kotlinx.coroutines.test.runTest
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertTrue

class OfflineFirstGroupRepositoryTest {
    private lateinit var database: TabMatesDatabase
    private lateinit var groupService: FakeGroupService
    private lateinit var tabEntryService: FakeTabEntryService
    private lateinit var pendingStore: FakePendingTabEntryBackfillStore
    private lateinit var repository: OfflineFirstGroupRepository

    @BeforeTest
    fun setUp() {
        database = createInMemoryDatabase()
        groupService = FakeGroupService(joinGroupResult = Result.Success(group("g1")))
        tabEntryService = FakeTabEntryService()
        pendingStore = FakePendingTabEntryBackfillStore()
        repository =
            OfflineFirstGroupRepository(
                groupService = groupService,
                database = database,
                tabEntryBackfiller =
                    GroupTabEntryBackfiller(tabEntryService, database, pendingStore, NoopLogger),
            )
    }

    @AfterTest
    fun tearDown() {
        database.close()
    }

    private suspend fun localGroupIds() = database.groupDao.getAllGroupIds().toSet()

    private suspend fun localEntryIds() = database.tabEntryDao.getAllTabEntryIds().toSet()

    @Test
    fun joinGroupPersistsGroupAndBackfillsItsEntries() =
        runTest {
            tabEntryService.groupEntriesResult = Result.Success(history(listOf(expense("e1", "g1"))))

            val result = repository.joinGroup(token = "token-g1", claimPlaceholderId = null)

            assertIs<Result.Success<Group>>(result)
            assertEquals(setOf("g1"), localGroupIds())
            assertEquals(setOf("e1"), localEntryIds())
            assertTrue(pendingStore.getAll().isEmpty())
        }

    @Test
    fun joinGroupSucceedsAndKeepsMarkerWhenBackfillFails() =
        runTest {
            tabEntryService.groupEntriesResult = Result.Failure(DataError.Remote.NO_INTERNET)

            val result = repository.joinGroup(token = "token-g1", claimPlaceholderId = null)

            assertIs<Result.Success<Group>>(result)
            assertEquals(setOf("g1"), localGroupIds())
            assertTrue(localEntryIds().isEmpty())
            assertEquals(setOf("g1"), pendingStore.getAll())
        }

    @Test
    fun joinGroupFailureNeverAttemptsBackfill() =
        runTest {
            groupService.joinGroupResult = Result.Failure(DataError.Remote.UNAUTHORIZED)

            val result = repository.joinGroup(token = "token-g1", claimPlaceholderId = null)

            assertIs<Result.Failure<DataError.Remote>>(result)
            assertTrue(localGroupIds().isEmpty())
            assertTrue(tabEntryService.receivedGroupIds.isEmpty())
            assertTrue(pendingStore.getAll().isEmpty())
        }
}
