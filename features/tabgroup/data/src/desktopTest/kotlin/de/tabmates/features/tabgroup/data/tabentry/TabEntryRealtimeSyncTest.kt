package de.tabmates.features.tabgroup.data.tabentry

import de.tabmates.core.domain.util.DataError
import de.tabmates.core.domain.util.EmptyResult
import de.tabmates.core.domain.util.Result
import de.tabmates.features.tabgroup.data.dto.GroupParticipantDto
import de.tabmates.features.tabgroup.data.dto.ParticipantTypeDto
import de.tabmates.features.tabgroup.data.dto.RecurrenceFrequencyDto
import de.tabmates.features.tabgroup.data.dto.RecurringEndDto
import de.tabmates.features.tabgroup.data.dto.RecurringEntryTypeDto
import de.tabmates.features.tabgroup.data.dto.RecurringRuleDto
import de.tabmates.features.tabgroup.data.dto.RecurringSeriesDto
import de.tabmates.features.tabgroup.data.dto.RecurringTemplateSplitDto
import de.tabmates.features.tabgroup.data.dto.TabEntryDto
import de.tabmates.features.tabgroup.data.mappers.toEntity
import de.tabmates.features.tabgroup.data.network.dto.WebSocketMessageDto
import de.tabmates.features.tabgroup.data.network.dto.WsMessageType
import de.tabmates.features.tabgroup.data.network.dto.WsSplitDto
import de.tabmates.features.tabgroup.data.sync.NoopLogger
import de.tabmates.features.tabgroup.data.sync.RecurringSeriesLocalWriter
import de.tabmates.features.tabgroup.data.sync.createInMemoryDatabase
import de.tabmates.features.tabgroup.data.sync.expense
import de.tabmates.features.tabgroup.data.sync.group
import de.tabmates.features.tabgroup.data.sync.insertGroup
import de.tabmates.features.tabgroup.database.TabMatesDatabase
import de.tabmates.features.tabgroup.database.entities.RecurringSlotClaimEntity
import de.tabmates.features.tabgroup.domain.group.GroupRemovalNotifier
import de.tabmates.features.tabgroup.domain.group.GroupRepository
import de.tabmates.features.tabgroup.domain.group.RemovedFromGroup
import de.tabmates.features.tabgroup.domain.models.Group
import de.tabmates.features.tabgroup.domain.models.GroupInvitePreview
import de.tabmates.features.tabgroup.domain.models.GroupParticipant
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import kotlinx.datetime.LocalDate
import kotlinx.serialization.json.Json
import kotlin.test.Test
import kotlin.test.assertEquals
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

    @Test
    fun `a soft-deleted generated entry keeps its slot claim`() =
        syncTest { database, channel ->
            database.insertGroup(group(id = GROUP_ID))
            database.tabEntryDao.upsertTabEntry(expense(id = ENTRY_ID, groupId = GROUP_ID).toEntity())

            channel.emit(ack(deletedAt = Instant.fromEpochMilliseconds(1), fromSeries = SERIES_ID))

            awaitCondition("the deleted entry should be gone") {
                database.tabEntryDao.getTabEntryById(ENTRY_ID) == null
            }
            // The claim is what stops the projector handing back a placeholder for an occurrence
            // somebody deleted on purpose, so removing the entry must not take it with it.
            assertEquals(
                listOf(RecurringSlotClaimEntity(SERIES_ID, OCCURRENCE_DATE.toString(), GROUP_ID)),
                database.recurringSlotClaimDao.observeClaimsForGroup(GROUP_ID).first(),
            )
        }

    @Test
    fun `a series-changed frame mirrors the schedule locally`() =
        syncTest { database, channel ->
            database.insertGroup(group(id = GROUP_ID))

            channel.emit(
                WebSocketMessageDto(
                    type = WsMessageType.RECURRING_SERIES_CHANGED,
                    payload = json.encodeToString(RecurringSeriesDto.serializer(), seriesDto()),
                ),
            )

            awaitCondition("the schedule should have been mirrored") {
                database.recurringSeriesDao.observeSeriesById(SERIES_ID).first() != null
            }
            val stored = database.recurringSeriesDao.observeSeriesById(SERIES_ID).first()
            assertNotNull(stored)
            assertEquals("Rent", stored.series.title)
        }

    @Test
    fun `being removed reports the group by its local name and then deletes it`() =
        syncTest(groupRepository = StubGroupRepository(listOf(group(id = GROUP_ID)))) { database, channel ->
            database.insertGroup(group(id = GROUP_ID))
            val removals = mutableListOf<RemovedFromGroup>()
            // Unconfined so the collector is subscribed before the frame arrives: the notifier is
            // a plain SharedFlow with no replay, exactly as the app shell sees it.
            backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
                removalNotifier.removals.collect { removals += it }
            }

            channel.emit(
                WebSocketMessageDto(
                    type = WsMessageType.REMOVED_FROM_GROUP,
                    payload = """{"groupId":"$GROUP_ID"}""",
                ),
            )

            awaitCondition("the group should have been deleted locally") {
                database.groupDao.getGroupById(GROUP_ID) == null
            }
            // The payload carries no title, so the name has to be read before the row is gone.
            assertEquals(listOf(RemovedFromGroup(groupId = GROUP_ID, title = "Group $GROUP_ID")), removals)
        }

    @Test
    fun `an unknown message type changes nothing`() =
        syncTest { database, channel ->
            database.insertGroup(group(id = GROUP_ID))

            channel.emit(WebSocketMessageDto(type = "SOMETHING_NEW", payload = "{}"))

            // Nothing to wait for; give the collector a turn and assert the group survived.
            awaitCondition("the group must still be there") {
                database.groupDao.getGroupById(GROUP_ID) != null
            }
        }

    // region helpers

    private fun ack(
        deletedAt: Instant?,
        fromSeries: String? = null,
    ): WebSocketMessageDto =
        WebSocketMessageDto(
            type = WsMessageType.ACK,
            payload = json.encodeToString(TabEntryDto.serializer(), entryDto(deletedAt, fromSeries)),
            requestId = "req-1",
        )

    private fun seriesDto(): RecurringSeriesDto {
        val participant = participantDto()
        return RecurringSeriesDto(
            id = SERIES_ID,
            groupId = GROUP_ID,
            entryType = RecurringEntryTypeDto.EXPENSE,
            isActive = true,
            needsAttention = false,
            createdAt = Instant.fromEpochMilliseconds(0),
            createdBy = participant,
            updatedAt = Instant.fromEpochMilliseconds(0),
            rule =
                RecurringRuleDto(
                    id = "rule-1",
                    title = "Rent",
                    description = "",
                    amount = 100.0,
                    currency = "EUR",
                    paidBy = participant,
                    splits =
                        listOf(
                            RecurringTemplateSplitDto(
                                participantId = participant.userId,
                                participant = participant,
                                split = WsSplitDto.Equal,
                                resolvedAmount = 100.0,
                            ),
                        ),
                    frequency = RecurrenceFrequencyDto.MONTHLY,
                    interval = 1,
                    startDate = OCCURRENCE_DATE,
                    end = RecurringEndDto.Never,
                ),
        )
    }

    private fun participantDto() =
        GroupParticipantDto(userId = "u1", username = "u1", userType = ParticipantTypeDto.REGISTERED)

    private fun entryDto(
        deletedAt: Instant?,
        fromSeries: String? = null,
    ): TabEntryDto {
        val participant = participantDto()
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
            recurringSeriesId = fromSeries,
            recurringOccurrenceDate = fromSeries?.let { OCCURRENCE_DATE },
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

    private val removalNotifier = GroupRemovalNotifier()

    private fun syncTest(
        groupRepository: GroupRepository = StubGroupRepository(),
        body: suspend TestScope.(TabMatesDatabase, FakeWebSocketChannel) -> Unit,
    ) = runTest {
        val database = createInMemoryDatabase()
        val channel = FakeWebSocketChannel()
        try {
            TabEntryRealtimeSync(
                webSocketConnector = channel,
                database = database,
                groupRepository = groupRepository,
                groupRemovalNotifier = removalNotifier,
                recurringSeriesLocalWriter = RecurringSeriesLocalWriter(database),
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
        const val SERIES_ID = "series-1"
        val OCCURRENCE_DATE = LocalDate(2026, 8, 1)
        const val AWAIT_TIMEOUT_MS = 5_000L
    }

    // endregion
}

/**
 * Answers [getGroups] from [groups] — the removal path reads the title from it before deleting the
 * group — and refuses everything else these tests do not exercise.
 */
private class StubGroupRepository(
    private val groups: List<Group> = emptyList(),
) : GroupRepository {
    private fun unused(): Nothing = error("GroupRepository is not exercised by these tests")

    override fun getGroups(): Flow<List<Group>> = flowOf(groups)

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

    override suspend fun removeParticipant(
        groupId: String,
        userId: String,
    ): EmptyResult<DataError.Remote> = unused()

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
