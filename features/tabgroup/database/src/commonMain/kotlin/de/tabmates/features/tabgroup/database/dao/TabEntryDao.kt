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
    @Query("SELECT * FROM tabentryentity WHERE groupId = :groupId ORDER BY createdAt DESC")
    fun getTabEntriesByGroupId(groupId: String): Flow<List<TabEntryWithSplits>>

    @Query("SELECT * FROM tabentryentity WHERE groupId = :groupId ORDER BY createdAt DESC LIMIT :limit")
    suspend fun getTabEntriesByGroupIdLimited(
        groupId: String,
        limit: Int,
    ): List<TabEntryEntity>

    @Transaction
    @Query("SELECT * FROM tabentryentity WHERE tabEntryId = :tabEntryId")
    suspend fun getTabEntryById(tabEntryId: String): TabEntryWithSplits?

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
                .filter { it.tabEntryId !in serverIds }
                .map { it.tabEntryId }
        if (staleIds.isNotEmpty()) {
            deleteTabEntriesById(staleIds)
        }
    }
}
