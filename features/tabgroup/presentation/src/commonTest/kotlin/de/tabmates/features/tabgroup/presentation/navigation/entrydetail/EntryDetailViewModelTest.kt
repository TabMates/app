package de.tabmates.features.tabgroup.presentation.navigation.entrydetail

import de.tabmates.features.tabgroup.domain.models.ExchangeRate
import de.tabmates.features.tabgroup.domain.models.TabEntry
import de.tabmates.features.tabgroup.presentation.navigation.addentry.EntryKind
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
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertTrue
import kotlin.time.Instant

@OptIn(ExperimentalCoroutinesApi::class)
class EntryDetailViewModelTest {
    private val testDispatcher = UnconfinedTestDispatcher()
    private val lastUpdatedAt = Instant.fromEpochMilliseconds(1_752_000_000_000)

    @BeforeTest
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
    }

    @AfterTest
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun sameCurrencyExpenseIsNotForeign() =
        runTest(testDispatcher) {
            val tabEntryRepo = FakeTabEntryRepository()
            tabEntryRepo.emit("g1", listOf(Fixtures.expense(id = "e1", groupId = "g1")))
            val viewModel = createViewModel(tabEntryRepository = tabEntryRepo)
            activateState(viewModel)

            val state = viewModel.state.value
            assertFalse(state.isForeignCurrency)
            assertEquals("EUR", state.entryCurrencyCode)
            assertEquals("EUR", state.groupCurrencyCode)
        }

    @Test
    fun foreignCurrencyExpenseExposesBothCurrenciesAndRates() =
        runTest(testDispatcher) {
            val tabEntryRepo = FakeTabEntryRepository()
            tabEntryRepo.emit(
                "g1",
                listOf(Fixtures.expense(id = "e1", groupId = "g1").copy(currencyCode = "USD")),
            )
            val exchangeRateRepo =
                FakeExchangeRateRepository(
                    initialRates =
                        listOf(
                            ExchangeRate("USD", 1.0, "USD", lastUpdatedAt),
                            ExchangeRate("EUR", 0.92, "USD", lastUpdatedAt),
                        ),
                )
            val viewModel =
                createViewModel(
                    tabEntryRepository = tabEntryRepo,
                    exchangeRateRepository = exchangeRateRepo,
                )
            activateState(viewModel)

            val state = viewModel.state.value
            assertTrue(state.isForeignCurrency)
            assertEquals("USD", state.entryCurrencyCode)
            assertEquals("$", state.entryCurrencySymbol)
            assertEquals("EUR", state.groupCurrencyCode)
            assertEquals("€", state.groupCurrencySymbol)
            assertEquals(mapOf("USD" to 1.0, "EUR" to 0.92), state.ratesByCurrency)
            assertEquals(lastUpdatedAt, state.ratesLastUpdatedAt)
        }

    @Test
    fun missingRatesLeaveEmptyMapAndNullTimestamp() =
        runTest(testDispatcher) {
            val tabEntryRepo = FakeTabEntryRepository()
            tabEntryRepo.emit(
                "g1",
                listOf(Fixtures.expense(id = "e1", groupId = "g1").copy(currencyCode = "USD")),
            )
            val viewModel = createViewModel(tabEntryRepository = tabEntryRepo)
            activateState(viewModel)

            val state = viewModel.state.value
            assertTrue(state.isForeignCurrency)
            assertTrue(state.ratesByCurrency.isEmpty())
            assertEquals(null, state.ratesLastUpdatedAt)
        }

    @Test
    fun incomeEntryIsExposedWithIncomeKind() =
        runTest(testDispatcher) {
            val tabEntryRepo = FakeTabEntryRepository()
            tabEntryRepo.emit(
                "g1",
                listOf(
                    Fixtures.income(
                        id = "e1",
                        groupId = "g1",
                        splits = listOf(Fixtures.split(tabEntryId = "e1")),
                    ),
                ),
            )
            val viewModel = createViewModel(tabEntryRepository = tabEntryRepo)
            activateState(viewModel)

            val state = viewModel.state.value
            assertFalse(state.isLoading)
            assertEquals(EntryKind.INCOME, state.entryKind)
            assertIs<TabEntry.Income>(state.entry)
            assertEquals(1, state.splits.size)
        }

    @Test
    fun anEntryThatIsNotThereIsReportedInsteadOfSpinningForever() =
        runTest(testDispatcher) {
            val viewModel = createViewModel()
            val events = mutableListOf<EntryDetailEvent>()
            backgroundScope.launch { viewModel.events.collect { events.add(it) } }
            activateState(viewModel)

            val state = viewModel.state.value
            assertFalse(state.isLoading)
            assertTrue(state.isMissing)
            assertEquals(listOf<EntryDetailEvent>(EntryDetailEvent.EntryUnavailable), events)
        }

    @Test
    fun anEntryThatLoadsIsNeverReportedAsUnavailable() =
        runTest(testDispatcher) {
            val tabEntryRepo = FakeTabEntryRepository()
            tabEntryRepo.emit("g1", listOf(Fixtures.expense(id = "e1", groupId = "g1")))
            val viewModel = createViewModel(tabEntryRepository = tabEntryRepo)
            val events = mutableListOf<EntryDetailEvent>()
            backgroundScope.launch { viewModel.events.collect { events.add(it) } }
            activateState(viewModel)

            assertFalse(viewModel.state.value.isMissing)
            assertTrue(events.isEmpty())
        }

    @Test
    fun deletingFromThisScreenReportsDeletedRatherThanUnavailable() =
        runTest(testDispatcher) {
            val tabEntryRepo = FakeTabEntryRepository()
            tabEntryRepo.emit("g1", listOf(Fixtures.expense(id = "e1", groupId = "g1")))
            val viewModel = createViewModel(tabEntryRepository = tabEntryRepo)
            val events = mutableListOf<EntryDetailEvent>()
            backgroundScope.launch { viewModel.events.collect { events.add(it) } }
            activateState(viewModel)

            viewModel.onConfirmDelete()
            advanceUntilIdle()

            // The row is gone now, but the screen already knows why.
            assertEquals(listOf<EntryDetailEvent>(EntryDetailEvent.EntryDeleted), events)
        }

    private fun TestScope.activateState(viewModel: EntryDetailViewModel) {
        backgroundScope.launch { viewModel.state.collect {} }
        advanceUntilIdle()
    }

    private fun createViewModel(
        tabEntryRepository: FakeTabEntryRepository = FakeTabEntryRepository(),
        groupRepository: FakeGroupRepository =
            FakeGroupRepository(initialGroups = listOf(Fixtures.group(id = "g1", currency = "EUR"))),
        currencyRepository: FakeCurrencyRepository = FakeCurrencyRepository(),
        exchangeRateRepository: FakeExchangeRateRepository = FakeExchangeRateRepository(),
        currentAccount: FakeCurrentAccount = FakeCurrentAccount(),
    ): EntryDetailViewModel =
        EntryDetailViewModel(
            entryId = "e1",
            groupId = "g1",
            tabEntryRepository = tabEntryRepository,
            groupRepository = groupRepository,
            currencyRepository = currencyRepository,
            exchangeRateRepository = exchangeRateRepository,
            currentAccount = currentAccount,
        )
}
