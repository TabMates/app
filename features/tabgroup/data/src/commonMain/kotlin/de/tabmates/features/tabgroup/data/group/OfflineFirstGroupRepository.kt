package de.tabmates.features.tabgroup.data.group

import de.tabmates.features.tabgroup.data.mappers.toDomain
import de.tabmates.features.tabgroup.database.TabMatesDatabase
import de.tabmates.features.tabgroup.database.entities.GroupParticipantEntity
import de.tabmates.features.tabgroup.database.entities.GroupWithParticipants
import de.tabmates.features.tabgroup.domain.group.GroupRepository
import de.tabmates.features.tabgroup.domain.group.GroupService
import de.tabmates.features.tabgroup.domain.models.Group
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.supervisorScope

class OfflineFirstGroupRepository(
    private val groupService: GroupService,
    private val database: TabMatesDatabase,
) : GroupRepository {
    override fun getGroups(): Flow<List<Group>> {
        return database.groupDao
            .getGroupsWithParticipants()
            .map { allGroupsWithParticipants ->
                supervisorScope {
                    allGroupsWithParticipants
                        .map { groupWithParticipants ->
                            async {
                                GroupWithParticipants(
                                    group = groupWithParticipants.group,
                                    participants =
                                        groupWithParticipants
                                            .participants
                                            .onlyActive(groupWithParticipants.group.groupId),
                                    lastTabEntry = groupWithParticipants.lastTabEntry,
                                ).toDomain()
                            }
                        }.awaitAll()
                }
            }
    }

    private suspend fun List<GroupParticipantEntity>.onlyActive(groupId: String): List<GroupParticipantEntity> {
        val activeParticipantIds =
            database
                .groupDao
                .getActiveParticipantsByGroupId(groupId)
                .first()
                .map { it.userId }

        return this.filter { it.userId in activeParticipantIds }
    }
}
