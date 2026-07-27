package de.tabmates.features.tabgroup.presentation.navigation.creategroup

import de.tabmates.core.domain.util.DataError
import de.tabmates.core.domain.util.EmptyResult
import de.tabmates.core.domain.util.Result
import de.tabmates.features.tabgroup.domain.group.GroupRepository
import de.tabmates.features.tabgroup.domain.models.Group
import de.tabmates.features.tabgroup.domain.models.GroupInvitePreview
import de.tabmates.features.tabgroup.domain.models.GroupParticipant
import de.tabmates.features.tabgroup.domain.models.ParticipantType
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flowOf
import kotlin.time.Instant

class FakeGroupRepository(
    var createGroupResult: Result<Group, DataError.Remote> = Result.Success(DEFAULT_GROUP),
    initialGroups: List<Group> = emptyList(),
    initialAllParticipants: List<GroupParticipant> = emptyList(),
) : GroupRepository {
    data class CreateGroupCall(
        val title: String,
        val description: String?,
        val defaultCurrencyCode: String,
        val otherUserIds: Set<String>,
    )

    val createGroupCalls: MutableList<CreateGroupCall> = mutableListOf()

    private val groupsFlow = MutableStateFlow(initialGroups)
    private val allParticipantsFlow = MutableStateFlow(initialAllParticipants)

    fun emitGroups(groups: List<Group>) {
        groupsFlow.value = groups
    }

    override fun getGroups(): Flow<List<Group>> = groupsFlow

    override fun getActiveParticipantsByGroupId(groupId: String): Flow<List<GroupParticipant>> =
        flowOf(emptyList())

    override fun getAllParticipants(): Flow<List<GroupParticipant>> = allParticipantsFlow

    override suspend fun fetchGroupById(groupId: String): EmptyResult<DataError.Remote> = Result.Success(Unit)

    override suspend fun createGroup(
        title: String,
        description: String?,
        defaultCurrencyCode: String,
        otherUserIds: Set<String>,
    ): Result<Group, DataError.Remote> {
        createGroupCalls += CreateGroupCall(title, description, defaultCurrencyCode, otherUserIds)
        return createGroupResult
    }

    override suspend fun leaveGroup(groupId: String): EmptyResult<DataError.Remote> = Result.Success(Unit)

    override suspend fun addParticipantsToGroup(
        groupId: String,
        userIds: Set<String>,
    ): Result<Group, DataError.Remote> = Result.Failure(DataError.Remote.UNKNOWN)

    data class AddNewParticipantsCall(
        val groupId: String,
        val usernames: List<String>,
    )

    val addNewParticipantsCalls: MutableList<AddNewParticipantsCall> = mutableListOf()
    var addNewParticipantsResult: Result<Group, DataError.Remote> = Result.Success(DEFAULT_GROUP)

    override suspend fun addNewParticipantsToGroup(
        groupId: String,
        usernames: List<String>,
    ): Result<Group, DataError.Remote> {
        addNewParticipantsCalls += AddNewParticipantsCall(groupId, usernames)
        return addNewParticipantsResult
    }

    data class UpdateGroupCall(
        val groupId: String,
        val title: String,
        val description: String?,
        val defaultCurrencyCode: String,
    )

    val updateGroupCalls: MutableList<UpdateGroupCall> = mutableListOf()
    var updateGroupResult: Result<Group, DataError.Remote> = Result.Success(DEFAULT_GROUP)

    override suspend fun updateGroup(
        groupId: String,
        title: String,
        description: String?,
        defaultCurrencyCode: String,
    ): Result<Group, DataError.Remote> {
        updateGroupCalls += UpdateGroupCall(groupId, title, description, defaultCurrencyCode)
        return updateGroupResult
    }

    override suspend fun rotateInviteToken(groupId: String): Result<Group, DataError.Remote> =
        Result.Failure(DataError.Remote.UNKNOWN)

    var previewInviteResult: Result<GroupInvitePreview, DataError.Remote> =
        Result.Failure(DataError.Remote.UNKNOWN)
    val previewInviteCalls: MutableList<String> = mutableListOf()

    override suspend fun previewInvite(token: String): Result<GroupInvitePreview, DataError.Remote> {
        previewInviteCalls += token
        return previewInviteResult
    }

    data class JoinGroupCall(
        val token: String,
        val claimPlaceholderId: String?,
    )

    var joinGroupResult: Result<Group, DataError.Remote> = Result.Success(DEFAULT_GROUP)
    val joinGroupCalls: MutableList<JoinGroupCall> = mutableListOf()

    override suspend fun joinGroup(
        token: String,
        claimPlaceholderId: String?,
    ): Result<Group, DataError.Remote> {
        joinGroupCalls += JoinGroupCall(token, claimPlaceholderId)
        return joinGroupResult
    }

    override suspend fun deleteAllGroups() = Unit

    companion object {
        private val FAKE_PARTICIPANT =
            GroupParticipant(
                userId = "creator",
                username = "creator",
                participantType = ParticipantType.REGISTERED,
            )
        val DEFAULT_GROUP =
            Group(
                id = "g1",
                title = "Title",
                description = null,
                defaultCurrencyCode = "EUR",
                participants = setOf(FAKE_PARTICIPANT),
                creator = FAKE_PARTICIPANT,
                inviteToken = "",
                lastActivityAt = Instant.fromEpochMilliseconds(0),
                lastTabEntry = null,
                createdAt = Instant.fromEpochMilliseconds(0),
            )
    }
}
