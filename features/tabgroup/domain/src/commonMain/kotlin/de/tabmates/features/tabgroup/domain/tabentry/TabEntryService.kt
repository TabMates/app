package de.tabmates.features.tabgroup.domain.tabentry

import de.tabmates.core.domain.util.DataError
import de.tabmates.core.domain.util.EmptyResult
import de.tabmates.core.domain.util.Result
import de.tabmates.features.tabgroup.domain.models.TabEntry
import kotlin.time.Instant

/**
 * Service contract for TabEntry remote operations.
 * Create / update happen over WebSocket via the outbox; this interface only covers HTTP-only ops.
 */
interface TabEntryService {
    suspend fun getTabEntriesForGroup(
        groupId: String,
        before: Instant?,
        pageSize: Int,
    ): Result<List<TabEntry>, DataError.Remote>

    suspend fun deleteTabEntry(tabEntryId: String): EmptyResult<DataError.Remote>
}
