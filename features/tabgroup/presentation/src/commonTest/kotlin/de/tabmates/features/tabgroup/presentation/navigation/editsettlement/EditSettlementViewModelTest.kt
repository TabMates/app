package de.tabmates.features.tabgroup.presentation.navigation.editsettlement

import androidx.compose.foundation.text.input.setTextAndPlaceCursorAtEnd
import de.tabmates.features.tabgroup.domain.models.TabEntry
import de.tabmates.features.tabgroup.presentation.navigation.creategroup.FakeCurrencyRepository
import de.tabmates.features.tabgroup.presentation.navigation.creategroup.FakeGroupRepository
import de.tabmates.features.tabgroup.presentation.testing.FakeCurrentAccount
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
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.atStartOfDayIn
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertIs

@OptIn(ExperimentalCoroutinesApi::class)
class EditSettlementViewModelTest {
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
    fun prefillsAmountDateAndFixedFields() =
        runTest(testDispatcher) {
            val tabEntryRepo = FakeTabEntryRepository()
            tabEntryRepo.emit(
                groupId = "g1",
                entries =
                    listOf(
                        Fixtures.settlement(
                            id = "s1",
                            groupId = "g1",
                            amount = 12.5,
                            paidByUserId = "user-1",
                            receivedByUserId = "user-2",
                            entryDate = LocalDate.parse("2024-03-05"),
                        ),
                    ),
            )
            val viewModel = createViewModel(tabEntryRepository = tabEntryRepo)
            activateState(viewModel)
            advanceUntilIdle()

            val state = viewModel.state.value
            assertFalse(state.isLoading)
            assertEquals("12.50", state.amountTextState.text.toString())
            assertEquals(LocalDate.parse("2024-03-05"), state.entryDate)
            assertEquals("Settlement", state.title)
            assertEquals("EUR", state.currencyCode)
            assertEquals("user-1", state.paidByUserId)
            assertEquals("user-2", state.receivedByUserId)
        }

    @Test
    fun saveUpdatesAmountAndDateKeepingFixedFields() =
        runTest(testDispatcher) {
            val tabEntryRepo = FakeTabEntryRepository()
            tabEntryRepo.emit(
                groupId = "g1",
                entries =
                    listOf(
                        Fixtures.settlement(
                            id = "s1",
                            groupId = "g1",
                            amount = 12.5,
                            paidByUserId = "user-1",
                            receivedByUserId = "user-2",
                            entryDate = LocalDate.parse("2024-03-05"),
                        ),
                    ),
            )
            val viewModel = createViewModel(tabEntryRepository = tabEntryRepo)
            val events = collectEvents(viewModel)
            activateState(viewModel)
            advanceUntilIdle()

            viewModel.state.value.amountTextState
                .setTextAndPlaceCursorAtEnd("20")
            viewModel.onDateSelected(
                LocalDate.parse("2024-04-01").atStartOfDayIn(TimeZone.UTC).toEpochMilliseconds(),
            )
            viewModel.onSaveClick()
            advanceUntilIdle()

            val updated =
                assertIs<TabEntry.Settlement>(
                    tabEntryRepo.getTabEntriesForGroup("g1").first().single(),
                )
            assertEquals(20.0, updated.amount)
            assertEquals(LocalDate.parse("2024-04-01"), updated.entryDate)
            assertEquals("Settlement", updated.title)
            assertEquals("EUR", updated.currencyCode)
            assertEquals("user-1", updated.paidByUserId)
            assertEquals("user-2", updated.receivedByUserId)
            assertIs<EditSettlementEvent.SettlementSaved>(events.last())
        }

    @Test
    fun invalidAmountEmitsErrorWithoutSaving() =
        runTest(testDispatcher) {
            val tabEntryRepo = FakeTabEntryRepository()
            tabEntryRepo.emit(
                groupId = "g1",
                entries = listOf(Fixtures.settlement(id = "s1", groupId = "g1", amount = 12.5)),
            )
            val viewModel = createViewModel(tabEntryRepository = tabEntryRepo)
            val events = collectEvents(viewModel)
            activateState(viewModel)
            advanceUntilIdle()

            viewModel.state.value.amountTextState
                .setTextAndPlaceCursorAtEnd("")
            viewModel.onSaveClick()
            advanceUntilIdle()

            assertIs<EditSettlementEvent.Error>(events.last())
            val unchanged =
                assertIs<TabEntry.Settlement>(
                    tabEntryRepo.getTabEntriesForGroup("g1").first().single(),
                )
            assertEquals(12.5, unchanged.amount)
        }

    private fun TestScope.collectEvents(viewModel: EditSettlementViewModel): List<EditSettlementEvent> {
        val events = mutableListOf<EditSettlementEvent>()
        backgroundScope.launch { viewModel.events.collect { events.add(it) } }
        return events
    }

    private fun TestScope.activateState(viewModel: EditSettlementViewModel) {
        backgroundScope.launch { viewModel.state.collect {} }
        advanceUntilIdle()
    }

    private fun createViewModel(
        groupId: String = "g1",
        settlementId: String = "s1",
        tabEntryRepository: FakeTabEntryRepository = FakeTabEntryRepository(),
        groupRepository: FakeGroupRepository =
            FakeGroupRepository(
                initialGroups =
                    listOf(
                        Fixtures.group(
                            id = "g1",
                            participants =
                                setOf(
                                    Fixtures.participant("user-1", "Alice"),
                                    Fixtures.participant("user-2", "Bob"),
                                ),
                        ),
                    ),
            ),
        currencyRepository: FakeCurrencyRepository = FakeCurrencyRepository(),
        currentAccount: FakeCurrentAccount = FakeCurrentAccount(),
    ): EditSettlementViewModel =
        EditSettlementViewModel(
            groupId = groupId,
            settlementId = settlementId,
            tabEntryRepository = tabEntryRepository,
            groupRepository = groupRepository,
            currencyRepository = currencyRepository,
            currentAccount = currentAccount,
        )
}
