package de.tabmates.features.tabgroup.data.sync

import de.tabmates.core.data.networking.get
import de.tabmates.core.domain.util.DataError
import de.tabmates.core.domain.util.Result
import de.tabmates.core.domain.util.map
import de.tabmates.features.tabgroup.data.dto.SyncResponseDto
import de.tabmates.features.tabgroup.data.mappers.toDomain
import de.tabmates.features.tabgroup.domain.models.SyncSnapshot
import de.tabmates.features.tabgroup.domain.sync.SyncService
import io.ktor.client.HttpClient
import org.koin.core.annotation.Single
import kotlin.time.Instant

@Single(binds = [SyncService::class])
class KtorSyncService(
    private val httpClient: HttpClient,
) : SyncService {
    override suspend fun sync(since: Instant?): Result<SyncSnapshot, DataError.Remote> {
        val queryParams =
            buildMap<String, Any> {
                since?.let { put("since", it.toString()) }
            }
        return httpClient
            .get<SyncResponseDto>(
                route = "/api/sync",
                queryParams = queryParams,
            ).map { it.toDomain() }
    }
}
