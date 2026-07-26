package de.tabmates.features.tabgroup.domain.models

import kotlinx.datetime.LocalDate
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.time.Instant

class GroupActivityTest {
    @Test
    fun fallsBackToGroupTimestampWithoutEntries() {
        assertEquals(instant(500), latestActivityAt(instant(500), emptyList()))
    }

    @Test
    fun newerEntryWinsOverGroupTimestamp() {
        val entries = listOf(expense(id = "e1", lastModifiedMs = 100), expense(id = "e2", lastModifiedMs = 900))
        assertEquals(instant(900), latestActivityAt(instant(500), entries))
    }

    @Test
    fun groupTimestampWinsOverOlderEntries() {
        val entries = listOf(expense(id = "e1", lastModifiedMs = 100), expense(id = "e2", lastModifiedMs = 200))
        assertEquals(instant(500), latestActivityAt(instant(500), entries))
    }

    @Test
    fun deletedEntryStillCounts() {
        // A remote delete bumps lastModifiedAt — deleting an expense is activity too.
        val entries = listOf(expense(id = "e1", lastModifiedMs = 900, deletedAtMs = 900))
        assertEquals(instant(900), latestActivityAt(instant(500), entries))
    }

    @Test
    fun ignoresEntryDateAndUsesModificationTime() {
        // Backdated expense entered today must still lift the group.
        val entry =
            expense(id = "e1", lastModifiedMs = 900).copy(entryDate = LocalDate.parse("1970-01-01"))
        assertEquals(instant(900), latestActivityAt(instant(500), listOf(entry)))
    }

    private fun instant(epochMs: Long): Instant = Instant.fromEpochMilliseconds(epochMs)

    private fun expense(
        id: String,
        lastModifiedMs: Long,
        deletedAtMs: Long? = null,
    ): TabEntry.Expense =
        TabEntry.Expense(
            tabEntryId = id,
            groupId = "g",
            title = "",
            description = "",
            amount = 10.0,
            currencyCode = "EUR",
            creatorId = "a",
            paidByUserId = "a",
            entryDate = LocalDate.parse("1970-01-02"),
            createdAt = instant(0),
            lastModifiedAt = instant(lastModifiedMs),
            lastModifiedByUserId = "a",
            version = 0,
            deletedAt = deletedAtMs?.let { instant(it) },
            deletedByUserId = deletedAtMs?.let { "a" },
            splits = emptyList(),
        )
}
