package de.tabmates.features.tabgroup.data.tabentry

import de.tabmates.core.data.networking.delete
import de.tabmates.core.data.networking.get
import de.tabmates.core.domain.util.DataError
import de.tabmates.core.domain.util.EmptyResult
import de.tabmates.core.domain.util.Result
import de.tabmates.core.domain.util.asEmptyResult
import de.tabmates.core.domain.util.map
import de.tabmates.features.tabgroup.data.dto.TabEntryDto
import de.tabmates.features.tabgroup.data.mappers.referencedParticipants
import de.tabmates.features.tabgroup.data.mappers.toDomain
import de.tabmates.features.tabgroup.domain.models.GroupTabEntryHistory
import de.tabmates.features.tabgroup.domain.tabentry.TabEntryService
import io.ktor.client.HttpClient
import org.koin.core.annotation.Single

@Single(binds = [TabEntryService::class])
class KtorTabEntryService(
    private val httpClient: HttpClient,
) : TabEntryService {
    override suspend fun deleteTabEntry(tabEntryId: String): EmptyResult<DataError.Remote> =
        httpClient
            .delete<Unit>(route = "/api/tab-entry/$tabEntryId")
            .asEmptyResult()

    override suspend fun getTabEntriesForGroup(groupId: String): Result<GroupTabEntryHistory, DataError.Remote> =
        httpClient
            .get<List<TabEntryDto>>(route = "/api/group/$groupId/tab-entries")
            .map { dtos ->
                GroupTabEntryHistory(
                    entries = dtos.map { it.toDomain() },
                    referencedParticipants =
                        dtos
                            .flatMap { it.referencedParticipants() }
                            .distinctBy { it.userId }
                            .map { it.toDomain() },
                )
            }
}
