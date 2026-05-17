package de.tabmates.features.tabgroup.domain.group

import de.tabmates.core.domain.util.DataError
import de.tabmates.core.domain.util.EmptyResult
import de.tabmates.core.domain.util.Result
import de.tabmates.features.tabgroup.domain.models.Group
import de.tabmates.features.tabgroup.domain.models.GroupInvitePreview

interface GroupService {
    suspend fun getGroups(): Result<List<Group>, DataError.Remote>

    suspend fun getGroupById(groupId: String): Result<Group, DataError.Remote>

    suspend fun createGroup(
        title: String,
        description: String?,
        defaultCurrencyCode: String,
        otherUserIds: Set<String>,
    ): Result<Group, DataError.Remote>

    suspend fun addParticipantsToGroup(
        groupId: String,
        userIds: Set<String>,
    ): Result<Group, DataError.Remote>

    suspend fun addNewParticipantsToGroup(
        groupId: String,
        usernames: List<String>,
    ): Result<Group, DataError.Remote>

    suspend fun leaveGroup(groupId: String): EmptyResult<DataError.Remote>

    suspend fun updateGroup(
        groupId: String,
        title: String,
        description: String?,
        defaultCurrencyCode: String,
    ): Result<Group, DataError.Remote>

    suspend fun rotateInviteToken(groupId: String): Result<Group, DataError.Remote>

    suspend fun previewInvite(token: String): Result<GroupInvitePreview, DataError.Remote>

    suspend fun joinGroup(
        token: String,
        claimPlaceholderId: String?,
    ): Result<Group, DataError.Remote>
}
