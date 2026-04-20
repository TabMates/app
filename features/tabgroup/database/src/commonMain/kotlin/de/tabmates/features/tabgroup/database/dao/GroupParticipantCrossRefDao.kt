package de.tabmates.features.tabgroup.database.dao

import androidx.room3.Dao
import androidx.room3.Query
import androidx.room3.Transaction
import androidx.room3.Upsert
import de.tabmates.features.tabgroup.database.entities.GroupParticipantCrossRef
import de.tabmates.features.tabgroup.database.entities.GroupParticipantEntity

@Dao
interface GroupParticipantCrossRefDao {
    @Upsert
    suspend fun upsertCrossRefs(crossRefs: List<GroupParticipantCrossRef>)

    @Query("SELECT userId FROM groupparticipantcrossref WHERE groupId = :groupId AND isActive = 1")
    suspend fun getActiveParticipantIdsForGroup(groupId: String): List<String>

    @Query("SELECT userId FROM groupparticipantcrossref WHERE groupId = :groupId")
    suspend fun getAllParticipantIdsForGroup(groupId: String): List<String>

    @Query(
        """
        UPDATE groupparticipantcrossref
        SET isActive = 0
        WHERE groupId = :groupId AND userId IN (:userIds)
    """,
    )
    suspend fun markParticipantAsInactive(
        groupId: String,
        userIds: List<String>,
    )

    @Query(
        """
        UPDATE groupparticipantcrossref
        SET isActive = 1
        WHERE groupId = :groupId AND userId IN (:userIds)
    """,
    )
    suspend fun reactivateParticipants(
        groupId: String,
        userIds: List<String>,
    )

    @Transaction
    suspend fun syncGroupParticipants(
        groupId: String,
        participants: List<GroupParticipantEntity>,
    ) {
        val serverParticipantIds = participants.map { it.userId }.toSet()
        val allLocalParticipantIds = getAllParticipantIdsForGroup(groupId).toSet()
        val activeLocalParticipantIds = getActiveParticipantIdsForGroup(groupId).toSet()
        val inactiveLocalParticipantIds = allLocalParticipantIds - activeLocalParticipantIds

        val participantsToReactivate = serverParticipantIds.intersect(inactiveLocalParticipantIds)
        val participantsToDeactivate = activeLocalParticipantIds - serverParticipantIds

        markParticipantAsInactive(groupId, participantsToDeactivate.toList())

        if (participants.isEmpty()) return

        reactivateParticipants(groupId, participantsToReactivate.toList())

        val newParticipants = serverParticipantIds - allLocalParticipantIds
        val newCrossRefs =
            newParticipants.map { userId ->
                GroupParticipantCrossRef(
                    groupId = groupId,
                    userId = userId,
                    isActive = true,
                )
            }
        upsertCrossRefs(newCrossRefs)
    }
}
