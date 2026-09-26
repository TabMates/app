package de.tabmates.features.tabgroup.presentation.navigation.groupoverview

import de.tabmates.features.tabgroup.domain.recurring.RecurrenceFrequency
import de.tabmates.features.tabgroup.domain.recurring.RecurringEnd
import de.tabmates.features.tabgroup.presentation.testing.RecurringFixtures
import kotlinx.datetime.LocalDate
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * The upcoming section shows what the group's schedules are *about to* produce. Nothing it lists has
 * moved a balance yet — the occurrences that have are placeholders down in the ledger instead.
 */
class UpcomingSchedulesTest {
    private val today = LocalDate.parse("2026-03-10")

    @Test
    fun `an active schedule is listed with its next date`() {
        val series =
            RecurringFixtures.series(
                startDate = LocalDate.parse("2026-01-05"),
                frequency = RecurrenceFrequency.MONTHLY,
            )

        val upcoming = upcomingSchedules(listOf(series), today)

        assertEquals(1, upcoming.size)
        assertEquals(LocalDate.parse("2026-04-05"), upcoming.single().nextDate)
    }

    @Test
    fun `an ended schedule is left out`() {
        // Ended schedules still explain entries that exist, but they belong on the schedules screen:
        // a section named for what is coming must not list something that produces nothing.
        val series = RecurringFixtures.series(isActive = false)

        assertTrue(upcomingSchedules(listOf(series), today).isEmpty())
    }

    @Test
    fun `an active schedule with no dates left is left out`() {
        val series =
            RecurringFixtures.series(
                startDate = LocalDate.parse("2026-01-05"),
                frequency = RecurrenceFrequency.MONTHLY,
                end = RecurringEnd.Count(2),
            )

        assertTrue(upcomingSchedules(listOf(series), today).isEmpty())
    }

    @Test
    fun `a parked schedule is listed without a date`() {
        // The server writes nothing for a parked schedule until someone repairs the template, so
        // promising a date would promise entries that are not coming.
        val series = RecurringFixtures.series(needsAttention = true)

        val upcoming = upcomingSchedules(listOf(series), today)

        assertEquals(1, upcoming.size)
        assertNull(upcoming.single().nextDate)
    }

    @Test
    fun `parked schedules sort ahead of dated ones which sort by date`() {
        // The section shows only the first few rows until it is expanded, and the parked one is the
        // only row asking for something — it must not be the one that gets hidden.
        val soon =
            RecurringFixtures.series(
                seriesId = "soon",
                startDate = LocalDate.parse("2026-03-12"),
                frequency = RecurrenceFrequency.MONTHLY,
            )
        val later =
            RecurringFixtures.series(
                seriesId = "later",
                startDate = LocalDate.parse("2026-03-28"),
                frequency = RecurrenceFrequency.MONTHLY,
            )
        val parked = RecurringFixtures.series(seriesId = "parked", needsAttention = true)

        val upcoming = upcomingSchedules(listOf(later, soon, parked), today)

        assertEquals(listOf("parked", "soon", "later"), upcoming.map { it.series.seriesId })
    }

    @Test
    fun `a skipped next occurrence advances to the one after it`() {
        val series =
            RecurringFixtures.series(
                startDate = LocalDate.parse("2026-01-05"),
                frequency = RecurrenceFrequency.MONTHLY,
                skipped = setOf(LocalDate.parse("2026-04-05")),
            )

        val upcoming = upcomingSchedules(listOf(series), today)

        assertEquals(LocalDate.parse("2026-05-05"), upcoming.single().nextDate)
    }
}
