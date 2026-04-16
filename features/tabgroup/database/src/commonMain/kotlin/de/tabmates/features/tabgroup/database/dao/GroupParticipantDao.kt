package de.tabmates.features.tabgroup.database.dao

import androidx.room3.Dao
import androidx.room3.Query
import androidx.room3.Upsert
import de.tabmates.features.tabgroup.database.entities.GroupParticipantEntity

@Dao
interface GroupParticipantDao {
    @Upsert
    suspend fun upsertParticipant(participant: GroupParticipantEntity)

    @Upsert
    suspend fun upsertParticipants(participants: List<GroupParticipantEntity>)

    @Query("SELECT * FROM groupparticipantentity")
    suspend fun getAllParticipants(): List<GroupParticipantEntity>

    @Query("SELECT * FROM groupparticipantentity WHERE userId = :userId")
    suspend fun getParticipantById(userId: String): GroupParticipantEntity?
}
