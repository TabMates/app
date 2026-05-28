package de.tabmates.features.tabgroup.presentation.navigation.addexpense

import androidx.compose.foundation.text.input.setTextAndPlaceCursorAtEnd
import de.tabmates.features.tabgroup.domain.models.SplitType
import de.tabmates.features.tabgroup.domain.models.TabEntry
import de.tabmates.features.tabgroup.presentation.navigation.creategroup.FakeCurrencyRepository
import de.tabmates.features.tabgroup.presentation.navigation.creategroup.FakeGroupRepository
import de.tabmates.features.tabgroup.presentation.testing.FakeExchangeRateRepository
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
import kotlin.test.assertTrue

@OptIn(ExperimentalCoroutinesApi::class)
class AddExpenseViewModelTest {
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
            assertEquals(AddExpenseEvent.ExpenseCreated, events.last())
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
            assertIs<AddExpenseEvent.Error>(events.last())
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
            assertIs<AddExpenseEvent.Error>(events.last())
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
            assertIs<AddExpenseEvent.Error>(events.last())
        }

    // endregion

    // region edit

    @Test
    fun editingPrefillsFormFromExistingExpense() =
        runTest(testDispatcher) {
            val tabEntryRepo = FakeTabEntryRepository()
            tabEntryRepo.emit("g1", listOf(existingExpense()))
            val viewModel = createViewModel(groupId = "g1", expenseId = "e1", tabEntryRepository = tabEntryRepo)
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
            val viewModel = createViewModel(groupId = "g1", expenseId = "e1", tabEntryRepository = tabEntryRepo)
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
            assertEquals(AddExpenseEvent.ExpenseCreated, events.last())
        }

    // endregion

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

    private fun TestScope.collectEvents(viewModel: AddExpenseViewModel): List<AddExpenseEvent> {
        val events = mutableListOf<AddExpenseEvent>()
        backgroundScope.launch { viewModel.events.collect { events.add(it) } }
        return events
    }

    private fun TestScope.activateState(viewModel: AddExpenseViewModel) {
        backgroundScope.launch { viewModel.state.collect {} }
        advanceUntilIdle()
    }

    private fun createViewModel(
        groupId: String,
        expenseId: String = "",
        tabEntryRepository: FakeTabEntryRepository = FakeTabEntryRepository(),
        groupRepository: FakeGroupRepository =
            FakeGroupRepository(initialGroups = listOf(Fixtures.group(id = "g1", currency = "EUR"))),
        currencyRepository: FakeCurrencyRepository = FakeCurrencyRepository(),
        exchangeRateRepository: FakeExchangeRateRepository = FakeExchangeRateRepository(),
        sessionStorage: FakeSessionStorage = FakeSessionStorage(),
    ): AddExpenseViewModel =
        AddExpenseViewModel(
            groupId = groupId,
            expenseId = expenseId,
            tabEntryRepository = tabEntryRepository,
            groupRepository = groupRepository,
            currencyRepository = currencyRepository,
            exchangeRateRepository = exchangeRateRepository,
            sessionStorage = sessionStorage,
        )
}
