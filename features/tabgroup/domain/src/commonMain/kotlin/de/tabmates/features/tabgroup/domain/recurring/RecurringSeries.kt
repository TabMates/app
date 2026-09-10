package de.tabmates.features.tabgroup.domain.recurring

import de.tabmates.features.tabgroup.domain.models.GroupParticipant
import de.tabmates.features.tabgroup.domain.models.SplitType
import kotlinx.datetime.LocalDate
import kotlin.time.Instant

/**
 * A repeating tab entry: a stored template plus a repetition rule that a server-side sweep turns
 * into ordinary entries.
 *
 * The client never generates entries. It mirrors the schedule so it can render an occurrence that
 * is due but not yet written (see `ScheduledEntryProjector`), which is what keeps the ledger
 * readable offline and in the window before the sweep runs.
 *
 * The series is the stable identity; [rule] is the current revision of its template. The server
 * appends a revision on every "this and future" edit and only ever ships the newest one, so there
 * is no local revision history to keep.
 */
data class RecurringSeries(
    val seriesId: String,
    val groupId: String,
    val entryType: RecurringEntryType,
    /** False once ended, either by a member or by the rule reaching its own end. */
    val isActive: Boolean,
    /**
     * The template names somebody who has left the group, so nothing is being generated until a
     * member repairs it. The only series state that needs a human — surface it.
     */
    val needsAttention: Boolean,
    val createdAt: Instant,
    val createdBy: GroupParticipant,
    val updatedAt: Instant,
    val rule: RecurringRule,
    /**
     * Future occurrence dates a member chose to skip. A skipped slot is still consumed, so it must
     * be excluded from the dates rendered as placeholders **and** counted against
     * [RecurringEnd.Count].
     */
    val skippedOccurrenceDates: Set<LocalDate> = emptySet(),
)

/** The template and schedule a series currently repeats. */
data class RecurringRule(
    val ruleId: String,
    val title: String,
    val description: String,
    val amount: Double,
    val currencyCode: String,
    /**
     * Fallback rate only. Each occurrence resolves its own rate when the server writes it, so a
     * placeholder rendered from this value can differ slightly from the entry that lands.
     */
    val exchangeRate: Double?,
    val paidByUserId: String,
    /** Set for [RecurringEntryType.SETTLEMENT] only. */
    val receivedByUserId: String?,
    /** Empty for [RecurringEntryType.SETTLEMENT], which has no splits. */
    val splits: List<RecurringTemplateSplit>,
    val frequency: RecurrenceFrequency,
    /** Repeat every N periods of [frequency]; 1 means every period. Always positive. */
    val interval: Int,
    /** The first occurrence, and the anchor every later occurrence date is computed from. */
    val startDate: LocalDate,
    val end: RecurringEnd,
)

/** A participant's share in a recurring template, copied verbatim into every occurrence. */
data class RecurringTemplateSplit(
    val splitId: String?,
    val participantId: String,
    val splitType: SplitType,
    val value: Double,
    val resolvedAmount: Double,
)

/**
 * Which kind of entry a series produces. Fixed for the life of the series — the server rejects an
 * edit that changes it.
 */
enum class RecurringEntryType {
    EXPENSE,
    INCOME,
    SETTLEMENT,
}
