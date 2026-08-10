package de.tabmates.features.tabgroup.domain.recurring

import kotlinx.datetime.LocalDate
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * The date arithmetic behind recurring entries, ported case-for-case from the server's
 * `RecurringOccurrenceGeneratorTest`.
 *
 * The server decides which occurrences get written; this decides which ones the client previews as
 * placeholders. They have to produce the same dates, so the cases that pin the server's month-length
 * behaviour are reproduced here verbatim rather than paraphrased.
 *
 * The server's per-run caps (`maxOccurrences`, `maxMonths`, the resume cursor) have no counterpart
 * here on purpose: those bound one sweep's write batch, while the client renders every due slot.
 */
class RecurringOccurrenceCalculatorTest {
    // region monthly and yearly clamping

    @Test
    fun `monthly anchored on the 31st borrows a day in February and gives it straight back`() {
        val dates =
            RecurringOccurrenceCalculator.dueOccurrences(
                rule = rule(RecurrenceFrequency.MONTHLY, startDate = LocalDate(2026, 1, 31)),
                asOf = LocalDate(2026, 4, 30),
            )

        // The regression the anchor-based helper exists for: stepping off the previous occurrence
        // would clamp to Feb 28 and then never recover the 31st, giving Mar 28 and Apr 28.
        assertEquals(
            listOf(
                LocalDate(2026, 1, 31),
                LocalDate(2026, 2, 28),
                LocalDate(2026, 3, 31),
                LocalDate(2026, 4, 30),
            ),
            dates,
        )
    }

    @Test
    fun `monthly anchored on the 31st clamps to 29 February in a leap year`() {
        val dates =
            RecurringOccurrenceCalculator.dueOccurrences(
                rule = rule(RecurrenceFrequency.MONTHLY, startDate = LocalDate(2028, 1, 31)),
                asOf = LocalDate(2028, 3, 31),
            )

        assertEquals(
            listOf(
                LocalDate(2028, 1, 31),
                LocalDate(2028, 2, 29),
                LocalDate(2028, 3, 31),
            ),
            dates,
        )
    }

    @Test
    fun `monthly anchored on 29 February falls on the 28th in a non-leap year`() {
        val date =
            RecurringOccurrenceCalculator.occurrenceDateForSlot(
                frequency = RecurrenceFrequency.MONTHLY,
                interval = 12,
                anchorDate = LocalDate(2028, 2, 29),
                slotIndex = 1,
            )

        assertEquals(LocalDate(2029, 2, 28), date)
    }

    @Test
    fun `yearly anchored on 29 February falls on the 28th in a non-leap year and recovers later`() {
        val anchor = LocalDate(2028, 2, 29)

        assertEquals(
            LocalDate(2029, 2, 28),
            RecurringOccurrenceCalculator.occurrenceDateForSlot(RecurrenceFrequency.YEARLY, 1, anchor, 1),
        )
        assertEquals(
            LocalDate(2032, 2, 29),
            RecurringOccurrenceCalculator.occurrenceDateForSlot(RecurrenceFrequency.YEARLY, 1, anchor, 4),
        )
    }

    @Test
    fun `monthly honours the interval`() {
        val dates =
            RecurringOccurrenceCalculator.dueOccurrences(
                rule =
                    rule(
                        RecurrenceFrequency.MONTHLY,
                        interval = 3,
                        startDate = LocalDate(2026, 1, 15),
                    ),
                asOf = LocalDate(2026, 7, 15),
            )

        assertEquals(
            listOf(
                LocalDate(2026, 1, 15),
                LocalDate(2026, 4, 15),
                LocalDate(2026, 7, 15),
            ),
            dates,
        )
    }

    @Test
    fun `every slot of a long monthly run is recomputed from the anchor`() {
        // Guards the clamping helper across a full year of short and long months in one pass: any
        // ratcheting bug shows up as a day that never returns to 31.
        val anchor = LocalDate(2026, 1, 31)
        val dates =
            (0..12).map {
                RecurringOccurrenceCalculator.occurrenceDateForSlot(
                    RecurrenceFrequency.MONTHLY,
                    1,
                    anchor,
                    it,
                )
            }

        assertEquals(
            listOf(31, 28, 31, 30, 31, 30, 31, 31, 30, 31, 30, 31, 31),
            dates.map { it.day },
        )
    }

    // endregion

    // region daily and weekly

    @Test
    fun `daily with interval one produces consecutive days`() {
        val dates =
            RecurringOccurrenceCalculator.dueOccurrences(
                rule = rule(RecurrenceFrequency.DAILY, startDate = LocalDate(2026, 1, 1)),
                asOf = LocalDate(2026, 1, 4),
            )

        assertEquals(
            listOf(
                LocalDate(2026, 1, 1),
                LocalDate(2026, 1, 2),
                LocalDate(2026, 1, 3),
                LocalDate(2026, 1, 4),
            ),
            dates,
        )
    }

    @Test
    fun `daily honours the interval`() {
        val dates =
            RecurringOccurrenceCalculator.dueOccurrences(
                rule = rule(RecurrenceFrequency.DAILY, interval = 3, startDate = LocalDate(2026, 1, 1)),
                asOf = LocalDate(2026, 1, 8),
            )

        assertEquals(
            listOf(
                LocalDate(2026, 1, 1),
                LocalDate(2026, 1, 4),
                LocalDate(2026, 1, 7),
            ),
            dates,
        )
    }

    @Test
    fun `weekly with interval two lands fortnightly on the anchor weekday`() {
        val dates =
            RecurringOccurrenceCalculator.dueOccurrences(
                rule = rule(RecurrenceFrequency.WEEKLY, interval = 2, startDate = LocalDate(2026, 1, 2)),
                asOf = LocalDate(2026, 1, 30),
            )

        assertEquals(
            listOf(
                LocalDate(2026, 1, 2),
                LocalDate(2026, 1, 16),
                LocalDate(2026, 1, 30),
            ),
            dates,
        )
    }

    // endregion

    // region end conditions

    @Test
    fun `until date is inclusive and stops the series`() {
        val dates =
            RecurringOccurrenceCalculator.dueOccurrences(
                rule =
                    rule(
                        RecurrenceFrequency.DAILY,
                        startDate = LocalDate(2026, 1, 1),
                        end = RecurringEnd.Until(LocalDate(2026, 1, 5)),
                    ),
                asOf = LocalDate(2026, 12, 31),
            )

        assertEquals(LocalDate(2026, 1, 5), dates.last())
        assertEquals(5, dates.size)
    }

    @Test
    fun `occurrence count produces exactly that many`() {
        val dates =
            RecurringOccurrenceCalculator.dueOccurrences(
                rule =
                    rule(
                        RecurrenceFrequency.DAILY,
                        startDate = LocalDate(2026, 1, 1),
                        end = RecurringEnd.Count(3),
                    ),
                asOf = LocalDate(2026, 12, 31),
            )

        assertEquals(3, dates.size)
    }

    @Test
    fun `a series starting in the future yields nothing yet`() {
        val dates =
            RecurringOccurrenceCalculator.dueOccurrences(
                rule = rule(RecurrenceFrequency.MONTHLY, startDate = LocalDate(2026, 9, 1)),
                asOf = LocalDate(2026, 8, 10),
            )

        assertTrue(dates.isEmpty())
    }

    // endregion

    // region skipped and claimed slots

    @Test
    fun `a skipped date is left out but still consumes its slot`() {
        val dates =
            RecurringOccurrenceCalculator.dueOccurrences(
                rule =
                    rule(
                        RecurrenceFrequency.DAILY,
                        startDate = LocalDate(2026, 1, 1),
                        end = RecurringEnd.Count(5),
                    ),
                asOf = LocalDate(2026, 12, 31),
                skippedDates = setOf(LocalDate(2026, 1, 3)),
            )

        // Four dates, not five — and the series still ends after its fifth slot rather than running
        // a day longer to make up for the skip.
        assertEquals(
            listOf(
                LocalDate(2026, 1, 1),
                LocalDate(2026, 1, 2),
                LocalDate(2026, 1, 4),
                LocalDate(2026, 1, 5),
            ),
            dates,
        )
    }

    @Test
    fun `a claimed slot is left out and still consumes its slot`() {
        // A claimed slot is one the server has already written an entry for. It must not be
        // previewed again, and it must not push the count-limited series a day further out.
        val dates =
            RecurringOccurrenceCalculator.dueOccurrences(
                rule =
                    rule(
                        RecurrenceFrequency.DAILY,
                        startDate = LocalDate(2026, 1, 1),
                        end = RecurringEnd.Count(4),
                    ),
                asOf = LocalDate(2026, 12, 31),
                claimedDates = setOf(LocalDate(2026, 1, 1), LocalDate(2026, 1, 2)),
            )

        assertEquals(listOf(LocalDate(2026, 1, 3), LocalDate(2026, 1, 4)), dates)
    }

    // endregion

    // region upcoming preview

    @Test
    fun `upcoming returns the next dates strictly after the given day`() {
        val dates =
            RecurringOccurrenceCalculator.upcomingOccurrences(
                rule = rule(RecurrenceFrequency.MONTHLY, startDate = LocalDate(2026, 1, 15)),
                after = LocalDate(2026, 3, 15),
                limit = 3,
            )

        assertEquals(
            listOf(
                LocalDate(2026, 4, 15),
                LocalDate(2026, 5, 15),
                LocalDate(2026, 6, 15),
            ),
            dates,
        )
    }

    @Test
    fun `upcoming leaves out skipped dates`() {
        val dates =
            RecurringOccurrenceCalculator.upcomingOccurrences(
                rule = rule(RecurrenceFrequency.MONTHLY, startDate = LocalDate(2026, 1, 15)),
                after = LocalDate(2026, 1, 20),
                limit = 2,
                skippedDates = setOf(LocalDate(2026, 2, 15)),
            )

        assertEquals(listOf(LocalDate(2026, 3, 15), LocalDate(2026, 4, 15)), dates)
    }

    @Test
    fun `upcoming stops at the series end rather than padding to the limit`() {
        val dates =
            RecurringOccurrenceCalculator.upcomingOccurrences(
                rule =
                    rule(
                        RecurrenceFrequency.MONTHLY,
                        startDate = LocalDate(2026, 1, 15),
                        end = RecurringEnd.Count(3),
                    ),
                after = LocalDate(2026, 1, 20),
                limit = 6,
            )

        assertEquals(listOf(LocalDate(2026, 2, 15), LocalDate(2026, 3, 15)), dates)
    }

    // endregion

    // region edit anchor

    @Test
    fun `isOccurrenceDate accepts a date the schedule produces and rejects one it does not`() {
        val monthly = rule(RecurrenceFrequency.MONTHLY, startDate = LocalDate(2026, 1, 15))

        assertTrue(RecurringOccurrenceCalculator.isOccurrenceDate(monthly, LocalDate(2026, 5, 15)))
        assertFalse(RecurringOccurrenceCalculator.isOccurrenceDate(monthly, LocalDate(2026, 5, 14)))
    }

    @Test
    fun `isOccurrenceDate rejects a date past the series end`() {
        val monthly =
            rule(
                RecurrenceFrequency.MONTHLY,
                startDate = LocalDate(2026, 1, 15),
                end = RecurringEnd.Until(LocalDate(2026, 3, 31)),
            )

        assertTrue(RecurringOccurrenceCalculator.isOccurrenceDate(monthly, LocalDate(2026, 3, 15)))
        assertFalse(RecurringOccurrenceCalculator.isOccurrenceDate(monthly, LocalDate(2026, 4, 15)))
    }

    // endregion

    private fun rule(
        frequency: RecurrenceFrequency,
        interval: Int = 1,
        startDate: LocalDate,
        end: RecurringEnd = RecurringEnd.Never,
    ) = RecurringRule(
        ruleId = "rule-1",
        title = "Rent",
        description = "",
        amount = 100.0,
        currencyCode = "EUR",
        exchangeRate = null,
        paidByUserId = "user-1",
        receivedByUserId = null,
        splits = emptyList(),
        frequency = frequency,
        interval = interval,
        startDate = startDate,
        end = end,
    )
}
