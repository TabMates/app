package de.tabmates.features.tabgroup.data.tabentry

import de.tabmates.core.data.networking.delete
import de.tabmates.core.domain.util.DataError
import de.tabmates.core.domain.util.EmptyResult
import de.tabmates.core.domain.util.asEmptyResult
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
}
