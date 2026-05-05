package de.tabmates.features.tabgroup.presentation.navigation.creategroup

import de.tabmates.core.domain.util.DataError
import de.tabmates.core.domain.util.EmptyResult
import de.tabmates.core.domain.util.Result
import de.tabmates.features.tabgroup.domain.group.GroupService
import de.tabmates.features.tabgroup.domain.models.Group
import de.tabmates.features.tabgroup.domain.models.GroupParticipant
import de.tabmates.features.tabgroup.domain.models.ParticipantType
import kotlin.time.Instant

class FakeGroupService(
    var createGroupResult: Result<Group, DataError.Remote> = Result.Success(DEFAULT_GROUP),
) : GroupService {
    data class CreateGroupCall(
        val title: String,
        val description: String?,
        val defaultCurrencyCode: String,
        val otherUserIds: Set<String>,
    )

    val createGroupCalls: MutableList<CreateGroupCall> = mutableListOf()

    override suspend fun getGroups(): Result<List<Group>, DataError.Remote> = Result.Success(emptyList())

    override suspend fun getGroupById(groupId: String): Result<Group, DataError.Remote> =
        Result.Failure(DataError.Remote.UNKNOWN)

    override suspend fun createGroup(
        title: String,
        description: String?,
        defaultCurrencyCode: String,
        otherUserIds: Set<String>,
    ): Result<Group, DataError.Remote> {
        createGroupCalls += CreateGroupCall(title, description, defaultCurrencyCode, otherUserIds)
        return createGroupResult
    }

    override suspend fun addParticipantsToGroup(
        groupId: String,
        userIds: Set<String>,
    ): Result<Group, DataError.Remote> = Result.Failure(DataError.Remote.UNKNOWN)

    override suspend fun addNewParticipantsToGroup(
        groupId: String,
        usernames: List<String>,
    ): Result<Group, DataError.Remote> = Result.Failure(DataError.Remote.UNKNOWN)

    override suspend fun leaveGroup(groupId: String): EmptyResult<DataError.Remote> = Result.Success(Unit)

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
                lastActivityAt = Instant.fromEpochMilliseconds(0),
                lastTabEntry = null,
                createdAt = Instant.fromEpochMilliseconds(0),
            )
    }
}
