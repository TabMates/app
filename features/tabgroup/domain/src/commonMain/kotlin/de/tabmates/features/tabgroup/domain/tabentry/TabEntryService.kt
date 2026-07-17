package de.tabmates.features.tabgroup.domain.tabentry

import de.tabmates.core.domain.util.DataError
import de.tabmates.core.domain.util.EmptyResult
import de.tabmates.core.domain.util.Result
import de.tabmates.features.tabgroup.domain.models.GroupTabEntryHistory
import de.tabmates.features.tabgroup.domain.models.TabEntry

/**
 * Service contract for TabEntry remote operations.
 * Create / update happen over WebSocket via the outbox; regular reads happen via `/api/sync`, so
 * HTTP only covers the delete plus the per-group history fetch below.
 */
interface TabEntryService {
    suspend fun deleteTabEntry(tabEntryId: String): EmptyResult<DataError.Remote>

    /**
     * Fetches the complete tab-entry history for [groupId], including soft-deleted entries
     * (non-null [TabEntry.deletedAt]) and every participant the entries reference — some of whom
     * may no longer be group members — so the caller can persist them before the entries/splits
     * to satisfy the split table's participant FK. `/api/sync` filters entries by
     * `lastModifiedAt >= since`, so entries predating the cursor never appear in a delta — this
     * call backfills them when a group becomes known locally outside the normal sync path
     * (joining a group).
     */
    suspend fun getTabEntriesForGroup(groupId: String): Result<GroupTabEntryHistory, DataError.Remote>
}
