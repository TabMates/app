package de.tabmates.features.tabgroup.data.sync

import de.tabmates.core.domain.sync.LastServerContactStore
import de.tabmates.core.domain.sync.PendingTabEntryBackfillStore
import de.tabmates.core.domain.sync.SyncCursorStore
import de.tabmates.core.domain.util.DataError
import de.tabmates.core.domain.util.EmptyResult
import de.tabmates.core.domain.util.asEmptyResult
import de.tabmates.core.domain.util.onSuccess
import de.tabmates.features.tabgroup.data.mappers.toEntity
import de.tabmates.features.tabgroup.data.mappers.toSplitEntities
import de.tabmates.features.tabgroup.database.TabMatesDatabase
import de.tabmates.features.tabgroup.database.entities.GroupParticipantEntity
import de.tabmates.features.tabgroup.database.entities.GroupWithParticipants
import de.tabmates.features.tabgroup.database.entities.types.ParticipantTypeDatabase
import de.tabmates.features.tabgroup.domain.models.SyncSnapshot
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
    private val lastServerContactStore: LastServerContactStore,
    private val tabEntryBackfiller: GroupTabEntryBackfiller,
    private val pendingBackfillStore: PendingTabEntryBackfillStore,
) : SyncRepository {
    // Serializes the login and reconnect triggers so their sync runs can't interleave and race
    // on the shared cursor / local DB.
    private val mutex = Mutex()

    override suspend fun sync(): EmptyResult<DataError.Remote> =
        mutex.withLock {
            val since = cursorStore.get()
            val isFullSync = since == null
            // Captured before the merge: applySnapshot upserts newly joined groups, after which
            // they're indistinguishable from long-known ones.
            val knownGroupIds =
                if (isFullSync) emptySet() else database.groupDao.getAllGroupIds().toSet()

            syncService
                .sync(since)
                .onSuccess { snapshot ->
                    applySnapshot(snapshot, isFullSync = isFullSync)
                    // Advance the cursor only after the merges succeed; a mid-merge failure leaves
                    // it unchanged so the next sync re-fetches (server queries are `>= since`,
                    // last-write-wins, so replaying is idempotent).
                    cursorStore.set(snapshot.serverTime)
                    lastServerContactStore.recordContactNow()

                    if (isFullSync) {
                        // The full snapshot carried every group's complete entry history, so any
                        // leftover retry markers are moot.
                        pendingBackfillStore.clearAll()
                    } else {
                        backfillNewAndPendingGroups(snapshot, knownGroupIds)
                    }
                }.asEmptyResult()
        }

    /**
     * The delta's `lastModifiedAt >= since` filter never returns entries that predate the cursor,
     * so a group that just became visible (joined on another device, or a join whose backfill
     * fetch failed) arrives without its history — fetch it per group. Backfill failures don't
     * fail the sync and the cursor stays advanced: retry is owned by the pending markers, so a
     * misbehaving per-group fetch can't stall delta sync for everything else.
     */
    private suspend fun backfillNewAndPendingGroups(
        snapshot: SyncSnapshot,
        knownGroupIds: Set<String>,
    ) {
        val activeGroupIds = snapshot.activeGroupIds.toSet()
        // Drop markers for groups the user has left so the set can't grow unbounded.
        pendingBackfillStore.retainAll(activeGroupIds)

        val newlyKnownGroupIds =
            snapshot.groups
                .map { it.id }
                .filter { it in activeGroupIds && it !in knownGroupIds }

        (newlyKnownGroupIds + pendingBackfillStore.getAll())
            .toSet()
            .forEach { groupId -> tabEntryBackfiller.backfill(groupId) }
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

        // Entries/splits may reference users who are no longer members of any group (left,
        // removed, or deleted account) and therefore aren't in any group's participant list
        // above. Persist them first so the split table's participant FK holds.
        if (snapshot.referencedParticipants.isNotEmpty()) {
            database.groupParticipantDao.upsertParticipants(
                snapshot.referencedParticipants.map { it.toEntity() },
            )
        }

        val (deleted, alive) = snapshot.tabEntries.partition { it.isDeleted }
        val splitsByEntryId = alive.associate { it.tabEntryId to it.toSplitEntities() }
        val allServerIds =
            if (isFullSync) snapshot.tabEntries.map { it.tabEntryId }.toSet() else null

        // Last-resort FK guard: if a split's participant object was missing from the payload,
        // synthesize a placeholder row (insert-ignore, never overwrites real data) so applying
        // the entries can't blow up the whole sync on the participant FK.
        val knownParticipantIds =
            buildSet {
                snapshot.groups.forEach { group -> group.participants.mapTo(this) { it.userId } }
                snapshot.referencedParticipants.mapTo(this) { it.userId }
            }
        val orphanSplitParticipants =
            splitsByEntryId.values
                .asSequence()
                .flatten()
                .map { it.participantId }
                .distinct()
                .filterNot { it in knownParticipantIds }
                .map {
                    GroupParticipantEntity(
                        userId = it,
                        username = "Unknown",
                        participantType = ParticipantTypeDatabase.PLACEHOLDER,
                    )
                }.toList()
        if (orphanSplitParticipants.isNotEmpty()) {
            database.groupParticipantDao.insertParticipantsIgnoringConflicts(orphanSplitParticipants)
        }

        database.tabEntryDao.applySyncedTabEntries(
            aliveEntries = alive.map { it.toEntity() },
            splitsByEntryId = splitsByEntryId,
            deletedIds = deleted.map { it.tabEntryId },
            allServerIds = allServerIds,
            splitDao = database.tabEntrySplitDao,
        )
    }
}
