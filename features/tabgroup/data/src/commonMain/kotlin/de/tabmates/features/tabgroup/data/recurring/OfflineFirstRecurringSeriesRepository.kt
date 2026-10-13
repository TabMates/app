package de.tabmates.features.tabgroup.data.recurring

import de.tabmates.core.domain.util.DataError
import de.tabmates.core.domain.util.EmptyResult
import de.tabmates.core.domain.util.Result
import de.tabmates.core.domain.util.asEmptyResult
import de.tabmates.core.domain.util.map
import de.tabmates.core.domain.util.onSuccess
import de.tabmates.features.tabgroup.data.mappers.toDomain
import de.tabmates.features.tabgroup.data.mappers.toDto
import de.tabmates.features.tabgroup.data.sync.RecurringSeriesLocalWriter
import de.tabmates.features.tabgroup.database.TabMatesDatabase
import de.tabmates.features.tabgroup.database.entities.RecurringExceptionEntity
import de.tabmates.features.tabgroup.domain.recurring.RecurringSeries
import de.tabmates.features.tabgroup.domain.recurring.RecurringSeriesRepository
import de.tabmates.features.tabgroup.domain.recurring.RecurringSlot
import de.tabmates.features.tabgroup.domain.recurring.RecurringTemplate
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.datetime.LocalDate
import org.koin.core.annotation.Single
import kotlin.time.Clock

/**
 * Reads schedules from the local mirror, writes them straight to the server.
 *
 * The asymmetry is deliberate and is the one place this repository departs from how tab entries
 * work. Reads have to survive offline, because a schedule is what lets the group screen project the
 * occurrences the server has not written yet. Writes must not be queued: a schedule is a standing
 * instruction to write into other people's shared ledgers, and one sitting in an outbox on a device
 * nobody is watching would fire days later against a group that has moved on. Every write here
 * needs a connection and reports its own failure.
 */
@Single(binds = [RecurringSeriesRepository::class])
class OfflineFirstRecurringSeriesRepository(
    private val service: RecurringSeriesService,
    private val database: TabMatesDatabase,
    private val localWriter: RecurringSeriesLocalWriter,
) : RecurringSeriesRepository {
    override fun getSeriesForGroup(groupId: String): Flow<List<RecurringSeries>> =
        database.recurringSeriesDao
            .observeSeriesByGroupId(groupId)
            .map { rows -> rows.map { it.toDomain() } }

    override fun getSeriesById(seriesId: String): Flow<RecurringSeries?> =
        database.recurringSeriesDao
            .observeSeriesById(seriesId)
            .map { it?.toDomain() }

    override fun getClaimedSlotsForGroup(groupId: String): Flow<Set<RecurringSlot>> =
        database.recurringSlotClaimDao
            .observeClaimsForGroup(groupId)
            .map { claims ->
                claims.mapTo(mutableSetOf()) {
                    RecurringSlot(it.seriesId, LocalDate.parse(it.occurrenceDate))
                }
            }

    override suspend fun createSeries(
        seriesId: String,
        groupId: String,
        template: RecurringTemplate,
    ): Result<RecurringSeries, DataError.Remote> =
        service
            .createSeries(seriesId = seriesId, groupId = groupId, template = template.toDto())
            .onSuccess { localWriter.persist(listOf(it)) }
            .map { it.toDomain() }

    override suspend fun updateSeries(
        seriesId: String,
        effectiveFrom: LocalDate,
        template: RecurringTemplate,
    ): Result<RecurringSeries, DataError.Remote> =
        service
            .updateSeries(seriesId = seriesId, effectiveFrom = effectiveFrom, template = template.toDto())
            .onSuccess { localWriter.persist(listOf(it)) }
            .map { it.toDomain() }

    /**
     * The three endpoints below answer with no body, so the local mirror is nudged by hand and then
     * reconciled from the server. Applying the change locally first is what keeps the screen from
     * snapping back for the length of the refresh round trip; the refresh is what makes the local
     * guess authoritative, including anything another member changed in the meantime.
     *
     * A failed refresh is not a failed write — the write already succeeded, and the next sync will
     * carry the schedule anyway — so its result is deliberately discarded.
     */
    override suspend fun skipOccurrence(
        seriesId: String,
        occurrenceDate: LocalDate,
    ): EmptyResult<DataError.Remote> =
        service
            .skipOccurrence(seriesId, occurrenceDate)
            .onSuccess {
                database.recurringSeriesDao.upsertExceptions(
                    listOf(RecurringExceptionEntity(seriesId, occurrenceDate.toString())),
                )
                refreshSeriesOfSameGroup(seriesId)
            }.asEmptyResult()

    override suspend fun unskipOccurrence(
        seriesId: String,
        occurrenceDate: LocalDate,
    ): EmptyResult<DataError.Remote> =
        service
            .unskipOccurrence(seriesId, occurrenceDate)
            .onSuccess {
                database.recurringSeriesDao.deleteException(seriesId, occurrenceDate.toString())
                refreshSeriesOfSameGroup(seriesId)
            }.asEmptyResult()

    override suspend fun endSeries(seriesId: String): EmptyResult<DataError.Remote> =
        service
            .endSeries(seriesId)
            .onSuccess {
                database.recurringSeriesDao.markEnded(seriesId, Clock.System.now().toEpochMilliseconds())
                refreshSeriesOfSameGroup(seriesId)
            }.asEmptyResult()

    /**
     * Replaces the group's schedules with what the server currently has.
     *
     * Needed on top of the account-wide sync, which only carries series changed since the cursor: a
     * group that just became visible arrives without the schedules it already had. Prunes anything
     * local the payload does not mention, because unlike a delta this response is complete.
     */
    override suspend fun refreshSeriesForGroup(groupId: String): EmptyResult<DataError.Remote> =
        service
            .getSeriesForGroup(groupId)
            .onSuccess { series ->
                val serverIds = series.mapTo(mutableSetOf()) { it.id }
                val stale =
                    database.recurringSeriesDao
                        .getSeriesIdsForGroup(groupId)
                        .filterNot { it in serverIds }
                localWriter.persist(series, staleSeriesIds = stale)
            }.asEmptyResult()

    private suspend fun refreshSeriesOfSameGroup(seriesId: String) {
        val groupId = database.recurringSeriesDao.getGroupIdForSeries(seriesId) ?: return
        refreshSeriesForGroup(groupId)
    }
}
