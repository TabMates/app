package de.tabmates.features.tabgroup.data.recurring

import de.tabmates.features.tabgroup.domain.models.TabEntry
import de.tabmates.features.tabgroup.domain.recurring.RecurringSeriesRepository
import de.tabmates.features.tabgroup.domain.recurring.ScheduledEntryProjector
import de.tabmates.features.tabgroup.domain.recurring.ScheduledLedger
import de.tabmates.features.tabgroup.domain.tabentry.TabEntryRepository
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flow
import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.atStartOfDayIn
import kotlinx.datetime.plus
import kotlinx.datetime.toLocalDateTime
import org.koin.core.annotation.Single
import kotlin.time.Clock

@Single(binds = [ScheduledLedger::class])
class DefaultScheduledLedger(
    private val tabEntryRepository: TabEntryRepository,
    private val recurringSeriesRepository: RecurringSeriesRepository,
    private val clock: Clock,
) : ScheduledLedger {
    override fun observeEntriesForGroup(groupId: String): Flow<List<TabEntry>> =
        combine(
            tabEntryRepository.getTabEntriesForGroup(groupId),
            recurringSeriesRepository.getSeriesForGroup(groupId),
            recurringSeriesRepository.getClaimedSlotsForGroup(groupId),
            utcDates(),
        ) { entries, series, claimedSlots, today ->
            if (series.isEmpty()) {
                entries
            } else {
                entries +
                    ScheduledEntryProjector.project(
                        series = series,
                        // Unfiltered on purpose: a soft-deleted occurrence still occupies its slot.
                        existingEntries = entries,
                        claimedSlots = claimedSlots,
                        today = today,
                    )
            }
        }

    /**
     * The current UTC day, re-emitted as each one ends.
     *
     * The projection is a function of "today", so reading the clock inside the combine would pin it
     * to whenever a repository last emitted — a session left open overnight would keep showing
     * yesterday's due set until something unrelated happened to change. UTC because that is the day
     * the server's sweep measures against, and the two have to agree on which occurrences are owed
     * at the edges of a day.
     */
    private fun utcDates(): Flow<LocalDate> =
        flow {
            while (true) {
                val now = clock.now()
                val today = now.toLocalDateTime(TimeZone.UTC).date
                emit(today)
                delay(today.plus(1, DateTimeUnit.DAY).atStartOfDayIn(TimeZone.UTC) - now)
            }
        }
}
