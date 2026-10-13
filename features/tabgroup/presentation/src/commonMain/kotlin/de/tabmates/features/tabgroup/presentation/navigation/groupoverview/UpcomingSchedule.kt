package de.tabmates.features.tabgroup.presentation.navigation.groupoverview

import de.tabmates.features.tabgroup.domain.recurring.RecurringOccurrenceCalculator
import de.tabmates.features.tabgroup.domain.recurring.RecurringSeries
import kotlinx.datetime.LocalDate

/** A schedule as the transactions tab's upcoming section shows it: the rule plus its next date. */
internal data class UpcomingSchedule(
    val series: RecurringSeries,
    val nextDate: LocalDate?,
)

/**
 * What the group's schedules are about to produce, in the order the section lists them.
 *
 * Only *upcoming* work belongs here — occurrences already due but unwritten are placeholders down in
 * the ledger, where they are counted in the balances. Nothing this function returns has moved a
 * balance yet.
 *
 * @param today the UTC day the server's sweep measures against, so both agree on what is still ahead
 */
internal fun upcomingSchedules(
    series: List<RecurringSeries>,
    today: LocalDate,
): List<UpcomingSchedule> =
    series
        .filter { it.isActive }
        .map { candidate ->
            UpcomingSchedule(
                series = candidate,
                // A parked schedule promises no date: the server writes nothing for it until a
                // member repairs the template.
                nextDate =
                    if (candidate.needsAttention) {
                        null
                    } else {
                        RecurringOccurrenceCalculator
                            .upcomingOccurrences(
                                rule = candidate.rule,
                                after = today,
                                limit = 1,
                                skippedDates = candidate.skippedOccurrenceDates,
                            ).firstOrNull()
                    },
            )
        }
        // An active schedule whose dates have run out has nothing upcoming to promise, so it belongs
        // on the schedules screen and not in a section named for what is coming.
        .filter { it.series.needsAttention || it.nextDate != null }
        // Parked first: they are the only rows asking for something, and the peek limit could
        // otherwise bury the one schedule that is silently producing nothing.
        .sortedWith(
            compareByDescending<UpcomingSchedule> { it.series.needsAttention }
                .thenBy(nullsLast()) { it.nextDate },
        )
