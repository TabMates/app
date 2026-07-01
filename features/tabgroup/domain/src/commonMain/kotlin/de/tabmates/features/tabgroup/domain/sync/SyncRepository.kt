package de.tabmates.features.tabgroup.domain.sync

import de.tabmates.core.domain.util.DataError
import de.tabmates.core.domain.util.EmptyResult

/**
 * Pulls groups and tab entries from the server via `/api/sync` and reconciles them into the local
 * database. Uses a persisted cursor to fetch a full snapshot on first run and deltas thereafter.
 */
interface SyncRepository {
    suspend fun sync(): EmptyResult<DataError.Remote>
}
