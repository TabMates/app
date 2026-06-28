package de.tabmates.features.tabgroup.database.dao

import androidx.room3.Dao
import androidx.room3.Query
import androidx.room3.Upsert
import de.tabmates.features.tabgroup.database.entities.TabEntrySplitEntity

@Dao
interface TabEntrySplitDao {
    @Upsert
    suspend fun upsertSplit(split: TabEntrySplitEntity)

    @Upsert
    suspend fun upsertSplits(splits: List<TabEntrySplitEntity>)

    @Query("DELETE FROM TabEntrySplitEntity WHERE splitId = :splitId")
    suspend fun deleteSplitById(splitId: String)

    @Query("DELETE FROM TabEntrySplitEntity WHERE tabEntryId IN (:tabEntryIds)")
    suspend fun deleteSplitsByTabEntryIds(tabEntryIds: List<String>)

    @Query("SELECT * FROM TabEntrySplitEntity WHERE tabEntryId = :tabEntryId")
    suspend fun getSplitsByTabEntryIdOnce(tabEntryId: String): List<TabEntrySplitEntity>
}
