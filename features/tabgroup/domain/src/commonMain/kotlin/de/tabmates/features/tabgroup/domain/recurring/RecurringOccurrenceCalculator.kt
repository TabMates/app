package de.tabmates.features.tabgroup.domain.recurring

import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.LocalDate
import kotlinx.datetime.minus
import kotlinx.datetime.plus

/**
 * Turns a recurrence rule into the calendar dates it produces.
 *
 * A port of the server's `RecurringOccurrenceGenerator`, and it has to stay one: the server decides
 * which occurrences actually get written, and this decides which ones the client renders as
 * placeholders in the meantime. A divergence shows up as a placeholder that never resolves, or a
 * written entry that was never previewed.
 *
 * Pure by design — no clock. Callers decide what "today" means, which keeps every timezone question
 * out of the date arithmetic.
 */
object RecurringOccurrenceCalculator {
    /**
     * Slots walked before giving up. Mirrors the server's own bound; years of daily occurrences.
     * Without it a daily rule anchored far in the past would spin on a background thread.
     */
    private const val MAX_SLOT_SCAN = 10_000

    private const val MONTHS_PER_YEAR = 12

    /**
     * The date of the zero-based [slotIndex]th occurrence of a rule anchored at [anchorDate].
     *
     * Always computed fresh from [anchorDate] rather than by stepping off the previous occurrence.
     * That is the whole trick for monthly rules: adding a month clamps to the month it lands on, so
     * chaining Jan 31 -> Feb 28 -> Mar 28 would ratchet the day down permanently after the first
     * short month and never recover the 31st. Recomputing from the anchor each time means February
     * borrows the day and March gives it straight back.
     */
    fun occurrenceDateForSlot(
        frequency: RecurrenceFrequency,
        interval: Int,
        anchorDate: LocalDate,
        slotIndex: Int,
    ): LocalDate {
        require(interval > 0) { "interval must be positive, was $interval" }
        require(slotIndex >= 0) { "slotIndex must not be negative, was $slotIndex" }

        val steps = interval.toLong() * slotIndex
        return when (frequency) {
            RecurrenceFrequency.DAILY -> anchorDate.plus(steps, DateTimeUnit.DAY)
            RecurrenceFrequency.WEEKLY -> anchorDate.plus(steps, DateTimeUnit.WEEK)
            RecurrenceFrequency.MONTHLY -> clampedMonthsFromAnchor(anchorDate, steps)
            RecurrenceFrequency.YEARLY -> clampedMonthsFromAnchor(anchorDate, steps * MONTHS_PER_YEAR)
        }
    }

    /**
     * Every occurrence of [rule] that is due on or before [asOf] and has not been accounted for.
     *
     * "Accounted for" is two different things, and both matter:
     * - [claimedDates] — a slot the server has already written an entry into. Includes slots whose
     *   entry was since deleted: the server keeps such a slot claimed forever, so a deliberately
     *   deleted occurrence must not come back as a placeholder.
     * - [skippedDates] — a slot a member skipped on purpose. Still consumes its slot, so it is
     *   filtered out of the result but not out of the walk.
     */
    fun dueOccurrences(
        rule: RecurringRule,
        asOf: LocalDate,
        claimedDates: Set<LocalDate> = emptySet(),
        skippedDates: Set<LocalDate> = emptySet(),
    ): List<LocalDate> =
        walkSlots(rule) { date ->
            when {
                date > asOf -> SlotVerdict.Stop
                date in claimedDates || date in skippedDates -> SlotVerdict.Consume
                else -> SlotVerdict.Take
            }
        }

    /**
     * The next [limit] occurrence dates strictly after [after], for a schedule preview.
     *
     * Skipped dates are left out — the preview is what the series is *going* to produce. Claimed
     * slots are not considered, because everything after [after] is by definition unwritten.
     */
    fun upcomingOccurrences(
        rule: RecurringRule,
        after: LocalDate,
        limit: Int,
        skippedDates: Set<LocalDate> = emptySet(),
    ): List<LocalDate> {
        require(limit > 0) { "limit must be positive, was $limit" }

        var taken = 0
        return walkSlots(rule) { date ->
            when {
                taken >= limit -> {
                    SlotVerdict.Stop
                }

                date <= after || date in skippedDates -> {
                    SlotVerdict.Consume
                }

                else -> {
                    taken++
                    SlotVerdict.Take
                }
            }
        }
    }

    /** Whether [date] is one of the dates [rule] produces — what an edit's `effectiveFrom` must be. */
    fun isOccurrenceDate(
        rule: RecurringRule,
        date: LocalDate,
    ): Boolean =
        walkSlots(rule) { slotDate ->
            when {
                slotDate > date -> SlotVerdict.Stop
                slotDate == date -> SlotVerdict.Take
                else -> SlotVerdict.Consume
            }
        }.isNotEmpty()

    /**
     * Walks the rule's slots in order, applying [verdict] to each date, until the rule's own end is
     * reached, the verdict says stop, or [MAX_SLOT_SCAN] slots have been examined.
     *
     * [RecurringEnd.Count] is checked against the slot index rather than the number of dates taken,
     * because a skipped occurrence consumes its slot without producing a date.
     */
    private fun walkSlots(
        rule: RecurringRule,
        verdict: (LocalDate) -> SlotVerdict,
    ): List<LocalDate> {
        val untilDate = (rule.end as? RecurringEnd.Until)?.date
        val occurrenceCount = (rule.end as? RecurringEnd.Count)?.count

        val dates = mutableListOf<LocalDate>()
        var slotIndex = 0
        while (slotIndex < MAX_SLOT_SCAN) {
            if (occurrenceCount != null && slotIndex >= occurrenceCount) break

            val date =
                occurrenceDateForSlot(
                    frequency = rule.frequency,
                    interval = rule.interval,
                    anchorDate = rule.startDate,
                    slotIndex = slotIndex,
                )
            if (untilDate != null && date > untilDate) break

            when (verdict(date)) {
                SlotVerdict.Stop -> return dates
                SlotVerdict.Take -> dates.add(date)
                SlotVerdict.Consume -> Unit
            }
            slotIndex++
        }
        return dates
    }

    /** What [walkSlots] should do with the slot it is looking at. */
    private enum class SlotVerdict {
        /** Include the date and move on. */
        Take,

        /** The slot is used up but produces no date — move on. */
        Consume,

        /** End the walk; nothing later can qualify. */
        Stop,
    }

    /**
     * [anchorDate] moved [monthsToAdd] months, keeping its day of the month where the target month
     * is long enough and clamping to that month's last day where it is not.
     *
     * Spelled out rather than delegated to `plus(DateTimeUnit.MONTH)` so the clamping rule is
     * pinned by this code and its tests, not by whichever behaviour the datetime library happens to
     * have. The server clamps via `YearMonth.atDay(min(day, lengthOfMonth))`; this must agree.
     */
    private fun clampedMonthsFromAnchor(
        anchorDate: LocalDate,
        monthsToAdd: Long,
    ): LocalDate {
        val firstOfTargetMonth =
            LocalDate(anchorDate.year, anchorDate.month, 1).plus(monthsToAdd, DateTimeUnit.MONTH)
        val lengthOfTargetMonth =
            firstOfTargetMonth.plus(1, DateTimeUnit.MONTH).minus(1, DateTimeUnit.DAY).day

        return LocalDate(
            firstOfTargetMonth.year,
            firstOfTargetMonth.month,
            minOf(anchorDate.day, lengthOfTargetMonth),
        )
    }
}
