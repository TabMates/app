package de.tabmates.features.tabgroup.presentation.navigation.groupdetail

import app.cash.turbine.test
import de.tabmates.core.presentation.format.NumberSymbols
import de.tabmates.features.tabgroup.domain.recurring.RecurringSlot
import de.tabmates.features.tabgroup.presentation.navigation.creategroup.FakeCurrencyRepository
import de.tabmates.features.tabgroup.presentation.navigation.creategroup.FakeGroupRepository
import de.tabmates.features.tabgroup.presentation.testing.FakeActivityRepository
import de.tabmates.features.tabgroup.presentation.testing.FakeCurrentAccount
import de.tabmates.features.tabgroup.presentation.testing.FakeExchangeRateRepository
import de.tabmates.features.tabgroup.presentation.testing.FakeRecurringSeriesRepository
import de.tabmates.features.tabgroup.presentation.testing.FakeScheduledLedger
import de.tabmates.features.tabgroup.presentation.testing.FakeTabEntryRepository
import de.tabmates.features.tabgroup.presentation.testing.Fixtures
import de.tabmates.features.tabgroup.presentation.testing.RecurringFixtures
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.minus
import kotlinx.datetime.plus
import kotlinx.datetime.toLocalDateTime
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import kotlin.time.Clock

/**
 * The group screen's half of the placeholder contract: occurrences a schedule owes but the server
 * has not written yet have to appear in the list *and* move the balances, so the numbers do not
 * jump when the server's sweep eventually writes the entry.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class GroupDetailScheduledEntriesTest {
    private val dispatcher = UnconfinedTestDispatcher()

    // The projector measures against the same UTC day the server's sweep does.
    private val today =
        Clock.System
            .now()
            .toLocalDateTime(TimeZone.UTC)
            .date
    private val alice = Fixtures.participant(id = "user-1", name = "Alice")
    private val bob = Fixtures.participant(id = "user-2", name = "Bob")

    @BeforeTest
    fun setUp() = Dispatchers.setMain(dispatcher)

    @AfterTest
    fun tearDown() = Dispatchers.resetMain()

    @Test
    fun `a due occurrence appears as a scheduled placeholder`() =
        runTest(dispatcher) {
            val recurring = FakeRecurringSeriesRepository()
            recurring.setSeries(dueSeries())
            val viewModel = viewModel(recurring)

            viewModel.state.test {
                advanceUntilIdle()
                val placeholders = expectMostRecentItem().entries.filter { it.isScheduledPlaceholder }
                assertEquals(1, placeholders.size)
                assertEquals(today, placeholders.single().entryDate)
                assertEquals("Rent", placeholders.single().title)
                cancelAndIgnoreRemainingEvents()
            }
        }

    @Test
    fun `a placeholder moves the balances like a real entry`() =
        runTest(dispatcher) {
            val recurring = FakeRecurringSeriesRepository()
            recurring.setSeries(dueSeries())
            val viewModel = viewModel(recurring)

            viewModel.state.test {
                advanceUntilIdle()
                val state = expectMostRecentItem()
                // Alice paid 100 and owes 50 of it, so she is up 50 and Bob is down 50 — exactly
                // what the numbers will read once the server writes the entry.
                assertEquals(50.0, state.memberNetBalances["user-1"])
                assertEquals(-50.0, state.memberNetBalances["user-2"])
                assertTrue(state.hasOutstandingDebts)
                cancelAndIgnoreRemainingEvents()
            }
        }

    @Test
    fun `a claimed slot produces no placeholder`() =
        runTest(dispatcher) {
            // The slot the server already wrote an entry for — including one since deleted, which
            // is why the claim is tracked separately from the entries themselves.
            val recurring = FakeRecurringSeriesRepository()
            recurring.setSeries(dueSeries())
            recurring.setClaimedSlots(RecurringSlot("series-1", today))
            val viewModel = viewModel(recurring)

            viewModel.state.test {
                advanceUntilIdle()
                val state = expectMostRecentItem()
                assertTrue(state.entries.none { it.isScheduledPlaceholder })
                assertFalse(state.hasOutstandingDebts)
                cancelAndIgnoreRemainingEvents()
            }
        }

    @Test
    fun `a parked series produces no placeholder`() =
        runTest(dispatcher) {
            val recurring = FakeRecurringSeriesRepository()
            recurring.setSeries(dueSeries().copy(needsAttention = true))
            val viewModel = viewModel(recurring)

            viewModel.state.test {
                advanceUntilIdle()
                assertTrue(expectMostRecentItem().entries.none { it.isScheduledPlaceholder })
                cancelAndIgnoreRemainingEvents()
            }
        }

    @Test
    fun `a future occurrence is not projected into the list`() =
        runTest(dispatcher) {
            val recurring = FakeRecurringSeriesRepository()
            recurring.setSeries(dueSeries(startDate = today.plusOneYear()))
            val viewModel = viewModel(recurring)

            viewModel.state.test {
                advanceUntilIdle()
                assertTrue(expectMostRecentItem().entries.none { it.isScheduledPlaceholder })
                cancelAndIgnoreRemainingEvents()
            }
        }

    private fun dueSeries(startDate: LocalDate = today) =
        RecurringFixtures.series(
            startDate = startDate,
            amount = 100.0,
            paidByUserId = "user-1",
            splits =
                listOf(
                    RecurringFixtures.templateSplit("user-1", resolvedAmount = 50.0),
                    RecurringFixtures.templateSplit("user-2", resolvedAmount = 50.0),
                ),
        )

    /** Calendar-aware: Feb 29 has no counterpart next year, so it clamps to Feb 28 rather than throwing. */
    private fun LocalDate.plusOneYear(): LocalDate {
        val firstOfTargetMonth = LocalDate(year + 1, month, 1)
        val lengthOfTargetMonth =
            firstOfTargetMonth
                .plus(1, DateTimeUnit.MONTH)
                .minus(1, DateTimeUnit.DAY)
                .day
        return LocalDate(year + 1, month, minOf(day, lengthOfTargetMonth))
    }

    private fun viewModel(recurring: FakeRecurringSeriesRepository): GroupDetailViewModel {
        val entries = FakeTabEntryRepository()
        return GroupDetailViewModel(
            groupId = "g1",
            groupRepository =
                FakeGroupRepository(
                    initialGroups = listOf(Fixtures.group(id = "g1", participants = setOf(alice, bob))),
                ),
            scheduledLedger = FakeScheduledLedger(entries, recurring, today),
            recurringSeriesRepository = recurring,
            currencyRepository = FakeCurrencyRepository(),
            exchangeRateRepository = FakeExchangeRateRepository(),
            activityRepository = FakeActivityRepository(),
            currentAccount = FakeCurrentAccount(),
            numberSymbols = NumberSymbols.Fallback,
        )
    }
}
