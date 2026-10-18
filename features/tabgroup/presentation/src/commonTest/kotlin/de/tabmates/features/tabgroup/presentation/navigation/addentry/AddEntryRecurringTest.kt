package de.tabmates.features.tabgroup.presentation.navigation.addentry

import androidx.compose.foundation.text.input.setTextAndPlaceCursorAtEnd
import de.tabmates.core.presentation.format.NumberSymbols
import de.tabmates.features.tabgroup.domain.models.TabEntry
import de.tabmates.features.tabgroup.domain.recurring.RecurrenceFrequency
import de.tabmates.features.tabgroup.domain.recurring.RecurringEnd
import de.tabmates.features.tabgroup.presentation.navigation.creategroup.FakeCurrencyRepository
import de.tabmates.features.tabgroup.presentation.navigation.creategroup.FakeGroupRepository
import de.tabmates.features.tabgroup.presentation.testing.FakeConnectionStatusRepository
import de.tabmates.features.tabgroup.presentation.testing.FakeCurrentAccount
import de.tabmates.features.tabgroup.presentation.testing.FakeExchangeRateRepository
import de.tabmates.features.tabgroup.presentation.testing.FakeRecurringSeriesRepository
import de.tabmates.features.tabgroup.presentation.testing.FakeTabEntryRepository
import de.tabmates.features.tabgroup.presentation.testing.Fixtures
import de.tabmates.features.tabgroup.presentation.testing.RecurringFixtures
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
import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.atStartOfDayIn
import kotlinx.datetime.plus
import kotlinx.datetime.toLocalDateTime
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlin.time.Clock

/**
 * The form's two new jobs: creating settlements, and creating a schedule instead of an entry.
 *
 * The second one is the load-bearing case. The server writes a schedule's first occurrence itself,
 * so a form that saved both an entry and a schedule would book the same thing twice in everybody's
 * ledger — with no error anywhere.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class AddEntryRecurringTest {
    private val dispatcher = UnconfinedTestDispatcher()
    private val alice = Fixtures.participant(id = "user-1", name = "Alice")
    private val bob = Fixtures.participant(id = "user-2", name = "Bob")

    @BeforeTest
    fun setUp() = Dispatchers.setMain(dispatcher)

    @AfterTest
    fun tearDown() = Dispatchers.resetMain()

    @Test
    fun `setting a repeat creates a series and no one-off entry`() =
        runTest(dispatcher) {
            val entries = FakeTabEntryRepository()
            val series = FakeRecurringSeriesRepository()
            val viewModel = viewModel(entries, series)
            activate(viewModel)

            viewModel.state.value.titleTextState
                .setTextAndPlaceCursorAtEnd("Rent")
            viewModel.state.value.amountTextState
                .setTextAndPlaceCursorAtEnd("900")
            viewModel.onRepeatFrequencyChange(RecurrenceFrequency.MONTHLY)
            viewModel.onRepeatIntervalChange(1)
            viewModel.onRepeatStartDateChange(today())
            viewModel.onRepeatEndChange(RecurringEnd.Never)
            viewModel.onSaveClick()
            advanceUntilIdle()

            assertEquals(1, series.recordedWrites.count { it.startsWith("create:") })
            // The sweep writes the first occurrence; writing one here too would double-book it.
            assertTrue(entries.getTabEntriesForGroup("g1").first().isEmpty())
        }

    @Test
    fun `leaving repeat unset still creates an ordinary entry`() =
        runTest(dispatcher) {
            val entries = FakeTabEntryRepository()
            val series = FakeRecurringSeriesRepository()
            val viewModel = viewModel(entries, series)
            activate(viewModel)

            viewModel.state.value.titleTextState
                .setTextAndPlaceCursorAtEnd("Lunch")
            viewModel.state.value.amountTextState
                .setTextAndPlaceCursorAtEnd("20")
            viewModel.onSaveClick()
            advanceUntilIdle()

            assertEquals(1, entries.getTabEntriesForGroup("g1").first().size)
            assertTrue(series.recordedWrites.isEmpty())
        }

    @Test
    fun `a repeat starting in the past is pulled forward to today`() =
        runTest(dispatcher) {
            // The server refuses a schedule that reaches back and invents entries nobody agreed to,
            // so the form corrects the date rather than posting a request that comes back a 400.
            val viewModel = viewModel()
            activate(viewModel)

            viewModel.onRepeatFrequencyChange(RecurrenceFrequency.MONTHLY)
            viewModel.onRepeatStartDateChange(LocalDate(2020, 1, 1))
            // Closing the editor is what lines the entry date up with the schedule.
            viewModel.onRepeatDismiss()
            advanceUntilIdle()

            assertEquals(
                today(),
                viewModel.state.value.repeat
                    ?.startDate,
            )
            assertEquals(today(), viewModel.state.value.entryDate)
        }

    @Test
    fun `switching to Never and back keeps the interval and end rule`() =
        runTest(dispatcher) {
            // The editor holds these as separate fields for exactly this reason: flipping through
            // "Never" while deciding must not silently reset a schedule the user already tuned.
            val viewModel = viewModel()
            activate(viewModel)

            viewModel.onRepeatFrequencyChange(RecurrenceFrequency.WEEKLY)
            viewModel.onRepeatIntervalChange(3)
            viewModel.onRepeatEndChange(RecurringEnd.Count(8))
            viewModel.onRepeatFrequencyChange(null)
            advanceUntilIdle()

            assertNull(viewModel.state.value.repeat)

            viewModel.onRepeatFrequencyChange(RecurrenceFrequency.WEEKLY)
            advanceUntilIdle()

            val repeat = assertNotNull(viewModel.state.value.repeat)
            assertEquals(3, repeat.interval)
            assertEquals(RecurringEnd.Count(8), repeat.end)
        }

    @Test
    fun `opening the editor seeds its start date from a future entry date`() =
        runTest(dispatcher) {
            val viewModel = viewModel()
            activate(viewModel)

            val future = LocalDate(today().year + 1, 6, 15)
            viewModel.onDateSelected(future.atStartOfDayIn(TimeZone.UTC).toEpochMilliseconds())
            viewModel.onRepeatOpen()
            advanceUntilIdle()

            assertEquals(future, viewModel.state.value.repeatStartDate)
        }

    @Test
    fun `creating a settlement writes a settlement entry`() =
        runTest(dispatcher) {
            val entries = FakeTabEntryRepository()
            val viewModel = viewModel(entries)
            activate(viewModel)

            viewModel.onKindChange(EntryKind.SETTLEMENT)
            viewModel.onPaidBySelected("user-1")
            viewModel.onReceivedBySelected("user-2")
            viewModel.state.value.titleTextState
                .setTextAndPlaceCursorAtEnd("Payback")
            viewModel.state.value.amountTextState
                .setTextAndPlaceCursorAtEnd("30")
            viewModel.onSaveClick()
            advanceUntilIdle()

            val settlement = assertIs<TabEntry.Settlement>(entries.getTabEntriesForGroup("g1").first().single())
            assertEquals("user-1", settlement.paidByUserId)
            assertEquals("user-2", settlement.receivedByUserId)
            assertEquals(30.0, settlement.amount)
        }

    @Test
    fun `a settlement to yourself is refused before it reaches the server`() =
        runTest(dispatcher) {
            val entries = FakeTabEntryRepository()
            val viewModel = viewModel(entries)
            activate(viewModel)

            viewModel.onKindChange(EntryKind.SETTLEMENT)
            viewModel.onPaidBySelected("user-1")
            viewModel.onReceivedBySelected("user-1")
            viewModel.state.value.titleTextState
                .setTextAndPlaceCursorAtEnd("Oops")
            viewModel.state.value.amountTextState
                .setTextAndPlaceCursorAtEnd("30")
            viewModel.onSaveClick()
            advanceUntilIdle()

            assertTrue(entries.getTabEntriesForGroup("g1").first().isEmpty())
        }

    @Test
    fun `editing a series seeds the split rows from its template`() =
        runTest(dispatcher) {
            // The template's splits are the only ones a schedule has — it owns no entry to read
            // them from. Loading none left every row unchecked, which saves the edit with nobody
            // on it or fails validation outright.
            val series = FakeRecurringSeriesRepository()
            series.setSeries(
                RecurringFixtures.series(
                    startDate = today().plus(1, DateTimeUnit.MONTH),
                    splits =
                        listOf(
                            RecurringFixtures.templateSplit("user-1", resolvedAmount = 60.0),
                            RecurringFixtures.templateSplit("user-2", resolvedAmount = 40.0),
                        ),
                ),
            )
            val viewModel = viewModel(series = series, seriesId = "series-1")
            activate(viewModel)

            val state = viewModel.state.value
            assertEquals(setOf("user-1", "user-2"), state.splitInputs.map { it.participantId }.toSet())
            assertTrue(
                state.splitInputs.all { it.included },
                "everyone the template splits across starts out included",
            )
            assertEquals("Rent", state.titleTextState.text.toString())
        }

    private fun today() =
        Clock.System
            .now()
            .toLocalDateTime(TimeZone.UTC)
            .date

    /**
     * Keeps a collector alive for the whole test. A one-shot `first()` lets `stateIn`'s
     * `WhileSubscribed` window lapse the moment virtual time is advanced, after which `state.value`
     * silently stops tracking the ViewModel.
     */
    private fun TestScope.activate(viewModel: AddEntryViewModel) {
        backgroundScope.launch { viewModel.state.collect {} }
        advanceUntilIdle()
    }

    private fun viewModel(
        entries: FakeTabEntryRepository = FakeTabEntryRepository(),
        series: FakeRecurringSeriesRepository = FakeRecurringSeriesRepository(),
        seriesId: String = "",
    ) = AddEntryViewModel(
        groupId = "g1",
        entryId = "",
        seriesId = seriesId,
        tabEntryRepository = entries,
        recurringSeriesRepository = series,
        connectionStatusRepository = FakeConnectionStatusRepository(),
        groupRepository =
            FakeGroupRepository(
                initialGroups = listOf(Fixtures.group(id = "g1", participants = setOf(alice, bob))),
            ),
        currencyRepository = FakeCurrencyRepository(),
        exchangeRateRepository = FakeExchangeRateRepository(),
        currentAccount = FakeCurrentAccount(),
        numberSymbols = NumberSymbols.Fallback,
    )
}
