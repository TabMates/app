package de.tabmates.features.tabgroup.domain.recurring

import de.tabmates.core.domain.util.DataError
import de.tabmates.core.domain.util.EmptyResult
import de.tabmates.core.domain.util.Result
import de.tabmates.features.tabgroup.domain.models.SplitType
import kotlinx.coroutines.flow.Flow
import kotlinx.datetime.LocalDate

/**
 * Recurring schedules for a group.
 *
 * Reads are offline-first off the local mirror. **Writes are not** — unlike tab entries there is no
 * outbox behind these: creating or changing a schedule is a rare, deliberate act, and queuing one
 * offline would leave a standing instruction to write into other people's ledgers pending on a
 * device nobody is watching. Every write here needs a connection and reports its own failure.
 */
interface RecurringSeriesRepository {
    fun getSeriesForGroup(groupId: String): Flow<List<RecurringSeries>>

    fun getSeriesById(seriesId: String): Flow<RecurringSeries?>

    /**
     * Slots in this group the server has written an entry into at some point.
     *
     * Needed alongside the entries themselves because a soft-deleted entry is dropped locally while
     * its slot stays claimed forever server-side. Without this, [ScheduledEntryProjector] would see
     * a free slot and keep re-projecting an occurrence somebody deleted on purpose.
     */
    fun getClaimedSlotsForGroup(groupId: String): Flow<Set<RecurringSlot>>

    /**
     * [seriesId] is client-generated and doubles as the idempotency key, so a create retried after
     * a dropped response cannot produce a second schedule writing the same rent twice a month.
     */
    suspend fun createSeries(
        seriesId: String,
        groupId: String,
        template: RecurringTemplate,
    ): Result<RecurringSeries, DataError.Remote>

    /**
     * Applies [template] from [effectiveFrom] onwards, leaving earlier occurrences untouched.
     *
     * [effectiveFrom] must be a future date the current schedule actually produces, and must equal
     * `template.startDate` — otherwise the server rejects it rather than silently re-anchoring the
     * rhythm to whichever day the edit was made. Occurrences between now and [effectiveFrom] are
     * deliberately abandoned, not replayed.
     *
     * This is also the only thing that clears [RecurringSeries.needsAttention].
     */
    suspend fun updateSeries(
        seriesId: String,
        effectiveFrom: LocalDate,
        template: RecurringTemplate,
    ): Result<RecurringSeries, DataError.Remote>

    /** Skips one future occurrence. The slot is still consumed, so the series does not run longer. */
    suspend fun skipOccurrence(
        seriesId: String,
        occurrenceDate: LocalDate,
    ): EmptyResult<DataError.Remote>

    suspend fun unskipOccurrence(
        seriesId: String,
        occurrenceDate: LocalDate,
    ): EmptyResult<DataError.Remote>

    /** Stops the schedule. Entries it already produced stay exactly as they are. */
    suspend fun endSeries(seriesId: String): EmptyResult<DataError.Remote>

    /**
     * Pulls a group's schedules from the server and replaces the local mirror for that group.
     *
     * Needed on top of the account-wide sync because the delta only carries series changed since
     * the cursor: a group that just became visible arrives without its existing schedules.
     */
    suspend fun refreshSeriesForGroup(groupId: String): EmptyResult<DataError.Remote>
}

/** The template half of a create or edit — what each occurrence will look like, and how it repeats. */
data class RecurringTemplate(
    val entryType: RecurringEntryType,
    val title: String,
    val description: String,
    val amount: Double,
    val currencyCode: String,
    /** Fallback only; the server resolves a live rate per occurrence when it writes one. */
    val exchangeRate: Double?,
    val paidByUserId: String,
    /** Required for [RecurringEntryType.SETTLEMENT], rejected for the other two. */
    val receivedByUserId: String?,
    /** Required for expenses and incomes, rejected for settlements. */
    val splits: List<NewRecurringTemplateSplit>,
    val frequency: RecurrenceFrequency,
    val interval: Int,
    /** May not be in the past. For an edit it must equal the request's `effectiveFrom`. */
    val startDate: LocalDate,
    val end: RecurringEnd,
)

/** A split as entered on the form: no id yet, and the resolved amount computed from the total. */
data class NewRecurringTemplateSplit(
    val participantId: String,
    val splitType: SplitType,
    val value: Double,
    val resolvedAmount: Double,
)
