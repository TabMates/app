package de.tabmates.features.tabgroup.data.tabentry

import de.tabmates.features.tabgroup.data.mappers.toEntity
import de.tabmates.features.tabgroup.data.network.dto.WebSocketMessageDto
import de.tabmates.features.tabgroup.data.network.dto.WsErrorPayload
import de.tabmates.features.tabgroup.data.network.dto.WsMessageType
import de.tabmates.features.tabgroup.data.sync.FakePendingTabEntryBackfillStore
import de.tabmates.features.tabgroup.data.sync.FakeTabEntryService
import de.tabmates.features.tabgroup.data.sync.GroupTabEntryBackfiller
import de.tabmates.features.tabgroup.data.sync.NoopLogger
import de.tabmates.features.tabgroup.data.sync.createInMemoryDatabase
import de.tabmates.features.tabgroup.data.sync.expense
import de.tabmates.features.tabgroup.data.sync.group
import de.tabmates.features.tabgroup.data.sync.insertGroup
import de.tabmates.features.tabgroup.database.TabMatesDatabase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.delay
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
import kotlin.test.assertTrue
import kotlin.test.fail

/**
 * The outbox's half of the acknowledgement protocol: a row is durable until the server says the
 * write landed, and a write the server refuses outright is undone rather than left on screen.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class TabEntryOutboxTest {
    private val json = Json { ignoreUnknownKeys = true }

    @Test
    fun `a sent write keeps its row until the ack arrives`() =
        outboxTest { database ->
            val channel = FakeWebSocketChannel()
            val outbox = outbox(database, channel)

            outbox.enqueueCreateExpense()
            channel.connect()
            channel.awaitSent(1)

            val row = database.pendingOutboxDao.getAll().single()
            assertNotNull(row.requestId, "a dispatched write must carry a correlation id")

            channel.emitAck(row.requestId!!)

            database.awaitRowCount(0)
        }

    @Test
    fun `a reconnect re-sends a write that was never acked, reusing its requestId`() =
        outboxTest { database ->
            val channel = FakeWebSocketChannel()
            outbox(database, channel).enqueueCreateExpense()

            channel.connect()
            channel.awaitSent(1)
            val requestId =
                database.pendingOutboxDao
                    .getAll()
                    .single()
                    .requestId
            channel.disconnect()
            channel.connect()
            channel.awaitSent(2)

            assertEquals(
                listOf(requestId, requestId),
                channel.sent.map { json.decodeFromString(WebSocketMessageDto.serializer(), it).requestId },
                "a retry must reuse the id so the server replays instead of writing twice",
            )
        }

    @Test
    fun `an ack for an unknown request changes nothing`() =
        outboxTest { database ->
            val channel = FakeWebSocketChannel()
            outbox(database, channel).enqueueCreateExpense()
            channel.connect()
            channel.awaitSent(1)

            channel.emitAck("some-other-request")

            database.awaitRowCount(1)
        }

    @Test
    fun `an uncorrelated error leaves every pending write alone`() =
        outboxTest { database ->
            val channel = FakeWebSocketChannel()
            outbox(database, channel).enqueueCreateExpense()
            channel.connect()
            channel.awaitSent(1)

            // What the server sends for RATE_LIMITED and MESSAGE_TOO_LARGE: backpressure on the
            // session, not a verdict on any one write.
            channel.emitError(requestId = null, code = "RATE_LIMITED", retryable = true)

            database.awaitRowCount(1)
        }

    @Test
    fun `a retryable error keeps the row and spends an attempt`() =
        outboxTest { database ->
            val channel = FakeWebSocketChannel()
            outbox(database, channel).enqueueCreateExpense()
            channel.connect()
            channel.awaitSent(1)
            val requestId =
                database.pendingOutboxDao
                    .getAll()
                    .single()
                    .requestId!!

            channel.emitError(requestId, code = "INTERNAL_ERROR", retryable = true)
            awaitCondition("the attempt should have been recorded") {
                database.pendingOutboxDao
                    .getAll()
                    .single()
                    .attemptCount == 1
            }

            val row = database.pendingOutboxDao.getAll().single()
            assertEquals("ws_internal_error_attempt_1", row.lastError)
        }

    @Test
    fun `a non-retryable error on a create deletes the row and the local entry`() =
        outboxTest { database ->
            database.insertGroup(group(id = GROUP_ID))
            database.tabEntryDao.upsertTabEntry(
                expense(id = ENTRY_ID, groupId = GROUP_ID).toEntity(),
            )
            val channel = FakeWebSocketChannel()
            outbox(database, channel).enqueueCreateExpense()
            channel.connect()
            channel.awaitSent(1)
            val requestId =
                database.pendingOutboxDao
                    .getAll()
                    .single()
                    .requestId!!

            channel.emitError(requestId, code = "GROUP_ACCESS_DENIED", retryable = false)
            database.awaitRowCount(0)

            assertNull(
                database.tabEntryDao.getTabEntryById(ENTRY_ID),
                "a create the server refused never existed there, so it must not linger here",
            )
        }

    @Test
    fun `a non-retryable error on an update refetches the group`() =
        outboxTest { database ->
            database.insertGroup(group(id = GROUP_ID))
            database.tabEntryDao.upsertTabEntry(expense(id = ENTRY_ID, groupId = GROUP_ID).toEntity())
            val channel = FakeWebSocketChannel()
            val service = FakeTabEntryService()
            outbox(database, channel, service).enqueueUpdateExpense()
            channel.connect()
            channel.awaitSent(1)
            val requestId =
                database.pendingOutboxDao
                    .getAll()
                    .single()
                    .requestId!!

            channel.emitError(requestId, code = "INVALID_TAB_ENTRY", retryable = false)
            database.awaitRowCount(0)

            assertEquals(
                listOf(GROUP_ID),
                service.receivedGroupIds,
                "the server's version of the entry is the truth, so the group is re-read",
            )
            assertNotNull(
                database.tabEntryDao.getTabEntryById(ENTRY_ID),
                "a rejected edit reverts the entry, it does not remove it",
            )
        }

    @Test
    fun `a non-retryable TAB_ENTRY_NOT_FOUND on an update deletes the local entry`() =
        outboxTest { database ->
            database.insertGroup(group(id = GROUP_ID))
            database.tabEntryDao.upsertTabEntry(expense(id = ENTRY_ID, groupId = GROUP_ID).toEntity())
            val channel = FakeWebSocketChannel()
            val service = FakeTabEntryService()
            outbox(database, channel, service).enqueueUpdateExpense()
            channel.connect()
            channel.awaitSent(1)
            val requestId =
                database.pendingOutboxDao
                    .getAll()
                    .single()
                    .requestId!!

            channel.emitError(requestId, code = "TAB_ENTRY_NOT_FOUND", retryable = false)
            database.awaitRowCount(0)

            assertNull(
                database.tabEntryDao.getTabEntryById(ENTRY_ID),
                "a backfill cannot remove what the server no longer reports, so this one is deleted",
            )
            assertTrue(service.receivedGroupIds.isEmpty())
        }

    @Test
    fun `re-editing an entry replaces its payload and mints a new requestId`() =
        outboxTest { database ->
            val channel = FakeWebSocketChannel()
            val outbox = outbox(database, channel)

            outbox.enqueueUpdateExpense(title = "first")
            val first = database.pendingOutboxDao.getAll().single()
            outbox.enqueueUpdateExpense(title = "second")
            val second = database.pendingOutboxDao.getAll().single()

            assertEquals(first.id, second.id, "an edit reuses the row")
            assertTrue(
                first.requestId != second.requestId,
                "reusing the id would have the server replay the first edit's ack and drop this one",
            )
            assertTrue(second.payload.contains("second"))
        }

    @Test
    fun `a delete waits for the ack of the write it deletes`() =
        outboxTest { database ->
            val channel = FakeWebSocketChannel()
            val service = FakeTabEntryService()
            val outbox = outbox(database, channel, service)

            outbox.enqueueCreateExpense()
            channel.connect()
            channel.awaitSent(1)
            val requestId =
                database.pendingOutboxDao
                    .getAll()
                    .single()
                    .requestId!!

            outbox.enqueueDeleteTabEntry(tabEntryId = ENTRY_ID, groupId = GROUP_ID)
            database.awaitRowCount(2)
            assertTrue(
                service.deletedIds.isEmpty(),
                "a DELETE that overtook the create would 404, retire itself, and let the create " +
                    "commit afterwards — leaving the entry alive on the server",
            )

            channel.emitAck(requestId)

            awaitCondition("the delete should run once the create is acked") {
                service.deletedIds == listOf(ENTRY_ID)
            }
            database.awaitRowCount(0)
        }

    // region helpers

    /** Runs [body] against a fresh in-memory database, closed even when the test fails. */
    private fun outboxTest(body: suspend TestScope.(TabMatesDatabase) -> Unit) =
        runTest {
            val database = createInMemoryDatabase()
            try {
                body(database)
            } finally {
                database.close()
            }
        }

    /**
     * Waits for work the outbox does off the test's own coroutine.
     *
     * Room's suspend DAO calls run on the database's internal dispatcher, so a drain kicked off by
     * a connection change or an inbound frame is not finished by the time the triggering call
     * returns — and no amount of virtual-time advancing helps, because the scheduler does not own
     * that thread. Polling in real time is what actually observes the result.
     */
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

    private suspend fun TabMatesDatabase.awaitRowCount(count: Int) =
        awaitCondition("expected $count pending row(s)") {
            pendingOutboxDao.getAll().size == count
        }

    private suspend fun FakeWebSocketChannel.awaitSent(count: Int) =
        awaitCondition("expected $count dispatched write(s), saw ${sent.size}") { sent.size == count }

    private fun TestScope.outbox(
        database: TabMatesDatabase,
        channel: FakeWebSocketChannel,
        service: FakeTabEntryService = FakeTabEntryService(),
    ): TabEntryOutbox {
        // Parented to backgroundScope so runTest cancels the outbox's two permanent collectors at
        // the end of the test; a standalone scope would keep them — and the fake channel and Room
        // database they hold — alive for the rest of the suite. Unconfined so an enqueue's drain
        // runs eagerly rather than waiting on the scheduler.
        val scope = CoroutineScope(backgroundScope.coroutineContext + UnconfinedTestDispatcher(testScheduler))
        return TabEntryOutbox(
            database = database,
            webSocketConnector = channel,
            service = service,
            backfiller =
                GroupTabEntryBackfiller(
                    tabEntryService = service,
                    database = database,
                    pendingBackfillStore = FakePendingTabEntryBackfillStore(),
                    logger = NoopLogger,
                ),
            staleSessionStore = FakeStaleSessionStore(),
            json = json,
            logger = NoopLogger,
            applicationScope = scope,
        )
    }

    private suspend fun TabEntryOutbox.enqueueCreateExpense() =
        enqueueCreateExpense(
            clientRequestId = ENTRY_ID,
            groupId = GROUP_ID,
            title = "Lunch",
            description = "",
            amount = 12.0,
            currencyCode = "EUR",
            exchangeRate = null,
            paidByUserId = "u1",
            entryDate = LocalDate(2026, 8, 1),
            splits = emptyList(),
            expectedVersion = 0,
        )

    private suspend fun TabEntryOutbox.enqueueUpdateExpense(title: String = "Lunch") =
        enqueueUpdateExpense(
            tabEntryId = ENTRY_ID,
            groupId = GROUP_ID,
            title = title,
            description = "",
            amount = 12.0,
            currencyCode = "EUR",
            exchangeRate = null,
            paidByUserId = "u1",
            entryDate = LocalDate(2026, 8, 1),
            splits = emptyList(),
            expectedVersion = 1,
        )

    private fun FakeWebSocketChannel.emitAck(requestId: String) =
        emit(WebSocketMessageDto(type = WsMessageType.ACK, payload = "{}", requestId = requestId))

    private fun FakeWebSocketChannel.emitError(
        requestId: String?,
        code: String,
        retryable: Boolean,
    ) = emit(
        WebSocketMessageDto(
            type = WsMessageType.ERROR,
            payload =
                json.encodeToString(
                    WsErrorPayload.serializer(),
                    WsErrorPayload(code = code, message = code, retryable = retryable),
                ),
            requestId = requestId,
        ),
    )

    private companion object {
        const val GROUP_ID = "g1"
        const val ENTRY_ID = "e1"
        const val AWAIT_TIMEOUT_MS = 5_000L
    }

    // endregion
}
