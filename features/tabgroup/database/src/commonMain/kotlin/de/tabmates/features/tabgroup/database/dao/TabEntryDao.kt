package de.tabmates.features.tabgroup.database.dao

import androidx.room3.Dao
import androidx.room3.Query
import androidx.room3.Transaction
import androidx.room3.Upsert
import de.tabmates.features.tabgroup.database.entities.TabEntryEntity
import de.tabmates.features.tabgroup.database.entities.TabEntrySplitEntity
import de.tabmates.features.tabgroup.database.entities.TabEntryWithSplits
import kotlinx.coroutines.flow.Flow

@Dao
interface TabEntryDao {
    @Upsert
    suspend fun upsertTabEntry(tabEntry: TabEntryEntity)

    @Upsert
    suspend fun upsertTabEntries(tabEntries: List<TabEntryEntity>)

    @Query("DELETE FROM tabentryentity WHERE tabEntryId = :tabEntryId")
    suspend fun deleteTabEntryById(tabEntryId: String)

    @Query("DELETE FROM tabentryentity WHERE tabEntryId IN (:tabEntryIds)")
    suspend fun deleteTabEntriesById(tabEntryIds: List<String>)

    @Transaction
    @Query("SELECT * FROM tabentryentity WHERE groupId = :groupId ORDER BY entryDate DESC, createdAt DESC")
    fun getTabEntriesByGroupId(groupId: String): Flow<List<TabEntryWithSplits>>

    @Query("SELECT tabEntryId FROM tabentryentity")
    suspend fun getAllTabEntryIds(): List<String>

    @Query("SELECT tabEntryId FROM tabentryentity WHERE pendingSync = 1")
    suspend fun getPendingSyncIds(): List<String>

    @Transaction
    @Query("SELECT * FROM tabentryentity WHERE tabEntryId = :tabEntryId")
    suspend fun getTabEntryById(tabEntryId: String): TabEntryWithSplits?

    /**
     * Observes a single entry plus its splits. The `@Transaction` + `@Relation` makes Room
     * re-emit on changes to either the entry row itself or its splits, so edits to entries with
     * no splits (e.g. settlements) are reflected too.
     */
    @Transaction
    @Query("SELECT * FROM tabentryentity WHERE tabEntryId = :tabEntryId")
    fun observeTabEntryById(tabEntryId: String): Flow<TabEntryWithSplits?>

    /**
     * Atomically replaces a tab-entry plus its full set of splits. Used by the realtime sync to
     * apply server echoes: prevents leaving the row with stale/orphaned splits if the call is
     * interrupted between the delete and the upsert.
     */
    @Transaction
    suspend fun replaceTabEntryWithSplits(
        entry: TabEntryEntity,
        splits: List<TabEntrySplitEntity>,
        splitDao: TabEntrySplitDao,
    ) {
        upsertTabEntry(entry)
        splitDao.deleteSplitsByTabEntryIds(listOf(entry.tabEntryId))
        if (splits.isNotEmpty()) {
            splitDao.upsertSplits(splits)
        }
    }

    /** Atomically removes a tab-entry and all its splits. */
    @Transaction
    suspend fun deleteTabEntryAndSplits(
        tabEntryId: String,
        splitDao: TabEntrySplitDao,
    ) {
        splitDao.deleteSplitsByTabEntryIds(listOf(tabEntryId))
        deleteTabEntryById(tabEntryId)
    }

    /**
     * Applies a batch of tab entries from `/api/sync`. [aliveEntries] are entries the server still
     * reports as present (upserted with their canonical split sets); [deletedIds] are entries the
     * server soft-deleted (hard-deleted locally). Rows with a pending optimistic local write are
     * never touched — the WebSocket echo / outbox owns those.
     *
     * On a full sync [allServerIds] is the complete set of ids the payload reported (alive + deleted);
     * local non-pending entries absent from it are pruned. On a delta sync pass `null` to skip pruning.
     */
    @Transaction
    suspend fun applySyncedTabEntries(
        aliveEntries: List<TabEntryEntity>,
        splitsByEntryId: Map<String, List<TabEntrySplitEntity>>,
        deletedIds: List<String>,
        allServerIds: Set<String>?,
        splitDao: TabEntrySplitDao,
    ) {
        val pendingIds = getPendingSyncIds().toSet()

        val applicableAlive = aliveEntries.filter { it.tabEntryId !in pendingIds }
        if (applicableAlive.isNotEmpty()) {
            upsertTabEntries(applicableAlive)
            val aliveIds = applicableAlive.map { it.tabEntryId }
            splitDao.deleteSplitsByTabEntryIds(aliveIds)
            val splits = aliveIds.flatMap { splitsByEntryId[it].orEmpty() }
            if (splits.isNotEmpty()) {
                splitDao.upsertSplits(splits)
            }
        }

        val applicableDeleted = deletedIds.filter { it !in pendingIds }
        if (applicableDeleted.isNotEmpty()) {
            splitDao.deleteSplitsByTabEntryIds(applicableDeleted)
            deleteTabEntriesById(applicableDeleted)
        }

        if (allServerIds != null) {
            val staleIds = getAllTabEntryIds().filter { it !in allServerIds && it !in pendingIds }
            if (staleIds.isNotEmpty()) {
                splitDao.deleteSplitsByTabEntryIds(staleIds)
                deleteTabEntriesById(staleIds)
            }
        }
    }
}
