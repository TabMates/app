package de.tabmates.features.tabgroup.data.group

import de.tabmates.core.domain.util.DataError
import de.tabmates.core.domain.util.Result
import de.tabmates.core.domain.util.onSuccess
import de.tabmates.features.tabgroup.data.mappers.toDomain
import de.tabmates.features.tabgroup.data.mappers.toEntity
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
import org.koin.core.annotation.Single

@Single(binds = [GroupRepository::class])
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

    override suspend fun fetchGroups(): Result<List<Group>, DataError.Remote> {
        return groupService
            .getGroups()
            .onSuccess { groups ->
                val groupsWithParticipants =
                    groups.map { group ->
                        GroupWithParticipants(
                            group = group.toEntity(),
                            participants = group.participants.map { it.toEntity() },
                            lastTabEntry = null,
                        )
                    }

                database.groupDao.upsertGroupsWithParticipantsAndCrossRefs(
                    groups = groupsWithParticipants,
                    participantDao = database.groupParticipantDao,
                    crossRefDao = database.groupParticipantCrossRefDao,
                )
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
