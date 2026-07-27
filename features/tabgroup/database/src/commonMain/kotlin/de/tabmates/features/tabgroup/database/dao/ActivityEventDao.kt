package de.tabmates.features.tabgroup.database.dao

import androidx.room3.Dao
import androidx.room3.Insert
import androidx.room3.Query
import androidx.room3.Transaction
import androidx.room3.Upsert
import de.tabmates.features.tabgroup.database.entities.ActivityEventEntity
import de.tabmates.features.tabgroup.database.entities.ActivityEventWithChanges
import de.tabmates.features.tabgroup.database.entities.ActivityFieldChangeEntity
import de.tabmates.features.tabgroup.database.entities.ConfirmedEntryVersion
import kotlinx.coroutines.flow.Flow

@Dao
interface ActivityEventDao {
    @Upsert
    suspend fun upsertEvents(events: List<ActivityEventEntity>)

    @Insert
    suspend fun insertChanges(changes: List<ActivityFieldChangeEntity>)

    @Query("DELETE FROM activityfieldchangeentity WHERE activityEventId IN (:eventIds)")
    suspend fun deleteChangesForEvents(eventIds: List<String>)

    /**
     * Applies a batch of events with their changes.
     *
     * Idempotent, which is what lets the paged fetch and the WebSocket push write through the same
     * path without coordinating: events are upserted by id, and their children are cleared before
     * reinsert so a replayed page cannot duplicate them (the children's keys are generated, so a
     * plain upsert would not collapse them).
     */
    @Transaction
    suspend fun upsertPage(
        events: List<ActivityEventEntity>,
        changes: List<ActivityFieldChangeEntity>,
    ) {
        if (events.isEmpty()) return
        upsertEvents(events)
        deleteChangesForEvents(events.map { it.id })
        if (changes.isNotEmpty()) {
            insertChanges(changes)
        }
    }

    /** Highest mirrored `seq`, for diagnostics and to detect an empty mirror. */
    @Query("SELECT MAX(seq) FROM activityevententity")
    suspend fun getMaxSeq(): Long?

    /**
     * Change rows across the whole mirror. Nothing in the app reads this; it exists so a test can
     * assert the group cascade actually reaches the children, which no call site deletes explicitly.
     */
    @Query("SELECT COUNT(*) FROM activityfieldchangeentity")
    suspend fun countChanges(): Int

    /**
     * The newest [limit] events across every group. `@Transaction` plus `@Relation` makes Room
     * re-emit when either an event row or one of its change rows moves.
     */
    @Transaction
    @Query("SELECT * FROM activityevententity ORDER BY seq DESC LIMIT :limit")
    fun observeAccountFeed(limit: Int): Flow<List<ActivityEventWithChanges>>

    @Transaction
    @Query("SELECT * FROM activityevententity WHERE groupId = :groupId ORDER BY seq DESC LIMIT :limit")
    fun observeGroupFeed(
        groupId: String,
        limit: Int,
    ): Flow<List<ActivityEventWithChanges>>

    /**
     * What the server has confirmed about the given entries, for the pending-row dedupe. Scoped to
     * the ids that actually have a pending write (never more than a handful) rather than aggregating
     * the whole log on every change.
     */
    @Query(
        """
        SELECT tabEntryId AS tabEntryId,
               MAX(entryVersion) AS maxVersion,
               MAX(CASE WHEN type = 'ENTRY_DELETED' THEN 1 ELSE 0 END) AS deleted
        FROM activityevententity
        WHERE tabEntryId IN (:tabEntryIds)
        GROUP BY tabEntryId
        """,
    )
    fun observeConfirmedEntryVersions(tabEntryIds: List<String>): Flow<List<ConfirmedEntryVersion>>

    /**
     * Not on the pruning hot path — the foreign key onto the group owns that. Kept for tests and for
     * an explicit local reset.
     */
    @Query("DELETE FROM activityevententity WHERE groupId = :groupId")
    suspend fun deleteByGroupId(groupId: String)
}
