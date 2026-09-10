package de.tabmates.features.tabgroup.domain.recurring

import de.tabmates.features.tabgroup.domain.models.GroupParticipant
import de.tabmates.features.tabgroup.domain.models.ParticipantType
import de.tabmates.features.tabgroup.domain.models.SplitType
import de.tabmates.features.tabgroup.domain.models.TabEntry
import kotlinx.datetime.LocalDate
import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertTrue
import kotlin.time.Instant

class ScheduledEntryProjectorTest {
    private val today = LocalDate(2026, 3, 15)
    private val creator = GroupParticipant("user-1", "Ada", ParticipantType.REGISTERED)

    @Test
    fun `projects one placeholder per due unwritten occurrence`() {
        val projected =
            ScheduledEntryProjector.project(
                series = listOf(series(startDate = LocalDate(2026, 1, 15))),
                existingEntries = emptyList(),
                claimedSlots = emptySet(),
                today = today,
            )

        assertContentEquals(
            listOf(LocalDate(2026, 1, 15), LocalDate(2026, 2, 15), LocalDate(2026, 3, 15)),
            projected.map { it.entryDate },
        )
        assertTrue(projected.all { it.isScheduledPlaceholder })
        assertTrue(projected.all { it.recurringSeriesId == "series-1" })
    }

    @Test
    fun `a slot with a local entry produces no placeholder`() {
        val written = expense(occurrenceDate = LocalDate(2026, 2, 15))

        val projected =
            ScheduledEntryProjector.project(
                series = listOf(series(startDate = LocalDate(2026, 1, 15))),
                existingEntries = listOf(written),
                claimedSlots = emptySet(),
                today = today,
            )

        assertContentEquals(
            listOf(LocalDate(2026, 1, 15), LocalDate(2026, 3, 15)),
            projected.map { it.entryDate },
        )
    }

    @Test
    fun `a deleted occurrence stays gone because its slot is still claimed`() {
        // The regression the claim record exists for. The server keeps a slot claimed forever, but
        // locally a soft-deleted entry is dropped from the table outright — so with only the entry
        // list to go on, an occurrence someone deleted on purpose reappears as a placeholder on
        // every projection, and no amount of syncing gets rid of it.
        val projected =
            ScheduledEntryProjector.project(
                series = listOf(series(startDate = LocalDate(2026, 1, 15))),
                existingEntries = emptyList(),
                claimedSlots = setOf(RecurringSlot("series-1", LocalDate(2026, 2, 15))),
                today = today,
            )

        assertContentEquals(
            listOf(LocalDate(2026, 1, 15), LocalDate(2026, 3, 15)),
            projected.map { it.entryDate },
        )
    }

    @Test
    fun `a skipped occurrence produces no placeholder`() {
        val projected =
            ScheduledEntryProjector.project(
                series =
                    listOf(
                        series(
                            startDate = LocalDate(2026, 1, 15),
                            skipped = setOf(LocalDate(2026, 2, 15)),
                        ),
                    ),
                existingEntries = emptyList(),
                claimedSlots = emptySet(),
                today = today,
            )

        assertContentEquals(
            listOf(LocalDate(2026, 1, 15), LocalDate(2026, 3, 15)),
            projected.map { it.entryDate },
        )
    }

    @Test
    fun `a parked series produces nothing`() {
        // needsAttention means the server is writing nothing until a human repairs the template.
        // Previewing occurrences would promise entries that are not coming.
        val projected =
            ScheduledEntryProjector.project(
                series = listOf(series(startDate = LocalDate(2026, 1, 15), needsAttention = true)),
                existingEntries = emptyList(),
                claimedSlots = emptySet(),
                today = today,
            )

        assertTrue(projected.isEmpty())
    }

    @Test
    fun `an ended series produces nothing`() {
        val projected =
            ScheduledEntryProjector.project(
                series = listOf(series(startDate = LocalDate(2026, 1, 15), isActive = false)),
                existingEntries = emptyList(),
                claimedSlots = emptySet(),
                today = today,
            )

        assertTrue(projected.isEmpty())
    }

    @Test
    fun `future occurrences are not projected`() {
        val projected =
            ScheduledEntryProjector.project(
                series = listOf(series(startDate = LocalDate(2026, 4, 1))),
                existingEntries = emptyList(),
                claimedSlots = emptySet(),
                today = today,
            )

        assertTrue(projected.isEmpty())
    }

    @Test
    fun `placeholder carries the template's splits and amount`() {
        val projected =
            ScheduledEntryProjector.project(
                series = listOf(series(startDate = today)),
                existingEntries = emptyList(),
                claimedSlots = emptySet(),
                today = today,
            )

        val placeholder = assertIs<TabEntry.Expense>(projected.single())
        assertEquals(120.0, placeholder.amount)
        assertEquals("EUR", placeholder.currencyCode)
        assertContentEquals(listOf("user-1", "user-2"), placeholder.splits.map { it.participantId })
        assertEquals(listOf(60.0, 60.0), placeholder.splits.map { it.resolvedAmount })
    }

    @Test
    fun `a settlement series without a receiver produces no placeholder`() {
        // The server rejects such a template, so this is unreachable in practice — but defaulting
        // the receiver would move money to the wrong person, so it must produce nothing instead.
        val broken =
            series(startDate = today).let {
                it.copy(
                    entryType = RecurringEntryType.SETTLEMENT,
                    rule = it.rule.copy(receivedByUserId = null, splits = emptyList()),
                )
            }

        val projected =
            ScheduledEntryProjector.project(
                series = listOf(broken),
                existingEntries = emptyList(),
                claimedSlots = emptySet(),
                today = today,
            )

        assertTrue(projected.isEmpty())
    }

    @Test
    fun `placeholder ids are stable across projections`() {
        fun projectOnce() =
            ScheduledEntryProjector.project(
                series = listOf(series(startDate = LocalDate(2026, 1, 15))),
                existingEntries = emptyList(),
                claimedSlots = emptySet(),
                today = today,
            )

        assertContentEquals(
            projectOnce().map { it.tabEntryId },
            projectOnce().map { it.tabEntryId },
        )
    }

    private fun series(
        startDate: LocalDate,
        isActive: Boolean = true,
        needsAttention: Boolean = false,
        skipped: Set<LocalDate> = emptySet(),
    ) = RecurringSeries(
        seriesId = "series-1",
        groupId = "group-1",
        entryType = RecurringEntryType.EXPENSE,
        isActive = isActive,
        needsAttention = needsAttention,
        createdAt = Instant.fromEpochMilliseconds(0),
        createdBy = creator,
        updatedAt = Instant.fromEpochMilliseconds(0),
        rule =
            RecurringRule(
                ruleId = "rule-1",
                title = "Rent",
                description = "",
                amount = 120.0,
                currencyCode = "EUR",
                exchangeRate = null,
                paidByUserId = "user-1",
                receivedByUserId = null,
                splits =
                    listOf(
                        RecurringTemplateSplit(null, "user-1", SplitType.EQUAL, 1.0, 60.0),
                        RecurringTemplateSplit(null, "user-2", SplitType.EQUAL, 1.0, 60.0),
                    ),
                frequency = RecurrenceFrequency.MONTHLY,
                interval = 1,
                startDate = startDate,
                end = RecurringEnd.Never,
            ),
        skippedOccurrenceDates = skipped,
    )

    private fun expense(occurrenceDate: LocalDate) =
        TabEntry.Expense(
            tabEntryId = "entry-1",
            groupId = "group-1",
            title = "Rent",
            description = "",
            amount = 120.0,
            currencyCode = "EUR",
            creatorId = "user-1",
            paidByUserId = "user-1",
            entryDate = occurrenceDate,
            createdAt = Instant.fromEpochMilliseconds(0),
            lastModifiedAt = Instant.fromEpochMilliseconds(0),
            lastModifiedByUserId = "user-1",
            version = 1,
            deletedAt = null,
            deletedByUserId = null,
            splits = emptyList(),
            recurringSeriesId = "series-1",
            recurringOccurrenceDate = occurrenceDate,
        )
}
