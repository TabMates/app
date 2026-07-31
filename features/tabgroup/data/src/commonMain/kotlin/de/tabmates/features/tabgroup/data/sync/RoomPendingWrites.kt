package de.tabmates.features.tabgroup.data.sync

import de.tabmates.core.domain.sync.PendingWrites
import de.tabmates.features.tabgroup.database.TabMatesDatabase
import kotlinx.coroutines.flow.Flow
import org.koin.core.annotation.Single

@Single(binds = [PendingWrites::class])
class RoomPendingWrites(
    private val database: TabMatesDatabase,
) : PendingWrites {
    override fun observeCount(): Flow<Int> = database.pendingOutboxDao.observeCount()
}
