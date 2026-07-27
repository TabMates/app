package de.tabmates.features.tabgroup.database.dao

import androidx.room3.Dao
import androidx.room3.Insert
import androidx.room3.OnConflictStrategy
import androidx.room3.Query
import androidx.room3.Upsert
import de.tabmates.features.tabgroup.database.entities.GroupParticipantEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface GroupParticipantDao {
    @Upsert
    suspend fun upsertParticipant(participant: GroupParticipantEntity)

    @Upsert
    suspend fun upsertParticipants(participants: List<GroupParticipantEntity>)

    /** Inserts only rows whose userId doesn't exist yet; existing rows are left untouched. */
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertParticipantsIgnoringConflicts(participants: List<GroupParticipantEntity>)

    @Query("SELECT * FROM groupparticipantentity")
    suspend fun getAllParticipants(): List<GroupParticipantEntity>

    /**
     * Every known participant, across groups and regardless of membership. This table is never pruned
     * per group, which is what lets an activity diff still name someone who has since left.
     */
    @Query("SELECT * FROM groupparticipantentity")
    fun observeAllParticipants(): Flow<List<GroupParticipantEntity>>

    @Query("SELECT * FROM groupparticipantentity WHERE userId = :userId")
    suspend fun getParticipantById(userId: String): GroupParticipantEntity?
}
