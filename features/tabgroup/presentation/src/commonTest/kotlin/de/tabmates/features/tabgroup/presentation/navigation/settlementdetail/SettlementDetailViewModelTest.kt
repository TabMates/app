package de.tabmates.features.tabgroup.presentation.navigation.settlementdetail

import de.tabmates.features.tabgroup.presentation.navigation.creategroup.FakeCurrencyRepository
import de.tabmates.features.tabgroup.presentation.navigation.creategroup.FakeGroupRepository
import de.tabmates.features.tabgroup.presentation.testing.FakeSessionStorage
import de.tabmates.features.tabgroup.presentation.testing.FakeTabEntryRepository
import de.tabmates.features.tabgroup.presentation.testing.Fixtures
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
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
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

@OptIn(ExperimentalCoroutinesApi::class)
class SettlementDetailViewModelTest {
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
    fun loadsSettlementById() =
        runTest(testDispatcher) {
            val tabEntryRepo = FakeTabEntryRepository()
            tabEntryRepo.emit(
                groupId = "g1",
                entries = listOf(Fixtures.settlement(id = "s1", groupId = "g1", amount = 25.0)),
            )
            val viewModel = createViewModel(tabEntryRepository = tabEntryRepo)
            activateState(viewModel)
            advanceUntilIdle()

            val state = viewModel.state.value
            assertFalse(state.isLoading)
            val settlement = assertNotNull(state.settlement)
            assertEquals("s1", settlement.tabEntryId)
            assertEquals(25.0, settlement.amount)
            assertEquals("€", state.groupCurrencySymbol)
        }

    @Test
    fun nonSettlementEntryExposesNullSettlement() =
        runTest(testDispatcher) {
            val tabEntryRepo = FakeTabEntryRepository()
            tabEntryRepo.emit(
                groupId = "g1",
                entries = listOf(Fixtures.expense(id = "s1", groupId = "g1")),
            )
            val viewModel = createViewModel(tabEntryRepository = tabEntryRepo)
            activateState(viewModel)
            advanceUntilIdle()

            assertNull(viewModel.state.value.settlement)
        }

    @Test
    fun confirmDeleteRemovesSettlementAndEmitsEvent() =
        runTest(testDispatcher) {
            val tabEntryRepo = FakeTabEntryRepository()
            tabEntryRepo.emit(
                groupId = "g1",
                entries = listOf(Fixtures.settlement(id = "s1", groupId = "g1")),
            )
            val viewModel = createViewModel(tabEntryRepository = tabEntryRepo)
            val events = collectEvents(viewModel)
            activateState(viewModel)
            advanceUntilIdle()

            viewModel.onConfirmDelete()
            advanceUntilIdle()

            assertTrue(tabEntryRepo.getTabEntriesForGroup("g1").first().isEmpty())
            assertIs<SettlementDetailEvent.SettlementDeleted>(events.last())
        }

    private fun TestScope.collectEvents(viewModel: SettlementDetailViewModel): List<SettlementDetailEvent> {
        val events = mutableListOf<SettlementDetailEvent>()
        backgroundScope.launch { viewModel.events.collect { events.add(it) } }
        return events
    }

    @Test
    fun aSettlementThatIsNotThereIsReportedInsteadOfSpinningForever() =
        runTest(testDispatcher) {
            val viewModel = createViewModel()
            val events = mutableListOf<SettlementDetailEvent>()
            backgroundScope.launch { viewModel.events.collect { events.add(it) } }
            activateState(viewModel)

            val state = viewModel.state.value
            assertFalse(state.isLoading)
            assertTrue(state.isMissing)
            assertEquals(listOf<SettlementDetailEvent>(SettlementDetailEvent.SettlementUnavailable), events)
        }

    @Test
    fun deletingFromThisScreenReportsDeletedRatherThanUnavailable() =
        runTest(testDispatcher) {
            val tabEntryRepo = FakeTabEntryRepository()
            tabEntryRepo.emit("g1", listOf(Fixtures.settlement(id = "s1", groupId = "g1")))
            val viewModel = createViewModel(tabEntryRepository = tabEntryRepo)
            val events = mutableListOf<SettlementDetailEvent>()
            backgroundScope.launch { viewModel.events.collect { events.add(it) } }
            activateState(viewModel)

            viewModel.onConfirmDelete()
            advanceUntilIdle()

            assertEquals(listOf<SettlementDetailEvent>(SettlementDetailEvent.SettlementDeleted), events)
        }

    private fun TestScope.activateState(viewModel: SettlementDetailViewModel) {
        backgroundScope.launch { viewModel.state.collect {} }
        advanceUntilIdle()
    }

    private fun createViewModel(
        settlementId: String = "s1",
        groupId: String = "g1",
        tabEntryRepository: FakeTabEntryRepository = FakeTabEntryRepository(),
        groupRepository: FakeGroupRepository =
            FakeGroupRepository(initialGroups = listOf(Fixtures.group(id = "g1"))),
        currencyRepository: FakeCurrencyRepository = FakeCurrencyRepository(),
        sessionStorage: FakeSessionStorage = FakeSessionStorage(),
    ): SettlementDetailViewModel =
        SettlementDetailViewModel(
            settlementId = settlementId,
            groupId = groupId,
            tabEntryRepository = tabEntryRepository,
            groupRepository = groupRepository,
            currencyRepository = currencyRepository,
            sessionStorage = sessionStorage,
        )
}
