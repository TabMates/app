package de.tabmates.features.tabgroup.data.recurring

import de.tabmates.core.domain.util.DataError
import de.tabmates.core.domain.util.EmptyResult
import de.tabmates.core.domain.util.Result
import de.tabmates.features.tabgroup.data.dto.RecurringSeriesDto
import de.tabmates.features.tabgroup.data.dto.RecurringTemplateDto
import kotlinx.datetime.LocalDate

/**
 * The remote contract for recurring schedules.
 *
 * Lives in the data layer rather than the domain because it trades in DTOs: the repository owns the
 * mapping, and every response has to reach the local mirror in the same shape the sync path writes.
 */
interface RecurringSeriesService {
    suspend fun getSeriesForGroup(groupId: String): Result<List<RecurringSeriesDto>, DataError.Remote>

    suspend fun createSeries(
        seriesId: String,
        groupId: String,
        template: RecurringTemplateDto,
    ): Result<RecurringSeriesDto, DataError.Remote>

    suspend fun updateSeries(
        seriesId: String,
        effectiveFrom: LocalDate,
        template: RecurringTemplateDto,
    ): Result<RecurringSeriesDto, DataError.Remote>

    suspend fun skipOccurrence(
        seriesId: String,
        occurrenceDate: LocalDate,
    ): EmptyResult<DataError.Remote>

    suspend fun unskipOccurrence(
        seriesId: String,
        occurrenceDate: LocalDate,
    ): EmptyResult<DataError.Remote>

    suspend fun endSeries(seriesId: String): EmptyResult<DataError.Remote>
}
