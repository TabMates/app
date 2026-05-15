package de.tabmates.features.tabgroup.data.group

import de.tabmates.core.domain.util.DataError
import de.tabmates.core.domain.util.EmptyResult
import de.tabmates.core.domain.util.Result
import de.tabmates.core.domain.util.asEmptyResult
import de.tabmates.core.domain.util.onSuccess
import de.tabmates.features.tabgroup.data.mappers.toDomain
import de.tabmates.features.tabgroup.data.mappers.toEntity
import de.tabmates.features.tabgroup.database.TabMatesDatabase
import de.tabmates.features.tabgroup.database.entities.GroupParticipantEntity
import de.tabmates.features.tabgroup.database.entities.GroupWithParticipants
import de.tabmates.features.tabgroup.domain.group.GroupRepository
import de.tabmates.features.tabgroup.domain.group.GroupService
import de.tabmates.features.tabgroup.domain.models.Group
import de.tabmates.features.tabgroup.domain.models.GroupParticipant
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

    override fun getActiveParticipantsByGroupId(groupId: String): Flow<List<GroupParticipant>> {
        return database.groupDao
            .getActiveParticipantsByGroupId(groupId)
            .map { participants ->
                participants.map { it.toDomain() }
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

    override suspend fun fetchGroupById(groupId: String): EmptyResult<DataError.Remote> {
        return groupService
            .getGroupById(groupId)
            .onSuccess { group ->
                database.groupDao.upsertGroupWithParticipantsAndCrossRefs(
                    group = group.toEntity(),
                    participants = group.participants.map { it.toEntity() },
                    participantDao = database.groupParticipantDao,
                    crossRefDao = database.groupParticipantCrossRefDao,
                )
            }.asEmptyResult()
    }

    override suspend fun createGroup(
        title: String,
        description: String?,
        defaultCurrencyCode: String,
        otherUserIds: Set<String>,
    ): Result<Group, DataError.Remote> {
        return groupService
            .createGroup(
                title = title,
                description = description,
                defaultCurrencyCode = defaultCurrencyCode,
                otherUserIds = otherUserIds,
            ).onSuccess { chat ->
                database.groupDao.upsertGroupWithParticipantsAndCrossRefs(
                    group = chat.toEntity(),
                    participants = chat.participants.map { it.toEntity() },
                    participantDao = database.groupParticipantDao,
                    crossRefDao = database.groupParticipantCrossRefDao,
                )
            }
    }

    override suspend fun leaveGroup(groupId: String): EmptyResult<DataError.Remote> {
        return groupService
            .leaveGroup(groupId)
            .onSuccess {
                database.groupDao.deleteGroupById(groupId)
            }
    }

    override suspend fun addParticipantsToGroup(
        groupId: String,
        userIds: Set<String>,
    ): Result<Group, DataError.Remote> {
        return groupService
            .addParticipantsToGroup(groupId, userIds)
            .onSuccess { group ->
                database.groupDao.upsertGroupWithParticipantsAndCrossRefs(
                    group = group.toEntity(),
                    participants = group.participants.map { it.toEntity() },
                    participantDao = database.groupParticipantDao,
                    crossRefDao = database.groupParticipantCrossRefDao,
                )
            }
    }

    override suspend fun addNewParticipantsToGroup(
        groupId: String,
        usernames: List<String>,
    ): Result<Group, DataError.Remote> {
        return groupService
            .addNewParticipantsToGroup(groupId, usernames)
            .onSuccess { group ->
                database.groupDao.upsertGroupWithParticipantsAndCrossRefs(
                    group = group.toEntity(),
                    participants = group.participants.map { it.toEntity() },
                    participantDao = database.groupParticipantDao,
                    crossRefDao = database.groupParticipantCrossRefDao,
                )
            }
    }

    override suspend fun rotateInviteToken(groupId: String): Result<Group, DataError.Remote> {
        return groupService
            .rotateInviteToken(groupId)
            .onSuccess { group ->
                database.groupDao.upsertGroupWithParticipantsAndCrossRefs(
                    group = group.toEntity(),
                    participants = group.participants.map { it.toEntity() },
                    participantDao = database.groupParticipantDao,
                    crossRefDao = database.groupParticipantCrossRefDao,
                )
            }
    }

    override suspend fun joinGroup(token: String): Result<Group, DataError.Remote> {
        return groupService
            .joinGroup(token)
            .onSuccess { group ->
                database.groupDao.upsertGroupWithParticipantsAndCrossRefs(
                    group = group.toEntity(),
                    participants = group.participants.map { it.toEntity() },
                    participantDao = database.groupParticipantDao,
                    crossRefDao = database.groupParticipantCrossRefDao,
                )
            }
    }

    override suspend fun deleteAllGroups() {
        database.groupDao.deleteAllGroups()
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
