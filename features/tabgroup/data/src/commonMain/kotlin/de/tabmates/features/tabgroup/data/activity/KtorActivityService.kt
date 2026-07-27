package de.tabmates.features.tabgroup.data.activity

import de.tabmates.core.data.networking.get
import de.tabmates.core.domain.util.DataError
import de.tabmates.core.domain.util.Result
import de.tabmates.core.domain.util.map
import de.tabmates.features.tabgroup.data.dto.ActivityFeedResponseDto
import de.tabmates.features.tabgroup.data.mappers.toDomain
import de.tabmates.features.tabgroup.domain.activity.ActivityFeedPage
import de.tabmates.features.tabgroup.domain.activity.ActivityService
import io.ktor.client.HttpClient
import org.koin.core.annotation.Single

@Single(binds = [ActivityService::class])
class KtorActivityService(
    private val httpClient: HttpClient,
) : ActivityService {
    override suspend fun getActivityFeed(
        since: Long?,
        limit: Int,
    ): Result<ActivityFeedPage, DataError.Remote> {
        val queryParams =
            buildMap<String, Any> {
                since?.let { put("since", it) }
                put("limit", limit)
            }
        return httpClient
            .get<ActivityFeedResponseDto>(
                route = "/api/activity",
                queryParams = queryParams,
            ).map { response ->
                ActivityFeedPage(
                    events = response.events.map { it.toDomain() },
                    nextCursor = response.nextCursor,
                    hasMore = response.hasMore,
                )
            }
    }
}
