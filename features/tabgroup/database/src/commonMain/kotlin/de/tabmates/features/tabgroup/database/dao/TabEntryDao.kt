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

    @Query(
        "SELECT * FROM tabentryentity WHERE groupId = :groupId ORDER BY entryDate DESC, createdAt DESC LIMIT :limit",
    )
    suspend fun getTabEntriesByGroupIdLimited(
        groupId: String,
        limit: Int,
    ): List<TabEntryEntity>

    @Transaction
    @Query("SELECT * FROM tabentryentity WHERE tabEntryId = :tabEntryId")
    suspend fun getTabEntryById(tabEntryId: String): TabEntryWithSplits?

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

    @Transaction
    suspend fun upsertTabEntriesAndSyncIfNecessary(
        groupId: String,
        entries: List<TabEntryEntity>,
        splitsByEntryId: Map<String, List<TabEntrySplitEntity>>,
        splitDao: TabEntrySplitDao,
        pageSize: Int,
        shouldSync: Boolean = false,
    ) {
        val localEntries = getTabEntriesByGroupIdLimited(groupId, pageSize)

        upsertTabEntries(entries)
        if (entries.isNotEmpty()) {
            splitDao.deleteSplitsByTabEntryIds(entries.map { it.tabEntryId })
        }
        val allSplits = splitsByEntryId.values.flatten()
        if (allSplits.isNotEmpty()) {
            splitDao.upsertSplits(allSplits)
        }

        if (!shouldSync) return

        val serverIds = entries.map { it.tabEntryId }.toSet()
        val staleIds =
            localEntries
                .filter { it.tabEntryId !in serverIds && !it.pendingSync }
                .map { it.tabEntryId }
        if (staleIds.isNotEmpty()) {
            deleteTabEntriesById(staleIds)
        }
    }
}
