package de.tabmates.features.tabgroup.data.tabentry

import de.tabmates.core.domain.util.DataError
import de.tabmates.core.domain.util.EmptyResult
import de.tabmates.core.domain.util.Result
import de.tabmates.features.tabgroup.data.dto.GroupParticipantDto
import de.tabmates.features.tabgroup.data.dto.ParticipantTypeDto
import de.tabmates.features.tabgroup.data.dto.TabEntryDto
import de.tabmates.features.tabgroup.data.mappers.toEntity
import de.tabmates.features.tabgroup.data.network.dto.WebSocketMessageDto
import de.tabmates.features.tabgroup.data.network.dto.WsMessageType
import de.tabmates.features.tabgroup.data.sync.NoopLogger
import de.tabmates.features.tabgroup.data.sync.createInMemoryDatabase
import de.tabmates.features.tabgroup.data.sync.expense
import de.tabmates.features.tabgroup.data.sync.group
import de.tabmates.features.tabgroup.data.sync.insertGroup
import de.tabmates.features.tabgroup.database.TabMatesDatabase
import de.tabmates.features.tabgroup.domain.group.GroupRepository
import de.tabmates.features.tabgroup.domain.models.Group
import de.tabmates.features.tabgroup.domain.models.GroupInvitePreview
import de.tabmates.features.tabgroup.domain.models.GroupParticipant
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import kotlinx.datetime.LocalDate
import kotlinx.serialization.json.Json
import kotlin.test.Test
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.fail
import kotlin.time.Instant

/**
 * The `deletedAt` contract on the upsert path. An `ACK` carries the canonical entity, and the
 * server deliberately replays one for a write whose entry has since been soft-deleted — so this
 * path has to agree with the `TAB_ENTRY_DELETED` the client already applied.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class TabEntryRealtimeSyncTest {
    private val json = Json { ignoreUnknownKeys = true }

    @Test
    fun `an ack for a live entry applies it`() =
        syncTest { database, channel ->
            database.insertGroup(group(id = GROUP_ID))

            channel.emit(ack(deletedAt = null))

            awaitCondition("the entry should have been applied") {
                database.tabEntryDao.getTabEntryById(ENTRY_ID) != null
            }
            assertNotNull(database.tabEntryDao.getTabEntryById(ENTRY_ID))
        }

    @Test
    fun `an ack for a soft-deleted entry removes it instead of resurrecting it`() =
        syncTest { database, channel ->
            database.insertGroup(group(id = GROUP_ID))
            database.tabEntryDao.upsertTabEntry(expense(id = ENTRY_ID, groupId = GROUP_ID).toEntity())

            channel.emit(ack(deletedAt = Instant.fromEpochMilliseconds(1)))

            awaitCondition("a deleted entry must not come back") {
                database.tabEntryDao.getTabEntryById(ENTRY_ID) == null
            }
            assertNull(
                database.tabEntryDao.getTabEntryById(ENTRY_ID),
                "local queries do not filter on deletedAt, so upserting one would put it on screen",
            )
        }

    // region helpers

    private fun ack(deletedAt: Instant?): WebSocketMessageDto =
        WebSocketMessageDto(
            type = WsMessageType.ACK,
            payload = json.encodeToString(TabEntryDto.serializer(), entryDto(deletedAt)),
            requestId = "req-1",
        )

    private fun entryDto(deletedAt: Instant?): TabEntryDto {
        val participant =
            GroupParticipantDto(userId = "u1", username = "u1", userType = ParticipantTypeDto.REGISTERED)
        return TabEntryDto.Expense(
            id = ENTRY_ID,
            groupId = GROUP_ID,
            creator = participant,
            paidBy = participant,
            title = "Lunch",
            description = "",
            amount = 12.0,
            currency = "EUR",
            splits = emptyList(),
            entryDate = LocalDate(2026, 8, 1),
            createdAt = Instant.fromEpochMilliseconds(0),
            lastModifiedAt = Instant.fromEpochMilliseconds(0),
            lastModifiedBy = participant,
            version = 0,
            deletedAt = deletedAt,
            deletedBy = deletedAt?.let { participant },
        )
    }

    private suspend fun awaitCondition(
        message: String,
        condition: suspend () -> Boolean,
    ) {
        withContext(Dispatchers.Default) {
            withTimeoutOrNull(AWAIT_TIMEOUT_MS) {
                while (!condition()) delay(5)
            } ?: fail(message)
        }
    }

    private fun syncTest(body: suspend TestScope.(TabMatesDatabase, FakeWebSocketChannel) -> Unit) =
        runTest {
            val database = createInMemoryDatabase()
            val channel = FakeWebSocketChannel()
            try {
                TabEntryRealtimeSync(
                    webSocketConnector = channel,
                    database = database,
                    groupRepository = UnusedGroupRepository,
                    json = json,
                    logger = NoopLogger,
                    applicationScope =
                        CoroutineScope(
                            backgroundScope.coroutineContext + UnconfinedTestDispatcher(testScheduler),
                        ),
                )
                body(database, channel)
            } finally {
                database.close()
            }
        }

    private companion object {
        const val GROUP_ID = "g1"
        const val ENTRY_ID = "e1"
        const val AWAIT_TIMEOUT_MS = 5_000L
    }

    // endregion
}

/** Only `GROUP_METADATA_CHANGED` reaches the repository, and these tests never send one. */
private object UnusedGroupRepository : GroupRepository {
    private fun unused(): Nothing = error("GroupRepository is not exercised by these tests")

    override fun getGroups(): Flow<List<Group>> = unused()

    override fun getActiveParticipantsByGroupId(groupId: String): Flow<List<GroupParticipant>> = unused()

    override fun getAllParticipants(): Flow<List<GroupParticipant>> = unused()

    override suspend fun fetchGroupById(groupId: String): EmptyResult<DataError.Remote> = unused()

    override suspend fun createGroup(
        title: String,
        description: String?,
        defaultCurrencyCode: String,
        otherUserIds: Set<String>,
    ): Result<Group, DataError.Remote> = unused()

    override suspend fun leaveGroup(groupId: String): EmptyResult<DataError.Remote> = unused()

    override suspend fun addParticipantsToGroup(
        groupId: String,
        userIds: Set<String>,
    ): Result<Group, DataError.Remote> = unused()

    override suspend fun addNewParticipantsToGroup(
        groupId: String,
        usernames: List<String>,
    ): Result<Group, DataError.Remote> = unused()

    override suspend fun updateGroup(
        groupId: String,
        title: String,
        description: String?,
        defaultCurrencyCode: String,
    ): Result<Group, DataError.Remote> = unused()

    override suspend fun rotateInviteToken(groupId: String): Result<Group, DataError.Remote> = unused()

    override suspend fun previewInvite(token: String): Result<GroupInvitePreview, DataError.Remote> = unused()

    override suspend fun joinGroup(
        token: String,
        claimPlaceholderId: String?,
    ): Result<Group, DataError.Remote> = unused()
}
