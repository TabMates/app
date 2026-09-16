package de.tabmates.features.tabgroup.database.dao

import androidx.room3.Dao
import androidx.room3.Insert
import androidx.room3.OnConflictStrategy
import androidx.room3.Query
import de.tabmates.features.tabgroup.database.entities.RecurringSlotClaimEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface RecurringSlotClaimDao {
    /**
     * Records slots as claimed. Insert-ignore, never upsert: a claim carries no state worth
     * refreshing, and seeing the same generated entry twice (a sync after a websocket broadcast) is
     * the normal case rather than the exception.
     */
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun recordClaims(claims: List<RecurringSlotClaimEntity>)

    @Query("SELECT * FROM recurringslotclaimentity WHERE groupId = :groupId")
    fun observeClaimsForGroup(groupId: String): Flow<List<RecurringSlotClaimEntity>>

    /**
     * Drops the claims of groups that are gone. Called wherever groups are pruned — the table
     * carries no foreign key, so nothing removes these rows on its own.
     */
    @Query("DELETE FROM recurringslotclaimentity WHERE groupId NOT IN (SELECT groupId FROM groupentity)")
    suspend fun deleteClaimsForRemovedGroups()
}
