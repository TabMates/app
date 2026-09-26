package de.tabmates.features.tabgroup.data.sync

import de.tabmates.core.domain.util.DataError
import de.tabmates.core.domain.util.EmptyResult
import de.tabmates.core.domain.util.Result
import de.tabmates.features.tabgroup.domain.recurring.RecurringSeries
import de.tabmates.features.tabgroup.domain.recurring.RecurringSeriesRepository
import de.tabmates.features.tabgroup.domain.recurring.RecurringSlot
import de.tabmates.features.tabgroup.domain.recurring.RecurringTemplate
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.datetime.LocalDate

/**
 * Records the per-group refreshes the sync path asks for, and does nothing else.
 *
 * Every write method fails loudly: the sync path has no business creating or editing schedules, so
 * a call reaching one of them is a wiring mistake worth a red test rather than a silent success.
 */
class FakeRecurringSeriesRepository : RecurringSeriesRepository {
    val refreshedGroupIds = mutableListOf<String>()

    override fun getSeriesForGroup(groupId: String): Flow<List<RecurringSeries>> = flowOf(emptyList())

    override fun getSeriesById(seriesId: String): Flow<RecurringSeries?> = flowOf(null)

    override fun getClaimedSlotsForGroup(groupId: String): Flow<Set<RecurringSlot>> = flowOf(emptySet())

    override suspend fun createSeries(
        seriesId: String,
        groupId: String,
        template: RecurringTemplate,
    ): Result<RecurringSeries, DataError.Remote> = error("unexpected createSeries in a sync test")

    override suspend fun updateSeries(
        seriesId: String,
        effectiveFrom: LocalDate,
        template: RecurringTemplate,
    ): Result<RecurringSeries, DataError.Remote> = error("unexpected updateSeries in a sync test")

    override suspend fun skipOccurrence(
        seriesId: String,
        occurrenceDate: LocalDate,
    ): EmptyResult<DataError.Remote> = error("unexpected skipOccurrence in a sync test")

    override suspend fun unskipOccurrence(
        seriesId: String,
        occurrenceDate: LocalDate,
    ): EmptyResult<DataError.Remote> = error("unexpected unskipOccurrence in a sync test")

    override suspend fun endSeries(seriesId: String): EmptyResult<DataError.Remote> =
        error("unexpected endSeries in a sync test")

    override suspend fun refreshSeriesForGroup(groupId: String): EmptyResult<DataError.Remote> {
        refreshedGroupIds += groupId
        return Result.Success(Unit)
    }
}
