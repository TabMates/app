package de.tabmates.features.tabgroup.domain.tabentry

import de.tabmates.core.domain.util.DataError
import de.tabmates.core.domain.util.EmptyResult

/**
 * Service contract for TabEntry remote operations.
 * Create / update happen over WebSocket via the outbox; reads happen via `/api/sync`, so this
 * interface only covers the HTTP-only delete.
 */
interface TabEntryService {
    suspend fun deleteTabEntry(tabEntryId: String): EmptyResult<DataError.Remote>
}
