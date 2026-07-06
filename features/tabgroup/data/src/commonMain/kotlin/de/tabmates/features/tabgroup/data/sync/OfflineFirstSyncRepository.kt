package de.tabmates.features.tabgroup.data.sync

import de.tabmates.core.domain.sync.SyncCursorStore
import de.tabmates.core.domain.util.DataError
import de.tabmates.core.domain.util.EmptyResult
import de.tabmates.core.domain.util.asEmptyResult
import de.tabmates.core.domain.util.onSuccess
import de.tabmates.features.tabgroup.data.mappers.toEntity
import de.tabmates.features.tabgroup.database.TabMatesDatabase
import de.tabmates.features.tabgroup.database.entities.GroupWithParticipants
import de.tabmates.features.tabgroup.database.entities.TabEntrySplitEntity
import de.tabmates.features.tabgroup.domain.models.SyncSnapshot
import de.tabmates.features.tabgroup.domain.models.TabEntry
import de.tabmates.features.tabgroup.domain.sync.SyncRepository
import de.tabmates.features.tabgroup.domain.sync.SyncService
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import org.koin.core.annotation.Single

@Single(binds = [SyncRepository::class])
class OfflineFirstSyncRepository(
    private val syncService: SyncService,
    private val database: TabMatesDatabase,
    private val cursorStore: SyncCursorStore,
) : SyncRepository {
    // Serializes the login and reconnect triggers so their sync runs can't interleave and race
    // on the shared cursor / local DB.
    private val mutex = Mutex()

    override suspend fun sync(): EmptyResult<DataError.Remote> =
        mutex.withLock {
            val since = cursorStore.get()
            syncService
                .sync(since)
                .onSuccess { snapshot ->
                    applySnapshot(snapshot, isFullSync = since == null)
                    // Advance the cursor only after the merges succeed; a mid-merge failure leaves
                    // it unchanged so the next sync re-fetches (server queries are `>= since`,
                    // last-write-wins, so replaying is idempotent).
                    cursorStore.set(snapshot.serverTime)
                }.asEmptyResult()
        }

    private suspend fun applySnapshot(
        snapshot: SyncSnapshot,
        isFullSync: Boolean,
    ) {
        val groupsWithParticipants =
            snapshot.groups.map { group ->
                GroupWithParticipants(
                    group = group.toEntity(),
                    participants = group.participants.map { it.toEntity() },
                    lastTabEntry = null,
                )
            }
        database.groupDao.syncChangedGroups(
            groups = groupsWithParticipants,
            activeGroupIds = snapshot.activeGroupIds,
            participantDao = database.groupParticipantDao,
            crossRefDao = database.groupParticipantCrossRefDao,
        )

        val (deleted, alive) = snapshot.tabEntries.partition { it.isDeleted }
        val aliveEntities = alive.map { it.toEntity() }
        val splitsByEntryId =
            alive.associate { entry ->
                entry.tabEntryId to
                    when (entry) {
                        is TabEntry.Expense -> entry.splits.map { it.toEntity() }
                        is TabEntry.Income -> entry.splits.map { it.toEntity() }
                        is TabEntry.Settlement -> emptyList<TabEntrySplitEntity>()
                    }
            }
        val allServerIds =
            if (isFullSync) snapshot.tabEntries.map { it.tabEntryId }.toSet() else null

        database.tabEntryDao.applySyncedTabEntries(
            aliveEntries = aliveEntities,
            splitsByEntryId = splitsByEntryId,
            deletedIds = deleted.map { it.tabEntryId },
            allServerIds = allServerIds,
            splitDao = database.tabEntrySplitDao,
        )
    }
}
