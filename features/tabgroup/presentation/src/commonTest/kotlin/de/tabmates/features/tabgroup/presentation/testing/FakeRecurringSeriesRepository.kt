package de.tabmates.features.tabgroup.presentation.testing

import de.tabmates.core.domain.util.DataError
import de.tabmates.core.domain.util.EmptyResult
import de.tabmates.core.domain.util.Result
import de.tabmates.features.tabgroup.domain.recurring.RecurringSeries
import de.tabmates.features.tabgroup.domain.recurring.RecurringSeriesRepository
import de.tabmates.features.tabgroup.domain.recurring.RecurringSlot
import de.tabmates.features.tabgroup.domain.recurring.RecurringTemplate
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.datetime.LocalDate

class FakeRecurringSeriesRepository : RecurringSeriesRepository {
    private val series = MutableStateFlow<List<RecurringSeries>>(emptyList())
    private val claimedSlots = MutableStateFlow<Set<RecurringSlot>>(emptySet())

    /** Schedule writes the screen asked for, in order, for tests that assert on intent. */
    val recordedWrites = mutableListOf<String>()

    var writeResult: Result<RecurringSeries, DataError.Remote>? = null
    var writeError: DataError.Remote? = null

    fun setSeries(vararg values: RecurringSeries) {
        series.value = values.toList()
    }

    fun setClaimedSlots(vararg values: RecurringSlot) {
        claimedSlots.value = values.toSet()
    }

    override fun getSeriesForGroup(groupId: String): Flow<List<RecurringSeries>> =
        series.map { all -> all.filter { it.groupId == groupId } }

    override fun getSeriesById(seriesId: String): Flow<RecurringSeries?> =
        series.map { all -> all.firstOrNull { it.seriesId == seriesId } }

    // Scoped to the group like the real one is: an unscoped fake would leak one group's claims into
    // another's projection, and a test with two groups would be quietly wrong rather than red.
    override fun getClaimedSlotsForGroup(groupId: String): Flow<Set<RecurringSlot>> =
        combine(series, claimedSlots) { all, slots ->
            val idsInGroup = all.filter { it.groupId == groupId }.mapTo(mutableSetOf()) { it.seriesId }
            slots.filterTo(mutableSetOf()) { it.seriesId in idsInGroup }
        }

    override suspend fun createSeries(
        seriesId: String,
        groupId: String,
        template: RecurringTemplate,
    ): Result<RecurringSeries, DataError.Remote> {
        recordedWrites += "create:$seriesId"
        writeError?.let { return Result.Failure(it) }
        return writeResult ?: Result.Failure(DataError.Remote.UNKNOWN)
    }

    override suspend fun updateSeries(
        seriesId: String,
        effectiveFrom: LocalDate,
        template: RecurringTemplate,
    ): Result<RecurringSeries, DataError.Remote> {
        recordedWrites += "update:$seriesId@$effectiveFrom"
        writeError?.let { return Result.Failure(it) }
        return writeResult ?: Result.Failure(DataError.Remote.UNKNOWN)
    }

    override suspend fun skipOccurrence(
        seriesId: String,
        occurrenceDate: LocalDate,
    ): EmptyResult<DataError.Remote> {
        recordedWrites += "skip:$seriesId@$occurrenceDate"
        return writeError?.let { Result.Failure(it) } ?: Result.Success(Unit)
    }

    override suspend fun unskipOccurrence(
        seriesId: String,
        occurrenceDate: LocalDate,
    ): EmptyResult<DataError.Remote> {
        recordedWrites += "unskip:$seriesId@$occurrenceDate"
        return writeError?.let { Result.Failure(it) } ?: Result.Success(Unit)
    }

    override suspend fun endSeries(seriesId: String): EmptyResult<DataError.Remote> {
        recordedWrites += "end:$seriesId"
        return writeError?.let { Result.Failure(it) } ?: Result.Success(Unit)
    }

    override suspend fun refreshSeriesForGroup(groupId: String): EmptyResult<DataError.Remote> =
        Result.Success(Unit)
}
