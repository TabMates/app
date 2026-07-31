package de.tabmates.features.tabgroup.presentation.navigation.groupdetail

import de.tabmates.features.tabgroup.domain.models.GroupBalance
import de.tabmates.features.tabgroup.presentation.navigation.creategroup.FakeCurrencyRepository
import de.tabmates.features.tabgroup.presentation.navigation.creategroup.FakeGroupRepository
import de.tabmates.features.tabgroup.presentation.testing.FakeActivityRepository
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
import kotlinx.datetime.LocalDate
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

@OptIn(ExperimentalCoroutinesApi::class)
class GroupDetailViewModelTest {
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
    fun missingGroupExposesNullItem() =
        runTest(testDispatcher) {
            val viewModel = createViewModel(groupId = "missing")
            activateState(viewModel)
            advanceUntilIdle()

            assertNull(viewModel.state.value.item)
        }

    @Test
    fun matchingGroupExposesItem() =
        runTest(testDispatcher) {
            val groupRepo =
                FakeGroupRepository(initialGroups = listOf(Fixtures.group(id = "g1", title = "Trip")))
            val viewModel = createViewModel(groupId = "g1", groupRepository = groupRepo)
            activateState(viewModel)
            advanceUntilIdle()

            val item = viewModel.state.value.item
            assertNotNull(item)
            assertEquals("g1", item.id)
            assertEquals("Trip", item.title)
        }

    @Test
    fun statsReflectTabEntries() =
        runTest(testDispatcher) {
            val groupRepo =
                FakeGroupRepository(initialGroups = listOf(Fixtures.group(id = "g1")))
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
                            splits = listOf(Fixtures.split(participantId = "user-1", resolvedAmount = 40.0)),
                        ),
                        Fixtures.expense(
                            id = "e2",
                            groupId = "g1",
                            amount = 50.0,
                            paidByUserId = "user-1",
                            splits = listOf(Fixtures.split(participantId = "user-1", resolvedAmount = 50.0)),
                        ),
                    ),
            )
            val viewModel =
                createViewModel(
                    groupId = "g1",
                    groupRepository = groupRepo,
                    tabEntryRepository = tabEntryRepo,
                )
            activateState(viewModel)
            advanceUntilIdle()

            val item = viewModel.state.value.item!!
            assertEquals(2, item.expenseCount)
            assertEquals(150.0, item.totalSpent)
            // user-1 paid 100 + 50 = 150, owed share 40 + 50 = 90 → owed 60
            assertEquals(GroupBalance.Owed(60.0), item.balance)
        }

    @Test
    fun entriesIncludeSettlementsSortedWithExpenses() =
        runTest(testDispatcher) {
            val groupRepo =
                FakeGroupRepository(initialGroups = listOf(Fixtures.group(id = "g1")))
            val tabEntryRepo = FakeTabEntryRepository()
            tabEntryRepo.emit(
                groupId = "g1",
                entries =
                    listOf(
                        Fixtures.settlement(
                            id = "s1",
                            groupId = "g1",
                            amount = 25.0,
                            entryDate = LocalDate.parse("2024-01-01"),
                        ),
                        Fixtures.expense(
                            id = "e1",
                            groupId = "g1",
                            amount = 100.0,
                            splits = listOf(Fixtures.split(participantId = "user-1", resolvedAmount = 100.0)),
                            entryDate = LocalDate.parse("2024-01-02"),
                        ),
                    ),
            )
            val viewModel =
                createViewModel(
                    groupId = "g1",
                    groupRepository = groupRepo,
                    tabEntryRepository = tabEntryRepo,
                )
            activateState(viewModel)
            advanceUntilIdle()

            val state = viewModel.state.value
            assertEquals(listOf("e1", "s1"), state.entries.map { it.tabEntryId })
            // Settlements never count toward the expense stat cards.
            val item = state.item!!
            assertEquals(1, item.expenseCount)
            assertEquals(100.0, item.totalSpent)
        }

    @Test
    fun memberNetBalancesCoverDebtsBetweenOtherMembers() =
        runTest(testDispatcher) {
            // Current user is user-1; the only debt is user-3 → user-2.
            val groupRepo =
                FakeGroupRepository(
                    initialGroups =
                        listOf(
                            Fixtures.group(
                                id = "g1",
                                participants =
                                    setOf(
                                        Fixtures.participant("user-1", "Alice"),
                                        Fixtures.participant("user-2", "Bob"),
                                        Fixtures.participant("user-3", "Cara"),
                                    ),
                            ),
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
                            amount = 20.0,
                            paidByUserId = "user-2",
                            splits =
                                listOf(
                                    Fixtures.split(participantId = "user-2", resolvedAmount = 10.0),
                                    Fixtures.split(participantId = "user-3", resolvedAmount = 10.0),
                                ),
                        ),
                    ),
            )
            val viewModel =
                createViewModel(
                    groupId = "g1",
                    groupRepository = groupRepo,
                    tabEntryRepository = tabEntryRepo,
                )
            activateState(viewModel)
            advanceUntilIdle()

            val state = viewModel.state.value
            // The current user is uninvolved, but the group still has open debts.
            assertEquals(0.0, state.memberNetBalances["user-1"])
            assertEquals(10.0, state.memberNetBalances["user-2"])
            assertEquals(-10.0, state.memberNetBalances["user-3"])
            assertTrue(state.hasOutstandingDebts)
        }

    @Test
    fun settledGroupHasNoOutstandingDebts() =
        runTest(testDispatcher) {
            val groupRepo =
                FakeGroupRepository(initialGroups = listOf(Fixtures.group(id = "g1")))
            val viewModel = createViewModel(groupId = "g1", groupRepository = groupRepo)
            activateState(viewModel)
            advanceUntilIdle()

            assertFalse(viewModel.state.value.hasOutstandingDebts)
        }

    @Test
    fun currencySymbolResolvedFromCurrencyRepository() =
        runTest(testDispatcher) {
            val groupRepo =
                FakeGroupRepository(
                    initialGroups = listOf(Fixtures.group(id = "g1", currency = "USD")),
                )
            val viewModel = createViewModel(groupId = "g1", groupRepository = groupRepo)
            activateState(viewModel)
            advanceUntilIdle()

            val item = viewModel.state.value.item
            assertNotNull(item)
            assertIs<String>(item.currencySymbol)
            assertEquals("$", item.currencySymbol)
        }

    private fun TestScope.activateState(viewModel: GroupDetailViewModel) {
        backgroundScope.launch { viewModel.state.collect {} }
        advanceUntilIdle()
    }

    private fun createViewModel(
        groupId: String,
        groupRepository: FakeGroupRepository = FakeGroupRepository(),
        tabEntryRepository: FakeTabEntryRepository = FakeTabEntryRepository(),
        currencyRepository: FakeCurrencyRepository = FakeCurrencyRepository(),
        exchangeRateRepository: FakeExchangeRateRepository = FakeExchangeRateRepository(),
        activityRepository: FakeActivityRepository = FakeActivityRepository(),
        currentAccount: FakeCurrentAccount = FakeCurrentAccount(),
    ): GroupDetailViewModel =
        GroupDetailViewModel(
            groupId = groupId,
            groupRepository = groupRepository,
            tabEntryRepository = tabEntryRepository,
            currencyRepository = currencyRepository,
            exchangeRateRepository = exchangeRateRepository,
            activityRepository = activityRepository,
            currentAccount = currentAccount,
        )
}
