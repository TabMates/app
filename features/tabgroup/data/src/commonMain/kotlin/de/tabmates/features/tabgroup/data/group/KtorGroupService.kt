package de.tabmates.features.tabgroup.data.group

import de.tabmates.core.data.networking.delete
import de.tabmates.core.data.networking.get
import de.tabmates.core.data.networking.patch
import de.tabmates.core.data.networking.post
import de.tabmates.core.domain.util.DataError
import de.tabmates.core.domain.util.EmptyResult
import de.tabmates.core.domain.util.Result
import de.tabmates.core.domain.util.asEmptyResult
import de.tabmates.core.domain.util.map
import de.tabmates.features.tabgroup.data.dto.GroupDto
import de.tabmates.features.tabgroup.data.dto.GroupInvitePreviewDto
import de.tabmates.features.tabgroup.data.dto.request.AddNewParticipantToGroupRequest
import de.tabmates.features.tabgroup.data.dto.request.CreateGroupRequest
import de.tabmates.features.tabgroup.data.dto.request.JoinGroupRequest
import de.tabmates.features.tabgroup.data.dto.request.ParticipantsRequest
import de.tabmates.features.tabgroup.data.dto.request.UpdateGroupRequest
import de.tabmates.features.tabgroup.data.mappers.toDomain
import de.tabmates.features.tabgroup.domain.group.GroupService
import de.tabmates.features.tabgroup.domain.models.Group
import de.tabmates.features.tabgroup.domain.models.GroupInvitePreview
import io.ktor.client.HttpClient
import org.koin.core.annotation.Single

@Single(binds = [GroupService::class])
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

    override suspend fun createGroup(
        title: String,
        description: String?,
        defaultCurrencyCode: String,
        otherUserIds: Set<String>,
    ): Result<Group, DataError.Remote> {
        return httpClient
            .post<CreateGroupRequest, GroupDto>(
                route = "/api/group",
                body =
                    CreateGroupRequest(
                        title = title,
                        description = description,
                        defaultCurrencyCode = defaultCurrencyCode,
                        otherParticipantUserIds = otherUserIds.toList(),
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

    override suspend fun updateGroup(
        groupId: String,
        title: String,
        description: String?,
        defaultCurrencyCode: String,
    ): Result<Group, DataError.Remote> {
        return httpClient
            .patch<UpdateGroupRequest, GroupDto>(
                route = "/api/group/$groupId",
                body = UpdateGroupRequest(
                    title = title,
                    description = description,
                    defaultCurrencyCode = defaultCurrencyCode,
                ),
            ).map { it.toDomain() }
    }

    override suspend fun rotateInviteToken(groupId: String): Result<Group, DataError.Remote> {
        return httpClient
            .post<Unit, GroupDto>(
                route = "/api/group/$groupId/invite/rotate",
                body = Unit,
            ).map { it.toDomain() }
    }

    override suspend fun previewInvite(token: String): Result<GroupInvitePreview, DataError.Remote> {
        return httpClient
            .get<GroupInvitePreviewDto>(
                route = "/api/group/invite/preview/$token",
            ).map { it.toDomain() }
    }

    override suspend fun joinGroup(
        token: String,
        claimPlaceholderId: String?,
    ): Result<Group, DataError.Remote> {
        return httpClient
            .post<JoinGroupRequest, GroupDto>(
                route = "/api/group/join",
                body = JoinGroupRequest(token = token, claimPlaceholderId = claimPlaceholderId),
            ).map { it.toDomain() }
    }
}
