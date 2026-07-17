package de.tabmates.features.tabgroup.data.sync

import de.tabmates.core.domain.logging.TabMatesLogger
import de.tabmates.core.domain.sync.PendingTabEntryBackfillStore
import de.tabmates.core.domain.util.onFailure
import de.tabmates.core.domain.util.onSuccess
import de.tabmates.features.tabgroup.data.mappers.toEntity
import de.tabmates.features.tabgroup.data.mappers.toSplitEntities
import de.tabmates.features.tabgroup.database.TabMatesDatabase
import de.tabmates.features.tabgroup.domain.models.GroupTabEntryHistory
import de.tabmates.features.tabgroup.domain.tabentry.TabEntryService
import kotlinx.coroutines.CancellationException
import org.koin.core.annotation.Single

/**
 * Fetches a group's complete tab-entry history and merges it into the local DB. Needed whenever a
 * group becomes known locally outside the full-sync path (joining, or a delta sync revealing a
 * group joined on another device): the delta's `lastModifiedAt >= since` filter never surfaces
 * entries that predate the cursor.
 */
@Single
class GroupTabEntryBackfiller(
    private val tabEntryService: TabEntryService,
    private val database: TabMatesDatabase,
    private val pendingBackfillStore: PendingTabEntryBackfillStore,
    private val logger: TabMatesLogger,
) {
    /**
     * Never throws and never fails the caller: on any failure the group id stays marked in
     * [PendingTabEntryBackfillStore] so the next delta sync retries. The group row must already
     * exist locally — entry rows carry an enforced FK to it.
     */
    suspend fun backfill(groupId: String) {
        // Marked before the fetch, not on failure: if the process dies mid-fetch the marker is
        // already durable and the next sync retries, instead of the group's history silently
        // staying incomplete (the group is no longer "unknown locally" by then).
        pendingBackfillStore.add(groupId)
        try {
            tabEntryService
                .getTabEntriesForGroup(groupId)
                .onSuccess { history ->
                    apply(history)
                    pendingBackfillStore.remove(groupId)
                }.onFailure { error ->
                    logger.warning(TAG, "Tab-entry backfill failed for group=$groupId: $error")
                }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            logger.error(TAG, "Tab-entry backfill failed for group=$groupId", e)
        }
    }

    private suspend fun apply(history: GroupTabEntryHistory) {
        // A group's full history can include entries from members who have since left — persist
        // them first so the split table's participant FK holds.
        if (history.referencedParticipants.isNotEmpty()) {
            database.groupParticipantDao.upsertParticipants(
                history.referencedParticipants.map { it.toEntity() },
            )
        }

        val (deleted, alive) = history.entries.partition { it.isDeleted }
        database.tabEntryDao.applySyncedTabEntries(
            aliveEntries = alive.map { it.toEntity() },
            splitsByEntryId = alive.associate { it.tabEntryId to it.toSplitEntities() },
            deletedIds = deleted.map { it.tabEntryId },
            // No pruning: rows of other groups must survive, and the target group can't have
            // stale local rows — its entries cascade-delete with the group row.
            allServerIds = null,
            splitDao = database.tabEntrySplitDao,
        )
    }

    private companion object {
        private const val TAG = "GroupTabEntryBackfiller"
    }
}
