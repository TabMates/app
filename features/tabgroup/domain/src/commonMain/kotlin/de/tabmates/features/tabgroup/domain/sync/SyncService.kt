package de.tabmates.features.tabgroup.domain.sync

import de.tabmates.core.domain.util.DataError
import de.tabmates.core.domain.util.Result
import de.tabmates.features.tabgroup.domain.models.SyncSnapshot
import kotlin.time.Instant

/**
 * Remote contract for the account-wide delta-sync endpoint. A single call fetches groups and tab
 * entries: pass `null` for a full snapshot, or the previous [SyncSnapshot.serverTime] cursor for a
 * delta.
 */
interface SyncService {
    suspend fun sync(since: Instant?): Result<SyncSnapshot, DataError.Remote>
}
