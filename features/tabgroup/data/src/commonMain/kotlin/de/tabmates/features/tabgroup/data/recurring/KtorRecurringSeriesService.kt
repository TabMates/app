package de.tabmates.features.tabgroup.data.recurring

import de.tabmates.core.data.networking.delete
import de.tabmates.core.data.networking.get
import de.tabmates.core.data.networking.patch
import de.tabmates.core.data.networking.post
import de.tabmates.core.domain.util.DataError
import de.tabmates.core.domain.util.EmptyResult
import de.tabmates.core.domain.util.Result
import de.tabmates.core.domain.util.asEmptyResult
import de.tabmates.features.tabgroup.data.dto.CreateRecurringSeriesRequestDto
import de.tabmates.features.tabgroup.data.dto.RecurringSeriesDto
import de.tabmates.features.tabgroup.data.dto.RecurringSeriesErrorDto
import de.tabmates.features.tabgroup.data.dto.RecurringTemplateDto
import de.tabmates.features.tabgroup.data.dto.SkipRecurringOccurrenceRequestDto
import de.tabmates.features.tabgroup.data.dto.UpdateRecurringSeriesRequestDto
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.statement.HttpResponse
import kotlinx.coroutines.CancellationException
import kotlinx.datetime.LocalDate
import org.koin.core.annotation.Single

/**
 * The schedule endpoints.
 *
 * Deliberately plain REST with no outbox behind it, mirroring the server's own split: entries are
 * high-volume writes that need the websocket ack contract, whereas a schedule is a rare, deliberate
 * act. Queueing one offline would leave a standing instruction to write into other people's ledgers
 * pending on a device nobody is watching.
 */
@Single(binds = [RecurringSeriesService::class])
class KtorRecurringSeriesService(
    private val httpClient: HttpClient,
) : RecurringSeriesService {
    override suspend fun getSeriesForGroup(groupId: String): Result<List<RecurringSeriesDto>, DataError.Remote> =
        httpClient.get<List<RecurringSeriesDto>>(route = "/api/group/$groupId/recurring-series")

    override suspend fun createSeries(
        seriesId: String,
        groupId: String,
        template: RecurringTemplateDto,
    ): Result<RecurringSeriesDto, DataError.Remote> =
        httpClient.post(
            route = "/api/recurring-series",
            body =
                CreateRecurringSeriesRequestDto(
                    groupId = groupId,
                    id = seriesId,
                    template = template,
                ),
            mapKnownError = { it.recurringErrorOrNull() },
        )

    override suspend fun updateSeries(
        seriesId: String,
        effectiveFrom: LocalDate,
        template: RecurringTemplateDto,
    ): Result<RecurringSeriesDto, DataError.Remote> =
        httpClient.patch(
            route = "/api/recurring-series/$seriesId",
            body =
                UpdateRecurringSeriesRequestDto(
                    effectiveFrom = effectiveFrom,
                    template = template,
                ),
            mapKnownError = { it.recurringErrorOrNull() },
        )

    override suspend fun skipOccurrence(
        seriesId: String,
        occurrenceDate: LocalDate,
    ): EmptyResult<DataError.Remote> =
        httpClient
            .post<SkipRecurringOccurrenceRequestDto, Unit>(
                route = "/api/recurring-series/$seriesId/exceptions",
                body = SkipRecurringOccurrenceRequestDto(occurrenceDate),
                mapKnownError = { it.recurringErrorOrNull() },
            ).asEmptyResult()

    override suspend fun unskipOccurrence(
        seriesId: String,
        occurrenceDate: LocalDate,
    ): EmptyResult<DataError.Remote> =
        httpClient
            .delete<Unit>(
                route = "/api/recurring-series/$seriesId/exceptions/$occurrenceDate",
                mapKnownError = { it.recurringErrorOrNull() },
            ).asEmptyResult()

    override suspend fun endSeries(seriesId: String): EmptyResult<DataError.Remote> =
        httpClient
            .delete<Unit>(
                route = "/api/recurring-series/$seriesId",
                mapKnownError = { it.recurringErrorOrNull() },
            ).asEmptyResult()
}

/**
 * Maps the two refusals the schedule endpoints state by code.
 *
 * The `503` is the one worth separating: it does not mean the server is struggling, it means the
 * feature is switched off in this environment, so the same request can succeed later untouched.
 * Anything else returns null and falls through to the generic status handling. The catch tolerates
 * non-JSON bodies but lets cancellation through, which has to reach the caller rather than be
 * answered with a plain failure.
 */
private suspend fun HttpResponse.recurringErrorOrNull(): DataError.Remote? {
    if (status.value != 400 && status.value != 503) return null
    val code =
        try {
            body<RecurringSeriesErrorDto>().code
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            null
        }
    return when (code) {
        "INVALID_RECURRING_RULE" -> DataError.Remote.INVALID_RECURRING_RULE
        "RECURRING_ENTRIES_DISABLED" -> DataError.Remote.RECURRING_ENTRIES_DISABLED
        else -> null
    }
}
