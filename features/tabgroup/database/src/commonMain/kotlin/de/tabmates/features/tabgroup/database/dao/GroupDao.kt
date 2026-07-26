package de.tabmates.features.tabgroup.database.dao

import androidx.room3.Dao
import androidx.room3.Query
import androidx.room3.Transaction
import androidx.room3.Upsert
import de.tabmates.features.tabgroup.database.entities.GroupEntity
import de.tabmates.features.tabgroup.database.entities.GroupParticipantCrossRef
import de.tabmates.features.tabgroup.database.entities.GroupParticipantEntity
import de.tabmates.features.tabgroup.database.entities.GroupWithParticipants
import kotlinx.coroutines.flow.Flow

@Dao
interface GroupDao {
    @Upsert
    suspend fun upsertGroup(group: GroupEntity)

    @Upsert
    suspend fun upsertGroups(groups: List<GroupEntity>)

    @Query("DELETE FROM groupentity WHERE groupId = :groupId")
    suspend fun deleteGroupById(groupId: String)

    @Transaction
    @Query(
        """
        SELECT g.*
        FROM groupentity g
        LEFT JOIN (
            SELECT groupId, MAX(lastModifiedAt) AS lastEntryModifiedAt
            FROM tabentryentity
            GROUP BY groupId
        ) lm ON g.groupId = lm.groupId
        ORDER BY
            MAX(g.lastModifiedAt, COALESCE(lm.lastEntryModifiedAt, 0)) DESC,
            g.groupId
    """,
    )
    fun getGroupsWithParticipants(): Flow<List<GroupWithParticipants>>

    @Transaction
    @Query("SELECT * FROM groupentity WHERE groupId = :groupId")
    suspend fun getGroupById(groupId: String): GroupWithParticipants?

    @Query("DELETE FROM groupentity")
    suspend fun deleteAllGroups()

    @Query("SELECT groupId FROM groupentity")
    suspend fun getAllGroupIds(): List<String>

    @Query("DELETE FROM groupentity WHERE groupId IN (:groupIds)")
    suspend fun deleteGroupsByIds(groupIds: List<String>)

    @Query("SELECT COUNT(*) FROM groupentity")
    fun getGroupCount(): Flow<Int>

    @Query(
        """
        SELECT p.*
        FROM groupparticipantentity p
        JOIN groupparticipantcrossref gpcr ON p.userId = gpcr.userId
        WHERE gpcr.groupId = :groupId AND gpcr.isActive = 1
        ORDER BY p.username
    """,
    )
    fun getActiveParticipantsByGroupId(groupId: String): Flow<List<GroupParticipantEntity>>

    @Transaction
    @Query("SELECT * FROM groupentity WHERE groupId = :groupId")
    fun getGroupInfoById(groupId: String): Flow<GroupWithParticipants?>

    @Transaction
    suspend fun upsertGroupWithParticipantsAndCrossRefs(
        group: GroupEntity,
        participants: List<GroupParticipantEntity>,
        participantDao: GroupParticipantDao,
        crossRefDao: GroupParticipantCrossRefDao,
    ) {
        upsertGroup(group)
        participantDao.upsertParticipants(participants)

        val crossRefs =
            participants.map { participant ->
                GroupParticipantCrossRef(
                    groupId = group.groupId,
                    userId = participant.userId,
                    isActive = true,
                )
            }
        crossRefDao.upsertCrossRefs(crossRefs)
        crossRefDao.syncGroupParticipants(group.groupId, participants)
    }

    @Transaction
    suspend fun upsertGroupsWithParticipantsAndCrossRefs(
        groups: List<GroupWithParticipants>,
        participantDao: GroupParticipantDao,
        crossRefDao: GroupParticipantCrossRefDao,
    ) {
        upsertGroups(groups.map { it.group })

        val localGroupIds = getAllGroupIds()
        val serverGroupIds = groups.map { it.group.groupId }
        val staleGroupIds = localGroupIds.filterNot { serverGroupIds.contains(it) }

        val allParticipants = groups.flatMap { it.participants }
        participantDao.upsertParticipants(allParticipants)

        val allCrossRefs =
            groups.flatMap { groupWithParticipants ->
                groupWithParticipants.participants.map { participant ->
                    GroupParticipantCrossRef(
                        groupId = groupWithParticipants.group.groupId,
                        userId = participant.userId,
                        isActive = true,
                    )
                }
            }
        crossRefDao.upsertCrossRefs(allCrossRefs)

        groups.forEach { groupWithParticipant ->
            crossRefDao.syncGroupParticipants(
                groupId = groupWithParticipant.group.groupId,
                participants = groupWithParticipant.participants,
            )
        }

        deleteGroupsByIds(staleGroupIds)
    }

    /**
     * Merges the groups returned by a delta sync. Unlike [upsertGroupsWithParticipantsAndCrossRefs],
     * which reconciles against the full server list, [groups] here holds only the groups that changed
     * since the sync cursor, so pruning is driven by [activeGroupIds] — the complete current set of
     * the user's group ids. Any local group whose id is absent from [activeGroupIds] (left, removed,
     * or deleted) is dropped.
     */
    @Transaction
    suspend fun syncChangedGroups(
        groups: List<GroupWithParticipants>,
        activeGroupIds: List<String>,
        participantDao: GroupParticipantDao,
        crossRefDao: GroupParticipantCrossRefDao,
    ) {
        upsertGroups(groups.map { it.group })

        val allParticipants = groups.flatMap { it.participants }
        participantDao.upsertParticipants(allParticipants)

        val allCrossRefs =
            groups.flatMap { groupWithParticipants ->
                groupWithParticipants.participants.map { participant ->
                    GroupParticipantCrossRef(
                        groupId = groupWithParticipants.group.groupId,
                        userId = participant.userId,
                        isActive = true,
                    )
                }
            }
        crossRefDao.upsertCrossRefs(allCrossRefs)

        groups.forEach { groupWithParticipant ->
            crossRefDao.syncGroupParticipants(
                groupId = groupWithParticipant.group.groupId,
                participants = groupWithParticipant.participants,
            )
        }

        val activeIds = activeGroupIds.toSet()
        val staleGroupIds = getAllGroupIds().filterNot { it in activeIds }
        deleteGroupsByIds(staleGroupIds)
    }
}
