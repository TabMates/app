package de.tabmates.features.tabgroup.data.recurring

import app.cash.turbine.test
import de.tabmates.core.domain.util.DataError
import de.tabmates.core.domain.util.EmptyResult
import de.tabmates.core.domain.util.Result
import de.tabmates.features.tabgroup.domain.models.GroupParticipant
import de.tabmates.features.tabgroup.domain.models.ParticipantType
import de.tabmates.features.tabgroup.domain.models.SplitType
import de.tabmates.features.tabgroup.domain.models.TabEntry
import de.tabmates.features.tabgroup.domain.recurring.RecurrenceFrequency
import de.tabmates.features.tabgroup.domain.recurring.RecurringEnd
import de.tabmates.features.tabgroup.domain.recurring.RecurringEntryType
import de.tabmates.features.tabgroup.domain.recurring.RecurringRule
import de.tabmates.features.tabgroup.domain.recurring.RecurringSeries
import de.tabmates.features.tabgroup.domain.recurring.RecurringSeriesRepository
import de.tabmates.features.tabgroup.domain.recurring.RecurringSlot
import de.tabmates.features.tabgroup.domain.recurring.RecurringTemplate
import de.tabmates.features.tabgroup.domain.recurring.RecurringTemplateSplit
import de.tabmates.features.tabgroup.domain.tabentry.NewTabEntrySplit
import de.tabmates.features.tabgroup.domain.tabentry.TabEntryRepository
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runTest
import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.atStartOfDayIn
import kotlinx.datetime.plus
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlin.time.Clock
import kotlin.time.Duration
import kotlin.time.Duration.Companion.hours
import kotlin.time.Duration.Companion.seconds
import kotlin.time.Instant

/**
 * The ledger's day boundary.
 *
 * An occurrence falls due at a calendar boundary, not at anything a repository emits, so the
 * projection has to advance on its own. Without that, a session left open overnight keeps showing
 * yesterday's due set — and the balance that goes with it — until something unrelated happens to
 * change.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class DefaultScheduledLedgerTest {
    private val today = LocalDate(2026, 8, 11)
    private val tomorrow = today.plus(1, DateTimeUnit.DAY)

    @Test
    fun `an occurrence due tomorrow appears once the UTC day turns over`() =
        runTest {
            // Six hours before midnight UTC, so the test crosses the boundary rather than starting
            // on it.
            val clock = MutableClock(tomorrow.atStartOfDayIn(TimeZone.UTC) - 6.hours)
            val ledger =
                DefaultScheduledLedger(
                    tabEntryRepository = FakeTabEntryRepository(),
                    recurringSeriesRepository =
                        FakeRecurringSeriesRepository(listOf(seriesStartingOn(tomorrow))),
                    clock = clock,
                )

            ledger.observeEntriesForGroup("g1").test {
                assertTrue(
                    awaitItem().none { it.isScheduledPlaceholder },
                    "an occurrence dated tomorrow is not owed yet",
                )

                // A second past the boundary: `advanceTimeBy` stops short of tasks scheduled on it.
                val pastMidnight = 6.hours + 1.seconds
                clock.advanceBy(pastMidnight)
                advanceTimeBy(pastMidnight)

                val projected = awaitItem().filter { it.isScheduledPlaceholder }
                assertEquals(1, projected.size)
                assertEquals(tomorrow, projected.single().entryDate)
                cancelAndIgnoreRemainingEvents()
            }
        }

    private fun seriesStartingOn(startDate: LocalDate) =
        RecurringSeries(
            seriesId = "series-1",
            groupId = "g1",
            entryType = RecurringEntryType.EXPENSE,
            isActive = true,
            needsAttention = false,
            createdAt = Instant.fromEpochMilliseconds(0),
            createdBy = GroupParticipant("user-1", "Alice", ParticipantType.REGISTERED),
            updatedAt = Instant.fromEpochMilliseconds(0),
            rule =
                RecurringRule(
                    ruleId = "rule-1",
                    title = "Rent",
                    description = "",
                    amount = 100.0,
                    currencyCode = "EUR",
                    exchangeRate = null,
                    paidByUserId = "user-1",
                    receivedByUserId = null,
                    splits =
                        listOf(
                            RecurringTemplateSplit(
                                splitId = "split-1",
                                participantId = "user-1",
                                splitType = SplitType.EQUAL,
                                value = 1.0,
                                resolvedAmount = 100.0,
                            ),
                        ),
                    frequency = RecurrenceFrequency.MONTHLY,
                    interval = 1,
                    startDate = startDate,
                    end = RecurringEnd.Never,
                ),
        )

    /** A clock the test moves by hand, so the day can turn over without waiting for it to. */
    private class MutableClock(
        private var now: Instant,
    ) : Clock {
        override fun now(): Instant = now

        fun advanceBy(duration: Duration) {
            now += duration
        }
    }

    private class FakeTabEntryRepository : TabEntryRepository {
        override fun getTabEntriesForGroup(groupId: String): Flow<List<TabEntry>> = flowOf(emptyList())

        override fun getTabEntryById(tabEntryId: String): Flow<TabEntry?> = flowOf(null)

        override suspend fun createExpense(
            groupId: String,
            title: String,
            description: String,
            amount: Double,
            currencyCode: String,
            exchangeRate: Double?,
            paidByUserId: String,
            entryDate: LocalDate,
            splits: List<NewTabEntrySplit>,
        ) = unexpected()

        override suspend fun updateExpense(
            tabEntryId: String,
            groupId: String,
            title: String,
            description: String,
            amount: Double,
            currencyCode: String,
            exchangeRate: Double?,
            paidByUserId: String,
            entryDate: LocalDate,
            splits: List<NewTabEntrySplit>,
        ) = unexpected()

        override suspend fun createIncome(
            groupId: String,
            title: String,
            description: String,
            amount: Double,
            currencyCode: String,
            exchangeRate: Double?,
            paidByUserId: String,
            entryDate: LocalDate,
            splits: List<NewTabEntrySplit>,
        ) = unexpected()

        override suspend fun updateIncome(
            tabEntryId: String,
            groupId: String,
            title: String,
            description: String,
            amount: Double,
            currencyCode: String,
            exchangeRate: Double?,
            paidByUserId: String,
            entryDate: LocalDate,
            splits: List<NewTabEntrySplit>,
        ) = unexpected()

        override suspend fun createSettlement(
            groupId: String,
            title: String,
            description: String,
            amount: Double,
            currencyCode: String,
            exchangeRate: Double?,
            paidByUserId: String,
            receivedByUserId: String,
            entryDate: LocalDate,
        ) = unexpected()

        override suspend fun updateSettlement(
            tabEntryId: String,
            groupId: String,
            title: String,
            description: String,
            amount: Double,
            currencyCode: String,
            exchangeRate: Double?,
            paidByUserId: String,
            receivedByUserId: String,
            entryDate: LocalDate,
        ) = unexpected()

        override suspend fun deleteTabEntry(tabEntryId: String): EmptyResult<DataError.Remote> = unexpected()

        private fun unexpected(): Nothing = error("unexpected write in a ledger test")
    }

    private class FakeRecurringSeriesRepository(
        private val series: List<RecurringSeries>,
    ) : RecurringSeriesRepository {
        // StateFlows, so the ledger's combine has something that stays open rather than completing
        // and settling the whole projection on its first value.
        private val seriesFlow = MutableStateFlow(series)
        private val claimsFlow = MutableStateFlow(emptySet<RecurringSlot>())

        override fun getSeriesForGroup(groupId: String): Flow<List<RecurringSeries>> = seriesFlow

        override fun getSeriesById(seriesId: String): Flow<RecurringSeries?> =
            MutableStateFlow(series.firstOrNull { it.seriesId == seriesId })

        override fun getClaimedSlotsForGroup(groupId: String): Flow<Set<RecurringSlot>> = claimsFlow

        override suspend fun createSeries(
            seriesId: String,
            groupId: String,
            template: RecurringTemplate,
        ): Result<RecurringSeries, DataError.Remote> = unexpected()

        override suspend fun updateSeries(
            seriesId: String,
            effectiveFrom: LocalDate,
            template: RecurringTemplate,
        ): Result<RecurringSeries, DataError.Remote> = unexpected()

        override suspend fun skipOccurrence(
            seriesId: String,
            occurrenceDate: LocalDate,
        ): EmptyResult<DataError.Remote> = unexpected()

        override suspend fun unskipOccurrence(
            seriesId: String,
            occurrenceDate: LocalDate,
        ): EmptyResult<DataError.Remote> = unexpected()

        override suspend fun endSeries(seriesId: String): EmptyResult<DataError.Remote> = unexpected()

        override suspend fun refreshSeriesForGroup(groupId: String): EmptyResult<DataError.Remote> =
            Result.Success(Unit)

        private fun unexpected(): Nothing = error("unexpected write in a ledger test")
    }
}
