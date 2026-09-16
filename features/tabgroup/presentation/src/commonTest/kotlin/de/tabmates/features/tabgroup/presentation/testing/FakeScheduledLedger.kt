package de.tabmates.features.tabgroup.presentation.testing

import de.tabmates.features.tabgroup.domain.models.TabEntry
import de.tabmates.features.tabgroup.domain.recurring.ScheduledEntryProjector
import de.tabmates.features.tabgroup.domain.recurring.ScheduledLedger
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import kotlin.time.Clock

/**
 * Runs the real projection over fake sources.
 *
 * Deliberately not a stub: the thing worth testing at the screen level is that placeholders reach
 * the balances, and a fake that just returned a canned list would prove nothing about that. Tests
 * with no schedules get exactly the entries they emitted.
 *
 * [today] is settable so a test can pin the day rather than depend on when it runs.
 */
class FakeScheduledLedger(
    private val tabEntryRepository: FakeTabEntryRepository = FakeTabEntryRepository(),
    private val recurringSeriesRepository: FakeRecurringSeriesRepository = FakeRecurringSeriesRepository(),
    private val today: LocalDate =
        Clock.System
            .now()
            .toLocalDateTime(TimeZone.UTC)
            .date,
) : ScheduledLedger {
    override fun observeEntriesForGroup(groupId: String): Flow<List<TabEntry>> =
        combine(
            tabEntryRepository.getTabEntriesForGroup(groupId),
            recurringSeriesRepository.getSeriesForGroup(groupId),
            recurringSeriesRepository.getClaimedSlotsForGroup(groupId),
        ) { entries, series, claimedSlots ->
            entries +
                ScheduledEntryProjector.project(
                    series = series,
                    existingEntries = entries,
                    claimedSlots = claimedSlots,
                    today = today,
                )
        }
}
