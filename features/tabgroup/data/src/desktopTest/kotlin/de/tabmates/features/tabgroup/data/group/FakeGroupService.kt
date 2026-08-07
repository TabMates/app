package de.tabmates.features.tabgroup.data.group

import de.tabmates.core.domain.util.DataError
import de.tabmates.core.domain.util.EmptyResult
import de.tabmates.core.domain.util.Result
import de.tabmates.features.tabgroup.domain.group.GroupService
import de.tabmates.features.tabgroup.domain.models.Group
import de.tabmates.features.tabgroup.domain.models.GroupInvitePreview

/** Only [joinGroup] is scripted; the join tests never touch the other endpoints. */
class FakeGroupService(
    var joinGroupResult: Result<Group, DataError.Remote>,
) : GroupService {
    override suspend fun joinGroup(
        token: String,
        claimPlaceholderId: String?,
    ): Result<Group, DataError.Remote> = joinGroupResult

    override suspend fun getGroupById(groupId: String): Result<Group, DataError.Remote> = unused()

    override suspend fun createGroup(
        title: String,
        description: String?,
        defaultCurrencyCode: String,
        otherUserIds: Set<String>,
    ): Result<Group, DataError.Remote> = unused()

    override suspend fun addParticipantsToGroup(
        groupId: String,
        userIds: Set<String>,
    ): Result<Group, DataError.Remote> = unused()

    override suspend fun addNewParticipantsToGroup(
        groupId: String,
        usernames: List<String>,
    ): Result<Group, DataError.Remote> = unused()

    override suspend fun leaveGroup(groupId: String): EmptyResult<DataError.Remote> = unused()

    override suspend fun removeParticipant(
        groupId: String,
        userId: String,
    ): EmptyResult<DataError.Remote> = unused()

    override suspend fun updateGroup(
        groupId: String,
        title: String,
        description: String?,
        defaultCurrencyCode: String,
    ): Result<Group, DataError.Remote> = unused()

    override suspend fun rotateInviteToken(groupId: String): Result<Group, DataError.Remote> = unused()

    override suspend fun previewInvite(token: String): Result<GroupInvitePreview, DataError.Remote> = unused()

    private fun unused(): Nothing = error("not used by these tests")
}
