package de.tabmates.features.tabgroup.presentation.navigation.groupoverview

import androidx.compose.runtime.snapshots.Snapshot
import app.cash.turbine.test
import de.tabmates.features.tabgroup.domain.models.GroupBalance
import de.tabmates.features.tabgroup.presentation.navigation.creategroup.FakeCurrencyRepository
import de.tabmates.features.tabgroup.presentation.navigation.creategroup.FakeGroupRepository
import de.tabmates.features.tabgroup.presentation.testing.FakeCurrentAccount
import de.tabmates.features.tabgroup.presentation.testing.FakeExchangeRateRepository
import de.tabmates.features.tabgroup.presentation.testing.FakeTabEntryRepository
import de.tabmates.features.tabgroup.presentation.testing.Fixtures
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertTrue

@OptIn(ExperimentalCoroutinesApi::class)
class GroupOverviewViewModelTest {
    private val testDispatcher = UnconfinedTestDispatcher()

    @BeforeTest
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
    }

    @AfterTest
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun initialEmptyStateExposesEmptyItems() =
        runTest(testDispatcher) {
            val viewModel = createViewModel()
            viewModel.state.test {
                val state = awaitItem()
                assertTrue(state.allItems.isEmpty())
                assertTrue(state.displayedItems.isEmpty())
                assertEquals(GroupFilter.ALL, state.filter)
                cancelAndConsumeRemainingEvents()
            }
        }

    @Test
    fun emittedGroupsMapToItems() =
        runTest(testDispatcher) {
            val groupRepo =
                FakeGroupRepository(
                    initialGroups =
                        listOf(
                            Fixtures.group(id = "g1", title = "Trip"),
                            Fixtures.group(id = "g2", title = "Flat"),
                        ),
                )
            val viewModel = createViewModel(groupRepository = groupRepo)
            activateState(viewModel)
            advanceUntilIdle()

            val items = viewModel.state.value.allItems
            assertEquals(2, items.size)
            assertEquals(setOf("Trip", "Flat"), items.map { it.title }.toSet())
        }

    @Test
    fun onFilterSelectedSettledExcludesActiveBalances() =
        runTest(testDispatcher) {
            val groupRepo =
                FakeGroupRepository(
                    initialGroups =
                        listOf(
                            Fixtures.group(id = "g1", title = "Active"),
                            Fixtures.group(id = "g2", title = "Settled"),
                        ),
                )
            val tabEntryRepo = FakeTabEntryRepository()
            tabEntryRepo.emit(
                groupId = "g1",
                entries =
                    listOf(
                        Fixtures.expense(
                            id = "e1",
                            groupId = "g1",
                            amount = 100.0,
                            paidByUserId = "user-1",
                            splits = listOf(Fixtures.split(participantId = "user-1", resolvedAmount = 30.0)),
                        ),
                    ),
            )
            val viewModel =
                createViewModel(groupRepository = groupRepo, tabEntryRepository = tabEntryRepo)
            activateState(viewModel)
            advanceUntilIdle()

            viewModel.onFilterSelected(GroupFilter.SETTLED)
            advanceUntilIdle()

            val state = viewModel.state.value
            val displayed = state.displayedItems
            assertEquals(1, displayed.size)
            assertEquals("Settled", displayed.single().title)
            assertIs<GroupBalance.Settled>(displayed.single().balance)
        }

    @Test
    fun onFilterSelectedActiveExcludesSettledBalances() =
        runTest(testDispatcher) {
            val groupRepo =
                FakeGroupRepository(
                    initialGroups =
                        listOf(
                            Fixtures.group(id = "g1", title = "Active"),
                            Fixtures.group(id = "g2", title = "Settled"),
                        ),
                )
            val tabEntryRepo = FakeTabEntryRepository()
            tabEntryRepo.emit(
                groupId = "g1",
                entries =
                    listOf(
                        Fixtures.expense(
                            id = "e1",
                            groupId = "g1",
                            amount = 100.0,
                            paidByUserId = "user-1",
                            splits = listOf(Fixtures.split(participantId = "user-1", resolvedAmount = 30.0)),
                        ),
                    ),
            )
            val viewModel =
                createViewModel(groupRepository = groupRepo, tabEntryRepository = tabEntryRepo)
            activateState(viewModel)
            advanceUntilIdle()

            viewModel.onFilterSelected(GroupFilter.ACTIVE)
            advanceUntilIdle()

            val displayed = viewModel.state.value.displayedItems
            assertEquals(1, displayed.size)
            assertEquals("Active", displayed.single().title)
        }

    @Test
    fun searchQueryFiltersByTitleCaseInsensitive() =
        runTest(testDispatcher) {
            val groupRepo =
                FakeGroupRepository(
                    initialGroups =
                        listOf(
                            Fixtures.group(id = "g1", title = "Weekend in Lisbon"),
                            Fixtures.group(id = "g2", title = "Office Lunch"),
                        ),
                )
            val viewModel = createViewModel(groupRepository = groupRepo)
            activateState(viewModel)
            advanceUntilIdle()

            viewModel.state.value.searchQueryState
                .edit { replace(0, length, "lisbon") }
            Snapshot.sendApplyNotifications()
            advanceUntilIdle()

            val displayed = viewModel.state.value.displayedItems
            assertEquals(1, displayed.size)
            assertEquals("Weekend in Lisbon", displayed.single().title)
        }

    @Test
    fun groupWithPendingExpenseExposesHasPendingSync() =
        runTest(testDispatcher) {
            val groupRepo =
                FakeGroupRepository(
                    initialGroups =
                        listOf(
                            Fixtures.group(id = "g1", title = "Pending"),
                            Fixtures.group(id = "g2", title = "Synced"),
                        ),
                )
            val tabEntryRepo = FakeTabEntryRepository()
            tabEntryRepo.emit(
                groupId = "g1",
                entries = listOf(Fixtures.expense(id = "e1", groupId = "g1", isPendingSync = true)),
            )
            tabEntryRepo.emit(
                groupId = "g2",
                entries = listOf(Fixtures.expense(id = "e2", groupId = "g2", isPendingSync = false)),
            )
            val viewModel =
                createViewModel(groupRepository = groupRepo, tabEntryRepository = tabEntryRepo)
            activateState(viewModel)
            advanceUntilIdle()

            val items =
                viewModel.state.value.allItems
                    .associateBy { it.title }
            assertTrue(items.getValue("Pending").hasPendingSync)
            assertTrue(!items.getValue("Synced").hasPendingSync)

            // Server echo clears the flag; the group must follow.
            tabEntryRepo.emit(
                groupId = "g1",
                entries = listOf(Fixtures.expense(id = "e1", groupId = "g1", isPendingSync = false)),
            )
            advanceUntilIdle()

            assertTrue(
                !viewModel.state.value.allItems
                    .first { it.title == "Pending" }
                    .hasPendingSync,
            )
        }

    @Test
    fun freshEntryLiftsGroupAboveGroupWithNewerServerTimestamp() =
        runTest(testDispatcher) {
            // g1 was touched later server-side, but g2 has a just-added (still pending) expense —
            // nothing bumps Group.lastActivityAt locally, so the entry has to drive the order.
            val groupRepo =
                FakeGroupRepository(
                    initialGroups =
                        listOf(
                            Fixtures.group(id = "g1", title = "Stale", activityEpochMs = 500),
                            Fixtures.group(id = "g2", title = "Fresh", activityEpochMs = 100),
                        ),
                )
            val tabEntryRepo = FakeTabEntryRepository()
            tabEntryRepo.emit(
                groupId = "g2",
                entries =
                    listOf(
                        Fixtures.expense(
                            id = "e1",
                            groupId = "g2",
                            isPendingSync = true,
                            lastModifiedEpochMs = 900,
                        ),
                    ),
            )
            val viewModel =
                createViewModel(groupRepository = groupRepo, tabEntryRepository = tabEntryRepo)
            activateState(viewModel)
            advanceUntilIdle()

            assertEquals(
                listOf("Fresh", "Stale"),
                viewModel.state.value.displayedItems
                    .map { it.title },
            )

            // Adding an even newer entry to the other group flips the order back.
            tabEntryRepo.emit(
                groupId = "g1",
                entries = listOf(Fixtures.expense(id = "e2", groupId = "g1", lastModifiedEpochMs = 1_500)),
            )
            advanceUntilIdle()

            assertEquals(
                listOf("Stale", "Fresh"),
                viewModel.state.value.displayedItems
                    .map { it.title },
            )
        }

    @Test
    fun equalTimestampsKeepStableOrderById() =
        runTest(testDispatcher) {
            val groupRepo =
                FakeGroupRepository(
                    initialGroups =
                        listOf(
                            Fixtures.group(id = "g2", title = "Second", activityEpochMs = 100),
                            Fixtures.group(id = "g1", title = "First", activityEpochMs = 100),
                        ),
                )
            val viewModel = createViewModel(groupRepository = groupRepo)
            activateState(viewModel)
            advanceUntilIdle()

            assertEquals(
                listOf("First", "Second"),
                viewModel.state.value.displayedItems
                    .map { it.title },
            )
        }

    private fun TestScope.activateState(viewModel: GroupOverviewViewModel) {
        backgroundScope.launch { viewModel.state.collect {} }
        advanceUntilIdle()
    }

    private fun createViewModel(
        groupRepository: FakeGroupRepository = FakeGroupRepository(),
        tabEntryRepository: FakeTabEntryRepository = FakeTabEntryRepository(),
        currencyRepository: FakeCurrencyRepository = FakeCurrencyRepository(),
        exchangeRateRepository: FakeExchangeRateRepository = FakeExchangeRateRepository(),
        currentAccount: FakeCurrentAccount = FakeCurrentAccount(),
    ): GroupOverviewViewModel =
        GroupOverviewViewModel(
            groupRepository = groupRepository,
            tabEntryRepository = tabEntryRepository,
            currencyRepository = currencyRepository,
            exchangeRateRepository = exchangeRateRepository,
            currentAccount = currentAccount,
        )
}
