package de.tabmates.features.tabgroup.data.tabentry

import de.tabmates.core.data.networking.get
import de.tabmates.core.domain.util.DataError
import de.tabmates.core.domain.util.Result
import de.tabmates.core.domain.util.map
import de.tabmates.features.tabgroup.data.dto.TabEntryDto
import de.tabmates.features.tabgroup.data.mappers.toDomain
import de.tabmates.features.tabgroup.domain.models.TabEntry
import de.tabmates.features.tabgroup.domain.tabentry.TabEntryService
import io.ktor.client.HttpClient
import org.koin.core.annotation.Single
import kotlin.time.Instant

@Single(binds = [TabEntryService::class])
class KtorTabEntryService(
    private val httpClient: HttpClient,
) : TabEntryService {
    override suspend fun getTabEntriesForGroup(
        groupId: String,
        before: Instant?,
        pageSize: Int,
    ): Result<List<TabEntry>, DataError.Remote> {
        val queryParams =
            buildMap<String, Any> {
                before?.let { put("before", it.toString()) }
                put("pageSize", pageSize)
            }
        return httpClient
            .get<List<TabEntryDto>>(
                route = "/api/group/$groupId/tab-entries",
                queryParams = queryParams,
            ).map { dtos -> dtos.map { it.toDomain() } }
    }
}
