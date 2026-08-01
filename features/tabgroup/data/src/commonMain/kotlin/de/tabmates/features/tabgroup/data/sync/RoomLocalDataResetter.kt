package de.tabmates.features.tabgroup.data.sync

import de.tabmates.core.domain.sync.ActivityCursorStore
import de.tabmates.core.domain.sync.LastServerContactStore
import de.tabmates.core.domain.sync.LocalDataResetter
import de.tabmates.core.domain.sync.PendingTabEntryBackfillStore
import de.tabmates.core.domain.sync.SyncCursorStore
import de.tabmates.features.tabgroup.database.TabMatesDatabase
import org.koin.core.annotation.Single

@Single(binds = [LocalDataResetter::class])
class RoomLocalDataResetter(
    private val database: TabMatesDatabase,
    private val syncCursorStore: SyncCursorStore,
    private val activityCursorStore: ActivityCursorStore,
    private val lastServerContactStore: LastServerContactStore,
    private val pendingBackfillStore: PendingTabEntryBackfillStore,
) : LocalDataResetter {
    override suspend fun resetLocalData() {
        syncCursorStore.clear()
        activityCursorStore.clear()
        lastServerContactStore.clear()
        pendingBackfillStore.clearAll()
        // Deleting the groups FK-cascades tab entries, splits, participant cross-refs and
        // activity events. The outbox has no FK to anything (by design — queued writes must
        // survive an app kill), so it has to be cleared explicitly; otherwise the previous
        // account's writes would replay under whoever signs in next.
        database.groupDao.deleteAllGroups()
        database.pendingOutboxDao.deleteAll()
    }

    override suspend fun resetReferenceData() {
        database.currencyDao.deleteAllCurrencies()
        database.exchangeRateDao.deleteAllExchangeRates()
    }
}
