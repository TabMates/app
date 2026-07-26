package de.tabmates.features.tabgroup.presentation.navigation.home

import de.tabmates.features.tabgroup.presentation.navigation.creategroup.FakeCurrencyRepository
import de.tabmates.features.tabgroup.presentation.navigation.creategroup.FakeGroupRepository
import de.tabmates.features.tabgroup.presentation.testing.FakeExchangeRateRepository
import de.tabmates.features.tabgroup.presentation.testing.FakeSessionStorage
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

@OptIn(ExperimentalCoroutinesApi::class)
class HomeViewModelTest {
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
    fun topGroupsAreTheThreeMostRecentlyChangedGroups() =
        runTest(testDispatcher) {
            val groupRepo =
                FakeGroupRepository(
                    initialGroups =
                        listOf(
                            Fixtures.group(id = "g1", title = "One", activityEpochMs = 400),
                            Fixtures.group(id = "g2", title = "Two", activityEpochMs = 300),
                            Fixtures.group(id = "g3", title = "Three", activityEpochMs = 200),
                            Fixtures.group(id = "g4", title = "Four", activityEpochMs = 100),
                        ),
                )
            val tabEntryRepo = FakeTabEntryRepository()
            // The oldest group by server timestamp has the freshest expense, so it must lead.
            tabEntryRepo.emit(
                groupId = "g4",
                entries = listOf(Fixtures.expense(id = "e1", groupId = "g4", lastModifiedEpochMs = 900)),
            )
            val viewModel =
                createViewModel(groupRepository = groupRepo, tabEntryRepository = tabEntryRepo)
            activateState(viewModel)
            advanceUntilIdle()

            assertEquals(
                listOf("Four", "One", "Two"),
                viewModel.state.value.topGroups
                    .map { it.title },
            )
        }

    @Test
    fun pendingLocalEntryReordersTopGroups() =
        runTest(testDispatcher) {
            val groupRepo =
                FakeGroupRepository(
                    initialGroups =
                        listOf(
                            Fixtures.group(id = "g1", title = "One", activityEpochMs = 400),
                            Fixtures.group(id = "g2", title = "Two", activityEpochMs = 300),
                        ),
                )
            val tabEntryRepo = FakeTabEntryRepository()
            val viewModel =
                createViewModel(groupRepository = groupRepo, tabEntryRepository = tabEntryRepo)
            activateState(viewModel)
            advanceUntilIdle()

            assertEquals(
                listOf("One", "Two"),
                viewModel.state.value.topGroups
                    .map { it.title },
            )

            // Offline write: entry is pending, no server round trip bumped Group.lastActivityAt.
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
            advanceUntilIdle()

            assertEquals(
                listOf("Two", "One"),
                viewModel.state.value.topGroups
                    .map { it.title },
            )
        }

    private fun TestScope.activateState(viewModel: HomeViewModel) {
        backgroundScope.launch { viewModel.state.collect {} }
        advanceUntilIdle()
    }

    private fun createViewModel(
        groupRepository: FakeGroupRepository = FakeGroupRepository(),
        tabEntryRepository: FakeTabEntryRepository = FakeTabEntryRepository(),
        currencyRepository: FakeCurrencyRepository = FakeCurrencyRepository(),
        exchangeRateRepository: FakeExchangeRateRepository = FakeExchangeRateRepository(),
        sessionStorage: FakeSessionStorage = FakeSessionStorage(),
    ): HomeViewModel =
        HomeViewModel(
            groupRepository = groupRepository,
            tabEntryRepository = tabEntryRepository,
            currencyRepository = currencyRepository,
            exchangeRateRepository = exchangeRateRepository,
            sessionStorage = sessionStorage,
        )
}
