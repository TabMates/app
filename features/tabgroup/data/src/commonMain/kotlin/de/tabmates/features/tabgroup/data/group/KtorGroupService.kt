package de.tabmates.features.tabgroup.data.group

import de.tabmates.core.data.networking.delete
import de.tabmates.core.data.networking.get
import de.tabmates.core.data.networking.post
import de.tabmates.core.domain.util.DataError
import de.tabmates.core.domain.util.EmptyResult
import de.tabmates.core.domain.util.Result
import de.tabmates.core.domain.util.asEmptyResult
import de.tabmates.core.domain.util.map
import de.tabmates.features.tabgroup.data.dto.GroupDto
import de.tabmates.features.tabgroup.data.dto.request.AddNewParticipantToGroupRequest
import de.tabmates.features.tabgroup.data.dto.request.ParticipantsRequest
import de.tabmates.features.tabgroup.data.mappers.toDomain
import de.tabmates.features.tabgroup.domain.group.GroupService
import de.tabmates.features.tabgroup.domain.models.Group
import io.ktor.client.HttpClient

class KtorGroupService(
    private val httpClient: HttpClient,
) : GroupService {
    override suspend fun getGroups(): Result<List<Group>, DataError.Remote> {
        return httpClient
            .get<List<GroupDto>>(
                route = "/api/group",
            ).map { groupDtos ->
                groupDtos.map { it.toDomain() }
            }
    }

    override suspend fun getGroupById(groupId: String): Result<Group, DataError.Remote> {
        return httpClient
            .get<GroupDto>(
                route = "/api/group/$groupId",
            ).map { it.toDomain() }
    }

    override suspend fun createGroup(otherUserIds: Set<String>): Result<Group, DataError.Remote> {
        return httpClient
            .post<ParticipantsRequest, GroupDto>(
                route = "/api/group",
                body =
                    ParticipantsRequest(
                        userIds = otherUserIds.toList(),
                    ),
            ).map { it.toDomain() }
    }

    override suspend fun addParticipantsToGroup(
        groupId: String,
        userIds: Set<String>,
    ): Result<Group, DataError.Remote> {
        return httpClient
            .post<ParticipantsRequest, GroupDto>(
                route = "/api/group/$groupId/add",
                body =
                    ParticipantsRequest(
                        userIds = userIds.toList(),
                    ),
            ).map { it.toDomain() }
    }

    override suspend fun addNewParticipantsToGroup(
        groupId: String,
        usernames: List<String>,
    ): Result<Group, DataError.Remote> {
        return httpClient
            .post<AddNewParticipantToGroupRequest, GroupDto>(
                route = "/api/group/$groupId/add-new",
                body =
                    AddNewParticipantToGroupRequest(
                        usernames = usernames,
                    ),
            ).map { it.toDomain() }
    }

    override suspend fun leaveGroup(groupId: String): EmptyResult<DataError.Remote> {
        return httpClient
            .delete<Unit>(
                route = "/api/group/$groupId/leave",
            ).asEmptyResult()
    }
}
