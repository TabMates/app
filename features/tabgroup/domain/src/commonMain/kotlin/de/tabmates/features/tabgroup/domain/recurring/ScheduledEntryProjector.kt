package de.tabmates.features.tabgroup.domain.recurring

import de.tabmates.features.tabgroup.domain.models.TabEntry
import de.tabmates.features.tabgroup.domain.models.TabEntrySplit
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.atStartOfDayIn

/**
 * Projects the occurrences a group's schedules owe but the server has not written yet.
 *
 * The server is the only writer of recurring entries — balances are shared, so a ledger that only
 * advanced when someone opened the app would leave every other member's numbers stale. That leaves
 * a gap between an occurrence falling due and its entry arriving: up to a sweep interval when
 * online, indefinitely when offline. This fills the gap with placeholders, so the numbers a member
 * sees are the numbers they will still see once the entry lands.
 *
 * Placeholders are ordinary [TabEntry] values carrying [TabEntry.isScheduledPlaceholder], which is
 * what lets every existing balance calculator consume them unchanged. They are never persisted and
 * never sent anywhere.
 */
object ScheduledEntryProjector {
    /**
     * Placeholders for every occurrence due on or before [today] that has no entry.
     *
     * @param series the group's schedules, as mirrored from the server
     * @param existingEntries the group's real entries, used to recognise slots already written
     * @param claimedSlots slots the server has written at some point, **including ones whose entry
     *   was since deleted**. The server keeps such a slot claimed forever, and locally a
     *   soft-deleted entry is dropped from the table entirely, so without this record a
     *   deliberately deleted occurrence would come back as a placeholder on every projection.
     * @param today the calendar day to measure against. Use the same UTC day the server's sweep
     *   uses, or the two disagree about which occurrences are owed at the edges of the day.
     */
    fun project(
        series: List<RecurringSeries>,
        existingEntries: List<TabEntry>,
        claimedSlots: Set<RecurringSlot>,
        today: LocalDate,
    ): List<TabEntry> {
        if (series.isEmpty()) return emptyList()

        val claimedBySeries =
            buildMap<String, MutableSet<LocalDate>> {
                claimedSlots.forEach { slot ->
                    getOrPut(slot.seriesId) { mutableSetOf() }.add(slot.occurrenceDate)
                }
                // Entries present locally are claimed whether or not the claim record caught them,
                // which keeps a projection correct even on the very first sync of a device.
                existingEntries.forEach { entry ->
                    val seriesId = entry.recurringSeriesId ?: return@forEach
                    val date = entry.recurringOccurrenceDate ?: return@forEach
                    getOrPut(seriesId) { mutableSetOf() }.add(date)
                }
            }

        return series.flatMap { candidate ->
            // A parked series is one whose template names somebody who has left the group. The
            // server writes nothing for it until a member repairs the template, so previewing its
            // occurrences would promise entries that are not coming.
            if (!candidate.isActive || candidate.needsAttention) return@flatMap emptyList()

            RecurringOccurrenceCalculator
                .dueOccurrences(
                    rule = candidate.rule,
                    asOf = today,
                    claimedDates = claimedBySeries[candidate.seriesId].orEmpty(),
                    skippedDates = candidate.skippedOccurrenceDates,
                ).mapNotNull { occurrenceDate -> candidate.toPlaceholder(occurrenceDate) }
        }
    }

    /**
     * Builds one placeholder from a series' template.
     *
     * The id is synthetic and derived from the slot, so it is stable across projections — a list
     * key that survives recomposition, and one that cannot collide with a server id.
     *
     * Null when the template could not produce a valid entry, which only a settlement series
     * missing its receiver can do. Inventing one would move money to the wrong person.
     */
    private fun RecurringSeries.toPlaceholder(occurrenceDate: LocalDate): TabEntry? {
        val placeholderId = placeholderId(seriesId, occurrenceDate)
        // The occurrence does not exist yet, so there is no creation instant to report. Midnight UTC
        // on the day it falls due is the honest answer, and it orders sanely against real entries.
        val dueAt = occurrenceDate.atStartOfDayIn(TimeZone.UTC)

        return when (entryType) {
            RecurringEntryType.EXPENSE -> {
                TabEntry.Expense(
                    tabEntryId = placeholderId,
                    groupId = groupId,
                    title = rule.title,
                    description = rule.description,
                    amount = rule.amount,
                    currencyCode = rule.currencyCode,
                    exchangeRate = rule.exchangeRate,
                    creatorId = createdBy.userId,
                    paidByUserId = rule.paidByUserId,
                    entryDate = occurrenceDate,
                    createdAt = dueAt,
                    lastModifiedAt = dueAt,
                    lastModifiedByUserId = createdBy.userId,
                    version = 0,
                    deletedAt = null,
                    deletedByUserId = null,
                    splits = rule.splits.toPlaceholderSplits(placeholderId),
                    recurringSeriesId = seriesId,
                    recurringOccurrenceDate = occurrenceDate,
                    isScheduledPlaceholder = true,
                )
            }

            RecurringEntryType.INCOME -> {
                TabEntry.Income(
                    tabEntryId = placeholderId,
                    groupId = groupId,
                    title = rule.title,
                    description = rule.description,
                    amount = rule.amount,
                    currencyCode = rule.currencyCode,
                    exchangeRate = rule.exchangeRate,
                    creatorId = createdBy.userId,
                    paidByUserId = rule.paidByUserId,
                    entryDate = occurrenceDate,
                    createdAt = dueAt,
                    lastModifiedAt = dueAt,
                    lastModifiedByUserId = createdBy.userId,
                    version = 0,
                    deletedAt = null,
                    deletedByUserId = null,
                    splits = rule.splits.toPlaceholderSplits(placeholderId),
                    recurringSeriesId = seriesId,
                    recurringOccurrenceDate = occurrenceDate,
                    isScheduledPlaceholder = true,
                )
            }

            RecurringEntryType.SETTLEMENT -> {
                TabEntry.Settlement(
                    tabEntryId = placeholderId,
                    groupId = groupId,
                    title = rule.title,
                    description = rule.description,
                    amount = rule.amount,
                    currencyCode = rule.currencyCode,
                    exchangeRate = rule.exchangeRate,
                    creatorId = createdBy.userId,
                    paidByUserId = rule.paidByUserId,
                    entryDate = occurrenceDate,
                    createdAt = dueAt,
                    lastModifiedAt = dueAt,
                    lastModifiedByUserId = createdBy.userId,
                    version = 0,
                    deletedAt = null,
                    deletedByUserId = null,
                    // A settlement series always carries a receiver; the server rejects one without.
                    // Falling back to the payer would silently move money to the wrong person, so an
                    // incomplete template produces no placeholder at all instead.
                    receivedByUserId = rule.receivedByUserId ?: return null,
                    recurringSeriesId = seriesId,
                    recurringOccurrenceDate = occurrenceDate,
                    isScheduledPlaceholder = true,
                )
            }
        }
    }

    private fun List<RecurringTemplateSplit>.toPlaceholderSplits(placeholderEntryId: String) =
        map { split ->
            TabEntrySplit(
                splitId = "$placeholderEntryId:${split.participantId}",
                tabEntryId = placeholderEntryId,
                participantId = split.participantId,
                splitType = split.splitType,
                value = split.value,
                resolvedAmount = split.resolvedAmount,
            )
        }

    /** The synthetic id a placeholder for one slot always gets. */
    fun placeholderId(
        seriesId: String,
        occurrenceDate: LocalDate,
    ): String = "$PLACEHOLDER_ID_PREFIX$seriesId:$occurrenceDate"

    private const val PLACEHOLDER_ID_PREFIX = "scheduled:"
}

/** One slot of a recurring series: the coordinate the server guarantees at most one entry for. */
data class RecurringSlot(
    val seriesId: String,
    val occurrenceDate: LocalDate,
)
