package de.tabmates.features.tabgroup.presentation.navigation.creategroup

import de.tabmates.core.domain.util.DataError
import de.tabmates.core.domain.util.Result
import de.tabmates.features.tabgroup.domain.group.GroupRepository
import de.tabmates.features.tabgroup.domain.models.Group
import de.tabmates.features.tabgroup.domain.models.GroupParticipant
import de.tabmates.features.tabgroup.domain.models.ParticipantType
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlin.time.Instant

class FakeGroupRepository(
    var createGroupResult: Result<Group, DataError.Remote> = Result.Success(DEFAULT_GROUP),
) : GroupRepository {
    data class CreateGroupCall(
        val title: String,
        val description: String?,
        val defaultCurrencyCode: String,
        val otherUserIds: Set<String>,
    )

    val createGroupCalls: MutableList<CreateGroupCall> = mutableListOf()
    private val groupsFlow = MutableStateFlow<List<Group>>(emptyList())

    override fun getGroups(): Flow<List<Group>> = groupsFlow

    override suspend fun fetchGroups(): Result<List<Group>, DataError.Remote> = Result.Success(groupsFlow.value)

    override suspend fun createGroup(
        title: String,
        description: String?,
        defaultCurrencyCode: String,
        otherUserIds: Set<String>,
    ): Result<Group, DataError.Remote> {
        createGroupCalls += CreateGroupCall(title, description, defaultCurrencyCode, otherUserIds)
        return createGroupResult
    }

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
