package de.tabmates.features.tabgroup.presentation.navigation.addentry

import androidx.compose.foundation.text.input.setTextAndPlaceCursorAtEnd
import de.tabmates.features.tabgroup.domain.models.ExchangeRate
import de.tabmates.features.tabgroup.domain.models.SplitType
import de.tabmates.features.tabgroup.domain.models.TabEntry
import de.tabmates.features.tabgroup.presentation.navigation.creategroup.FakeCurrencyRepository
import de.tabmates.features.tabgroup.presentation.navigation.creategroup.FakeGroupRepository
import de.tabmates.features.tabgroup.presentation.testing.FakeCurrentAccount
import de.tabmates.features.tabgroup.presentation.testing.FakeExchangeRateRepository
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
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlin.time.Instant

@OptIn(ExperimentalCoroutinesApi::class)
class AddEntryViewModelTest {
    private val testDispatcher = UnconfinedTestDispatcher()

    @BeforeTest
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
    }

    @AfterTest
    fun tearDown() {
        Dispatchers.resetMain()
    }

    // region add

    @Test
    fun nonEditingStartsWithEmptyFormAndNotEditing() =
        runTest(testDispatcher) {
            val viewModel = createViewModel(groupId = "g1")
            activateState(viewModel)
            advanceUntilIdle()

            val state = viewModel.state.value
            assertFalse(state.isEditing)
            assertEquals("", state.titleTextState.text.toString())
            assertEquals("", state.amountTextState.text.toString())
        }

    @Test
    fun validCreateAddsExpenseAndEmitsSavedEvent() =
        runTest(testDispatcher) {
            val tabEntryRepo = FakeTabEntryRepository()
            val viewModel = createViewModel(groupId = "g1", tabEntryRepository = tabEntryRepo)
            val events = collectEvents(viewModel)
            activateState(viewModel)
            advanceUntilIdle()

            viewModel.state.value.titleTextState
                .setTextAndPlaceCursorAtEnd("Lunch")
            viewModel.state.value.amountTextState
                .setTextAndPlaceCursorAtEnd("20")
            viewModel.onSaveClick()
            advanceUntilIdle()

            val entries = tabEntryRepo.getTabEntriesForGroup("g1").first()
            assertEquals(1, entries.size)
            val expense = assertIs<TabEntry.Expense>(entries.first())
            assertEquals("Lunch", expense.title)
            assertEquals(20.0, expense.amount)
            assertEquals(AddEntryEvent.EntrySaved, events.last())
        }

    @Test
    fun creatingIncomeAddsIncomeEntry() =
        runTest(testDispatcher) {
            val tabEntryRepo = FakeTabEntryRepository()
            val viewModel = createViewModel(groupId = "g1", tabEntryRepository = tabEntryRepo)
            val events = collectEvents(viewModel)
            activateState(viewModel)
            advanceUntilIdle()

            viewModel.onKindChange(EntryKind.INCOME)
            viewModel.state.value.titleTextState
                .setTextAndPlaceCursorAtEnd("Refund")
            viewModel.state.value.amountTextState
                .setTextAndPlaceCursorAtEnd("30")
            viewModel.onSaveClick()
            advanceUntilIdle()

            val entries = tabEntryRepo.getTabEntriesForGroup("g1").first()
            assertEquals(1, entries.size)
            val income = assertIs<TabEntry.Income>(entries.first())
            assertEquals("Refund", income.title)
            assertEquals(30.0, income.amount)
            assertEquals(AddEntryEvent.EntrySaved, events.last())
        }

    @Test
    fun missingAmountEmitsErrorAndDoesNotCreate() =
        runTest(testDispatcher) {
            val tabEntryRepo = FakeTabEntryRepository()
            val viewModel = createViewModel(groupId = "g1", tabEntryRepository = tabEntryRepo)
            val events = collectEvents(viewModel)
            activateState(viewModel)
            advanceUntilIdle()

            viewModel.state.value.titleTextState
                .setTextAndPlaceCursorAtEnd("Lunch")
            viewModel.onSaveClick()
            advanceUntilIdle()

            assertTrue(tabEntryRepo.getTabEntriesForGroup("g1").first().isEmpty())
            assertIs<AddEntryEvent.Error>(events.last())
        }

    @Test
    fun titleOver255CharsEmitsErrorAndDoesNotCreate() =
        runTest(testDispatcher) {
            val tabEntryRepo = FakeTabEntryRepository()
            val viewModel = createViewModel(groupId = "g1", tabEntryRepository = tabEntryRepo)
            val events = collectEvents(viewModel)
            activateState(viewModel)
            advanceUntilIdle()

            viewModel.state.value.amountTextState
                .setTextAndPlaceCursorAtEnd("20")
            viewModel.state.value.titleTextState
                .setTextAndPlaceCursorAtEnd("a".repeat(256))
            viewModel.onSaveClick()
            advanceUntilIdle()

            assertTrue(tabEntryRepo.getTabEntriesForGroup("g1").first().isEmpty())
            assertIs<AddEntryEvent.Error>(events.last())
        }

    @Test
    fun descriptionOver255CharsEmitsErrorAndDoesNotCreate() =
        runTest(testDispatcher) {
            val tabEntryRepo = FakeTabEntryRepository()
            val viewModel = createViewModel(groupId = "g1", tabEntryRepository = tabEntryRepo)
            val events = collectEvents(viewModel)
            activateState(viewModel)
            advanceUntilIdle()

            viewModel.state.value.amountTextState
                .setTextAndPlaceCursorAtEnd("20")
            viewModel.state.value.titleTextState
                .setTextAndPlaceCursorAtEnd("Lunch")
            viewModel.state.value.descriptionTextState
                .setTextAndPlaceCursorAtEnd("a".repeat(256))
            viewModel.onSaveClick()
            advanceUntilIdle()

            assertTrue(tabEntryRepo.getTabEntriesForGroup("g1").first().isEmpty())
            assertIs<AddEntryEvent.Error>(events.last())
        }

    // endregion

    // region edit

    @Test
    fun editingPrefillsFormFromExistingExpense() =
        runTest(testDispatcher) {
            val tabEntryRepo = FakeTabEntryRepository()
            tabEntryRepo.emit("g1", listOf(existingExpense()))
            val viewModel = createViewModel(groupId = "g1", entryId = "e1", tabEntryRepository = tabEntryRepo)
            activateState(viewModel)
            advanceUntilIdle()

            val state = viewModel.state.value
            assertTrue(state.isEditing)
            assertEquals("Old title", state.titleTextState.text.toString())
            assertEquals("Old note", state.descriptionTextState.text.toString())
            assertEquals("100.00", state.amountTextState.text.toString())
            assertEquals("user-1", state.paidByUserId)
            assertEquals(SplitType.EQUAL, state.splitType)
        }

    @Test
    fun editSaveUpdatesExistingExpenseInPlace() =
        runTest(testDispatcher) {
            val tabEntryRepo = FakeTabEntryRepository()
            tabEntryRepo.emit("g1", listOf(existingExpense()))
            val viewModel = createViewModel(groupId = "g1", entryId = "e1", tabEntryRepository = tabEntryRepo)
            val events = collectEvents(viewModel)
            activateState(viewModel)
            advanceUntilIdle()

            viewModel.state.value.titleTextState
                .setTextAndPlaceCursorAtEnd("New title")
            viewModel.onSaveClick()
            advanceUntilIdle()

            val entries = tabEntryRepo.getTabEntriesForGroup("g1").first()
            assertEquals(1, entries.size)
            val expense = assertIs<TabEntry.Expense>(entries.first())
            assertEquals("e1", expense.tabEntryId)
            assertEquals("New title", expense.title)
            assertEquals(AddEntryEvent.EntrySaved, events.last())
        }

    @Test
    fun editingIncomeLocksKindAndUpdatesInPlace() =
        runTest(testDispatcher) {
            val tabEntryRepo = FakeTabEntryRepository()
            tabEntryRepo.emit("g1", listOf(existingIncome()))
            val viewModel = createViewModel(groupId = "g1", entryId = "i1", tabEntryRepository = tabEntryRepo)
            val events = collectEvents(viewModel)
            activateState(viewModel)
            advanceUntilIdle()

            assertTrue(viewModel.state.value.isEditing)
            assertEquals(EntryKind.INCOME, viewModel.state.value.entryKind)
            // Kind is locked while editing — attempts to flip it are ignored.
            viewModel.onKindChange(EntryKind.EXPENSE)
            assertEquals(EntryKind.INCOME, viewModel.state.value.entryKind)

            viewModel.state.value.titleTextState
                .setTextAndPlaceCursorAtEnd("New income title")
            viewModel.onSaveClick()
            advanceUntilIdle()

            val entries = tabEntryRepo.getTabEntriesForGroup("g1").first()
            assertEquals(1, entries.size)
            val income = assertIs<TabEntry.Income>(entries.first())
            assertEquals("i1", income.tabEntryId)
            assertEquals("New income title", income.title)
            assertEquals(AddEntryEvent.EntrySaved, events.last())
        }

    // endregion

    // region exchange rates

    @Test
    fun ratesTimestampAndPickerRatesAreExposed() =
        runTest(testDispatcher) {
            val lastUpdatedAt = Instant.fromEpochMilliseconds(1_752_000_000_000)
            val exchangeRateRepo =
                FakeExchangeRateRepository(
                    initialRates =
                        listOf(
                            ExchangeRate("USD", 1.0, "USD", lastUpdatedAt),
                            ExchangeRate("EUR", 0.92, "USD", lastUpdatedAt),
                        ),
                )
            val viewModel = createViewModel(groupId = "g1", exchangeRateRepository = exchangeRateRepo)
            activateState(viewModel)
            backgroundScope.launch { viewModel.currencyPickerState.collect {} }
            advanceUntilIdle()

            assertEquals(lastUpdatedAt, viewModel.state.value.ratesLastUpdatedAt)
            val pickerState = viewModel.currencyPickerState.value
            assertEquals("EUR", pickerState.baseCurrencyCode)
            assertEquals(mapOf("USD" to 1.0, "EUR" to 0.92), pickerState.ratesByCurrency)
            assertEquals(lastUpdatedAt, pickerState.ratesLastUpdatedAt)
        }

    @Test
    fun creatingForeignCurrencyExpenseLocksDisplayedRate() =
        runTest(testDispatcher) {
            val tabEntryRepo = FakeTabEntryRepository()
            val viewModel =
                createViewModel(
                    groupId = "g1",
                    tabEntryRepository = tabEntryRepo,
                    exchangeRateRepository = usdEurRates(),
                )
            activateState(viewModel)
            advanceUntilIdle()

            viewModel.onCurrencySelected("USD")
            viewModel.state.value.titleTextState
                .setTextAndPlaceCursorAtEnd("Lunch")
            viewModel.state.value.amountTextState
                .setTextAndPlaceCursorAtEnd("20")
            viewModel.onSaveClick()
            advanceUntilIdle()

            val expense = assertIs<TabEntry.Expense>(tabEntryRepo.getTabEntriesForGroup("g1").first().single())
            // Group base EUR, 1 USD = 0.92 EUR — exactly the rate the hint showed at save time.
            assertEquals(0.92, expense.exchangeRate!!, absoluteTolerance = 1e-9)
        }

    @Test
    fun creatingSameCurrencyExpenseHasNullExchangeRate() =
        runTest(testDispatcher) {
            val tabEntryRepo = FakeTabEntryRepository()
            val viewModel =
                createViewModel(
                    groupId = "g1",
                    tabEntryRepository = tabEntryRepo,
                    exchangeRateRepository = usdEurRates(),
                )
            activateState(viewModel)
            advanceUntilIdle()

            viewModel.state.value.titleTextState
                .setTextAndPlaceCursorAtEnd("Lunch")
            viewModel.state.value.amountTextState
                .setTextAndPlaceCursorAtEnd("20")
            viewModel.onSaveClick()
            advanceUntilIdle()

            val expense = assertIs<TabEntry.Expense>(tabEntryRepo.getTabEntriesForGroup("g1").first().single())
            assertNull(expense.exchangeRate)
        }

    @Test
    fun creatingWithoutLoadedRatesLeavesExchangeRateNull() =
        runTest(testDispatcher) {
            val tabEntryRepo = FakeTabEntryRepository()
            val viewModel = createViewModel(groupId = "g1", tabEntryRepository = tabEntryRepo)
            activateState(viewModel)
            advanceUntilIdle()

            viewModel.onCurrencySelected("USD")
            viewModel.state.value.titleTextState
                .setTextAndPlaceCursorAtEnd("Lunch")
            viewModel.state.value.amountTextState
                .setTextAndPlaceCursorAtEnd("20")
            viewModel.onSaveClick()
            advanceUntilIdle()

            val expense = assertIs<TabEntry.Expense>(tabEntryRepo.getTabEntriesForGroup("g1").first().single())
            assertNull(expense.exchangeRate)
        }

    @Test
    fun editingWithoutCurrencyChangeKeepsOriginalLockedRate() =
        runTest(testDispatcher) {
            val tabEntryRepo = FakeTabEntryRepository()
            // Locked at 0.90 when created; live rates have since moved to 0.92.
            tabEntryRepo.emit(
                "g1",
                listOf(existingExpense().copy(currencyCode = "USD", exchangeRate = 0.90)),
            )
            val viewModel =
                createViewModel(
                    groupId = "g1",
                    entryId = "e1",
                    tabEntryRepository = tabEntryRepo,
                    exchangeRateRepository = usdEurRates(),
                )
            activateState(viewModel)
            advanceUntilIdle()

            viewModel.state.value.titleTextState
                .setTextAndPlaceCursorAtEnd("New title")
            viewModel.onSaveClick()
            advanceUntilIdle()

            val expense = assertIs<TabEntry.Expense>(tabEntryRepo.getTabEntriesForGroup("g1").first().single())
            assertEquals(0.90, expense.exchangeRate!!, absoluteTolerance = 1e-9)
        }

    @Test
    fun editingCurrencyReSnapshotsRateFromLiveRates() =
        runTest(testDispatcher) {
            val tabEntryRepo = FakeTabEntryRepository()
            tabEntryRepo.emit(
                "g1",
                listOf(existingExpense().copy(currencyCode = "USD", exchangeRate = 0.90)),
            )
            val viewModel =
                createViewModel(
                    groupId = "g1",
                    entryId = "e1",
                    tabEntryRepository = tabEntryRepo,
                    exchangeRateRepository = usdEurRates(),
                )
            activateState(viewModel)
            advanceUntilIdle()

            viewModel.onCurrencySelected("GBP")
            viewModel.state.value.titleTextState
                .setTextAndPlaceCursorAtEnd("New title")
            viewModel.onSaveClick()
            advanceUntilIdle()

            val expense = assertIs<TabEntry.Expense>(tabEntryRepo.getTabEntriesForGroup("g1").first().single())
            // 1 GBP -> (1/0.80) USD -> * 0.92 = 1.15 EUR; the stale 0.90 snapshot is replaced.
            assertEquals(1.15, expense.exchangeRate!!, absoluteTolerance = 1e-9)
        }

    // endregion

    private fun usdEurRates(): FakeExchangeRateRepository =
        FakeExchangeRateRepository(
            initialRates =
                listOf(
                    ExchangeRate("USD", 1.0, "USD", Instant.fromEpochMilliseconds(0)),
                    ExchangeRate("EUR", 0.92, "USD", Instant.fromEpochMilliseconds(0)),
                    ExchangeRate("GBP", 0.80, "USD", Instant.fromEpochMilliseconds(0)),
                ),
        )

    private fun existingExpense(): TabEntry.Expense =
        Fixtures
            .expense(
                id = "e1",
                groupId = "g1",
                amount = 100.0,
                paidByUserId = "user-1",
                splits =
                    listOf(
                        Fixtures.split(tabEntryId = "e1", participantId = "user-1", resolvedAmount = 100.0),
                    ),
            ).copy(title = "Old title", description = "Old note")

    private fun existingIncome(): TabEntry.Income =
        Fixtures
            .income(
                id = "i1",
                groupId = "g1",
                amount = 100.0,
                paidByUserId = "user-1",
                splits =
                    listOf(
                        Fixtures.split(tabEntryId = "i1", participantId = "user-1", resolvedAmount = 100.0),
                    ),
            ).copy(title = "Old income", description = "note")

    private fun TestScope.collectEvents(viewModel: AddEntryViewModel): List<AddEntryEvent> {
        val events = mutableListOf<AddEntryEvent>()
        backgroundScope.launch { viewModel.events.collect { events.add(it) } }
        return events
    }

    private fun TestScope.activateState(viewModel: AddEntryViewModel) {
        backgroundScope.launch { viewModel.state.collect {} }
        advanceUntilIdle()
    }

    private fun createViewModel(
        groupId: String,
        entryId: String = "",
        tabEntryRepository: FakeTabEntryRepository = FakeTabEntryRepository(),
        groupRepository: FakeGroupRepository =
            FakeGroupRepository(initialGroups = listOf(Fixtures.group(id = "g1", currency = "EUR"))),
        currencyRepository: FakeCurrencyRepository = FakeCurrencyRepository(),
        exchangeRateRepository: FakeExchangeRateRepository = FakeExchangeRateRepository(),
        currentAccount: FakeCurrentAccount = FakeCurrentAccount(),
    ): AddEntryViewModel =
        AddEntryViewModel(
            groupId = groupId,
            entryId = entryId,
            tabEntryRepository = tabEntryRepository,
            groupRepository = groupRepository,
            currencyRepository = currencyRepository,
            exchangeRateRepository = exchangeRateRepository,
            currentAccount = currentAccount,
        )
}
