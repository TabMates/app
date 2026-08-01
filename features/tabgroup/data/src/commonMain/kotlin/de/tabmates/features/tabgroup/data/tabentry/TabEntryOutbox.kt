package de.tabmates.features.tabgroup.data.tabentry

import de.tabmates.core.data.di.APPLICATION_SCOPE
import de.tabmates.core.domain.auth.StaleSessionStore
import de.tabmates.core.domain.logging.TabMatesLogger
import de.tabmates.core.domain.util.DataError
import de.tabmates.core.domain.util.Result
import de.tabmates.features.tabgroup.data.mappers.toWsSplit
import de.tabmates.features.tabgroup.data.network.ConnectionState
import de.tabmates.features.tabgroup.data.network.WebSocketChannel
import de.tabmates.features.tabgroup.data.network.dto.NewTabEntrySplitWsPayload
import de.tabmates.features.tabgroup.data.network.dto.NewTabEntryWsPayload
import de.tabmates.features.tabgroup.data.network.dto.WebSocketMessageDto
import de.tabmates.features.tabgroup.data.network.dto.WsErrorPayload
import de.tabmates.features.tabgroup.data.network.dto.WsMessageType
import de.tabmates.features.tabgroup.data.sync.GroupTabEntryBackfiller
import de.tabmates.features.tabgroup.data.tabentry.TabEntryOutbox.Companion.ACK_TIMEOUT_MS
import de.tabmates.features.tabgroup.data.tabentry.TabEntryOutbox.Companion.MAX_ATTEMPTS
import de.tabmates.features.tabgroup.database.TabMatesDatabase
import de.tabmates.features.tabgroup.database.entities.PendingOutboxEntity
import de.tabmates.features.tabgroup.domain.tabentry.NewTabEntrySplit
import de.tabmates.features.tabgroup.domain.tabentry.SplitResolver
import de.tabmates.features.tabgroup.domain.tabentry.TabEntryService
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.datetime.LocalDate
import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.Json
import org.koin.core.annotation.Named
import org.koin.core.annotation.Single
import kotlin.time.Clock
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

/**
 * Durable outbox for tab-entry writes. The repository enqueues every write here; the outbox tries
 * to dispatch it when the WS is CONNECTED.
 *
 * **A row is deleted only when the server acknowledges it**, never when the frame is handed to the
 * socket. A write sitting in a send buffer on a connection that dies before the server commits is
 * not a write that happened, and deleting the row on send is how those vanished without a trace.
 * Sending moves the row to [inFlight] instead; the `ACK` that names its
 * [PendingOutboxEntity.requestId] is what removes it.
 *
 * Transient dispatch failures (offline, server 5xx) leave the row untouched for the next CONNECTED
 * tick. Only PERMANENT failures count toward [MAX_ATTEMPTS]; once reached, the row is parked and
 * skipped on subsequent drains. A write the server rejects as non-retryable is rolled back — the
 * row goes, and so does the optimistic local entry it was going to produce.
 */
@Single(createdAtStart = true)
class TabEntryOutbox(
    private val database: TabMatesDatabase,
    private val webSocketConnector: WebSocketChannel,
    private val service: TabEntryService,
    private val backfiller: GroupTabEntryBackfiller,
    private val staleSessionStore: StaleSessionStore,
    private val json: Json,
    private val logger: TabMatesLogger,
    @Named(APPLICATION_SCOPE) private val applicationScope: CoroutineScope,
) {
    private val mutex = Mutex()

    /**
     * Per-session retry counter. Resets every app launch so the user can recover stuck rows
     * (e.g. a transient server bug that masqueraded as permanent) simply by reopening the app.
     * Always accessed under [mutex]; the DB [PendingOutboxEntity.attemptCount] is kept for
     * forensics only and never consulted for parking decisions.
     */
    private val sessionAttempts = mutableMapOf<String, Int>()

    /**
     * Writes sent on the current connection and still waiting for their verdict, keyed by
     * [PendingOutboxEntity.requestId]. Purely an optimisation and a re-send guard — the rows
     * themselves are durable, so anything lost here (a process death, a dropped socket) is
     * recovered by the next drain. Always accessed under [mutex].
     */
    private val inFlight = mutableMapOf<String, Long>()

    init {
        webSocketConnector
            .connectionState
            .onEach { state ->
                if (state == ConnectionState.CONNECTED) {
                    drain()
                } else {
                    // Nothing sent on a socket that is gone can still be acknowledged on it. Clear
                    // the claims so the next CONNECTED edge re-sends those rows; the server answers
                    // a repeat of a requestId it already applied from its replay cache.
                    mutex.withLock { inFlight.clear() }
                }
            }.launchIn(applicationScope)

        webSocketConnector
            .messages
            .onEach { message -> onServerFrame(message) }
            .launchIn(applicationScope)
    }

    suspend fun enqueueCreateExpense(
        clientRequestId: String,
        groupId: String,
        title: String,
        description: String,
        amount: Double,
        currencyCode: String,
        exchangeRate: Double?,
        paidByUserId: String,
        entryDate: LocalDate,
        splits: List<NewTabEntrySplit>,
        expectedVersion: Int?,
    ) {
        logger.debug(TAG, "Outbox enqueue create id=$clientRequestId")
        val payload =
            NewTabEntryWsPayload.Expense(
                id = clientRequestId,
                groupId = groupId,
                paidByUserId = paidByUserId,
                title = title,
                description = description,
                amount = amount,
                currency = currencyCode,
                exchangeRate = exchangeRate,
                entryDate = entryDate,
                splits = buildSplitPayloads(splits, amount),
            )
        persistWrite(
            rowId = clientRequestId,
            outboxType = OUTBOX_TYPE_NEW_TAB_ENTRY,
            messageType = WsMessageType.NEW_TAB_ENTRY,
            payload = payload,
            expectedVersion = expectedVersion,
        )
    }

    suspend fun enqueueCreateSettlement(
        clientRequestId: String,
        groupId: String,
        title: String,
        description: String,
        amount: Double,
        currencyCode: String,
        exchangeRate: Double?,
        paidByUserId: String,
        receivedByUserId: String,
        entryDate: LocalDate,
        expectedVersion: Int?,
    ) {
        val payload =
            NewTabEntryWsPayload.Settlement(
                id = clientRequestId,
                groupId = groupId,
                paidByUserId = paidByUserId,
                title = title,
                description = description,
                amount = amount,
                currency = currencyCode,
                exchangeRate = exchangeRate,
                entryDate = entryDate,
                receivedByUserId = receivedByUserId,
            )
        persistWrite(
            rowId = clientRequestId,
            outboxType = OUTBOX_TYPE_NEW_TAB_ENTRY,
            messageType = WsMessageType.NEW_TAB_ENTRY,
            payload = payload,
            expectedVersion = expectedVersion,
        )
    }

    suspend fun enqueueUpdateExpense(
        tabEntryId: String,
        groupId: String,
        title: String,
        description: String,
        amount: Double,
        currencyCode: String,
        exchangeRate: Double?,
        paidByUserId: String,
        entryDate: LocalDate,
        splits: List<NewTabEntrySplit>,
        expectedVersion: Int?,
    ) {
        val payload =
            NewTabEntryWsPayload.Expense(
                id = tabEntryId,
                groupId = groupId,
                paidByUserId = paidByUserId,
                title = title,
                description = description,
                amount = amount,
                currency = currencyCode,
                exchangeRate = exchangeRate,
                entryDate = entryDate,
                splits = buildSplitPayloads(splits, amount),
            )
        persistWrite(
            rowId = "$UPDATE_ID_PREFIX$tabEntryId",
            outboxType = OUTBOX_TYPE_TAB_ENTRY_UPDATE,
            messageType = WsMessageType.UPDATED_TAB_ENTRY,
            payload = payload,
            expectedVersion = expectedVersion,
        )
    }

    suspend fun enqueueCreateIncome(
        clientRequestId: String,
        groupId: String,
        title: String,
        description: String,
        amount: Double,
        currencyCode: String,
        exchangeRate: Double?,
        paidByUserId: String,
        entryDate: LocalDate,
        splits: List<NewTabEntrySplit>,
        expectedVersion: Int?,
    ) {
        logger.debug(TAG, "Outbox enqueue create income id=$clientRequestId")
        val payload =
            NewTabEntryWsPayload.Income(
                id = clientRequestId,
                groupId = groupId,
                paidByUserId = paidByUserId,
                title = title,
                description = description,
                amount = amount,
                currency = currencyCode,
                exchangeRate = exchangeRate,
                entryDate = entryDate,
                splits = buildSplitPayloads(splits, amount),
            )
        persistWrite(
            rowId = clientRequestId,
            outboxType = OUTBOX_TYPE_NEW_TAB_ENTRY,
            messageType = WsMessageType.NEW_TAB_ENTRY,
            payload = payload,
            expectedVersion = expectedVersion,
        )
    }

    suspend fun enqueueUpdateIncome(
        tabEntryId: String,
        groupId: String,
        title: String,
        description: String,
        amount: Double,
        currencyCode: String,
        exchangeRate: Double?,
        paidByUserId: String,
        entryDate: LocalDate,
        splits: List<NewTabEntrySplit>,
        expectedVersion: Int?,
    ) {
        val payload =
            NewTabEntryWsPayload.Income(
                id = tabEntryId,
                groupId = groupId,
                paidByUserId = paidByUserId,
                title = title,
                description = description,
                amount = amount,
                currency = currencyCode,
                exchangeRate = exchangeRate,
                entryDate = entryDate,
                splits = buildSplitPayloads(splits, amount),
            )
        persistWrite(
            rowId = "$UPDATE_ID_PREFIX$tabEntryId",
            outboxType = OUTBOX_TYPE_TAB_ENTRY_UPDATE,
            messageType = WsMessageType.UPDATED_TAB_ENTRY,
            payload = payload,
            expectedVersion = expectedVersion,
        )
    }

    suspend fun enqueueUpdateSettlement(
        tabEntryId: String,
        groupId: String,
        title: String,
        description: String,
        amount: Double,
        currencyCode: String,
        exchangeRate: Double?,
        paidByUserId: String,
        receivedByUserId: String,
        entryDate: LocalDate,
        expectedVersion: Int?,
    ) {
        val payload =
            NewTabEntryWsPayload.Settlement(
                id = tabEntryId,
                groupId = groupId,
                paidByUserId = paidByUserId,
                title = title,
                description = description,
                amount = amount,
                currency = currencyCode,
                exchangeRate = exchangeRate,
                entryDate = entryDate,
                receivedByUserId = receivedByUserId,
            )
        persistWrite(
            rowId = "$UPDATE_ID_PREFIX$tabEntryId",
            outboxType = OUTBOX_TYPE_TAB_ENTRY_UPDATE,
            messageType = WsMessageType.UPDATED_TAB_ENTRY,
            payload = payload,
            expectedVersion = expectedVersion,
        )
    }

    /**
     * Persists one WebSocket write and kicks a drain.
     *
     * The [PendingOutboxEntity.requestId] is minted here, fresh on every call, and stored both in
     * the envelope the server will read and in its own column so an incoming ack can be matched
     * back to this row. Regenerating it is the point: an update row is keyed on the entry id and
     * upserted in place, so a second edit that reused the first edit's id would be answered from
     * the server's replay cache and never applied.
     */
    @OptIn(ExperimentalUuidApi::class)
    private suspend fun persistWrite(
        rowId: String,
        outboxType: String,
        messageType: String,
        payload: NewTabEntryWsPayload,
        expectedVersion: Int?,
    ) {
        val requestId = Uuid.random().toString()
        val envelope =
            WebSocketMessageDto(
                type = messageType,
                payload = json.encodeToString(NewTabEntryWsPayload.serializer(), payload),
                requestId = requestId,
            )
        database.pendingOutboxDao.upsert(
            PendingOutboxEntity(
                id = rowId,
                type = outboxType,
                payload = json.encodeToString(WebSocketMessageDto.serializer(), envelope),
                createdAt = Clock.System.now().toEpochMilliseconds(),
                expectedVersion = expectedVersion,
                requestId = requestId,
            ),
        )
        applicationScope.launch { drain() }
    }

    /**
     * Cancels a not-yet-dispatched create for [tabEntryId] (and any queued update for it).
     * Returns `true` when such a row was present, meaning the entry never reached the server, so
     * the caller can skip enqueuing a remote delete that would only 404 — and, worse, could race
     * the create's own echo back onto the server.
     *
     * Runs under [mutex] so it is atomic against [drain]. **Not dispatched** is the condition, not
     * merely "has a row": since a row now survives until the server acknowledges it, a create that
     * is already in flight may well be committed by the time this is called. Cancelling that one
     * locally would strand it on the server, so it is left alone and the caller falls back to a
     * remote delete, which runs after the create it is deleting.
     */
    suspend fun cancelPendingCreate(tabEntryId: String): Boolean =
        mutex.withLock {
            val pending = database.pendingOutboxDao.getAll()
            val createRow =
                pending.firstOrNull { it.id == tabEntryId && it.type == OUTBOX_TYPE_NEW_TAB_ENTRY }
                    ?: return@withLock false
            if (createRow.requestId?.let { it in inFlight } == true) {
                logger.debug(TAG, "Create $tabEntryId is in flight; deleting it remotely instead")
                return@withLock false
            }
            database.pendingOutboxDao.deleteById(createRow.id)
            sessionAttempts.remove(createRow.id)
            val updateId = "$UPDATE_ID_PREFIX$tabEntryId"
            pending.firstOrNull { it.id == updateId }?.let { updateRow ->
                database.pendingOutboxDao.deleteById(updateId)
                sessionAttempts.remove(updateId)
                updateRow.requestId?.let(inFlight::remove)
            }
            true
        }

    /**
     * Queues a remote delete, carrying a snapshot of what is being deleted.
     *
     * The caller must read the snapshot *before* wiping the local row: nothing else survives the
     * delete, so without it the activity feed's pending row has no title or amount to show.
     */
    suspend fun enqueueDeleteTabEntry(
        tabEntryId: String,
        groupId: String? = null,
        title: String? = null,
        amount: Double? = null,
        currencyCode: String? = null,
        entryType: String? = null,
    ) {
        val payload =
            PendingDeletePayload(
                tabEntryId = tabEntryId,
                groupId = groupId,
                title = title,
                amount = amount,
                currencyCode = currencyCode,
                entryType = entryType,
            )
        database.pendingOutboxDao.upsert(
            PendingOutboxEntity(
                id = "$DELETE_ID_PREFIX$tabEntryId",
                type = OUTBOX_TYPE_TAB_ENTRY_DELETE,
                payload = json.encodeToString(PendingDeletePayload.serializer(), payload),
                createdAt = Clock.System.now().toEpochMilliseconds(),
            ),
        )
        applicationScope.launch { drain() }
    }

    private fun buildSplitPayloads(
        splits: List<NewTabEntrySplit>,
        amount: Double,
    ): List<NewTabEntrySplitWsPayload> {
        if (splits.isEmpty()) return emptyList()
        val resolved = SplitResolver.resolveAmounts(splits, amount)
        return splits.mapIndexed { index, split ->
            NewTabEntrySplitWsPayload(
                id = null,
                participantId = split.participantId,
                split = toWsSplit(split.splitType, split.value),
                resolvedAmount = resolved[index],
            )
        }
    }

    private suspend fun drain() {
        // An expired session is in re-auth limbo, and re-auth has to sign in before it can compare
        // the account id — so for that moment the app holds a *different* account's token, and the
        // socket opens off exactly that. Dispatching here would send this account's queued writes
        // as someone else, and the server would answer with a rejection this outbox then rolls
        // back — destroying them rather than holding them. Drains resume the moment the matching
        // account signs back in and the record is cleared.
        if (staleSessionStore.get() != null) {
            logger.debug(TAG, "Outbox drain skipped: session expired, awaiting re-auth")
            return
        }

        // No connection → nothing to attempt. Drain will re-run on next CONNECTED edge.
        if (webSocketConnector.connectionState.value != ConnectionState.CONNECTED) {
            logger.debug(
                TAG,
                "Outbox drain skipped: not connected (state=${webSocketConnector.connectionState.value})",
            )
            return
        }

        mutex.withLock {
            val now = Clock.System.now().toEpochMilliseconds()
            val pending = database.pendingOutboxDao.getAll()
            for (item in pending) {
                val sessionCount = sessionAttempts[item.id] ?: 0
                if (sessionCount >= MAX_ATTEMPTS) {
                    // Parked for this session — will retry on next app launch.
                    continue
                }
                if (isAwaitingAck(item, now)) {
                    continue
                }
                if (isBlockedByEarlierWrite(item, pending)) {
                    continue
                }
                when (val result = dispatch(item)) {
                    is DispatchResult.Sent -> {
                        logger.debug(TAG, "Outbox sent id=${item.id}; awaiting ack ${result.requestId}")
                        inFlight[result.requestId] = now
                        // Nothing else would notice a verdict that never comes on a socket that
                        // stays up: drains are otherwise only triggered by an enqueue or a
                        // connection edge. This wakes up once the claim is old enough to re-send.
                        applicationScope.launch {
                            delay(ACK_TIMEOUT_MS)
                            drain()
                        }
                    }

                    is DispatchResult.Success -> {
                        logger.debug(TAG, "Outbox completed id=${item.id}; deleting row")
                        database.pendingOutboxDao.deleteById(item.id)
                        sessionAttempts.remove(item.id)
                    }

                    is DispatchResult.Transient -> {
                        // Don't burn attempt budget on retriable failures (offline, 5xx, timeouts).
                        logger.debug(
                            TAG,
                            "Transient dispatch failure id=${item.id} reason=${result.reason}; leaving for retry",
                        )
                    }

                    is DispatchResult.Permanent -> {
                        val nextCount = sessionCount + 1
                        sessionAttempts[item.id] = nextCount
                        val tag =
                            if (nextCount >= MAX_ATTEMPTS) {
                                logger.error(
                                    TAG,
                                    "Outbox entry id=${item.id} exhausted retries ($nextCount/$MAX_ATTEMPTS) this session; parking until restart. last=${result.reason}",
                                )
                                "permanent_${result.reason}_parked_session"
                            } else {
                                "permanent_${result.reason}_attempt_$nextCount"
                            }
                        recordAttempt(item, lastError = tag)
                    }
                }
            }
        }
    }

    private suspend fun dispatch(item: PendingOutboxEntity): DispatchResult =
        try {
            when (item.type) {
                OUTBOX_TYPE_NEW_TAB_ENTRY,
                OUTBOX_TYPE_TAB_ENTRY_UPDATE,
                -> {
                    dispatchNewTabEntry(withRequestId(item))
                }

                OUTBOX_TYPE_TAB_ENTRY_DELETE -> {
                    dispatchDelete(json.decodePendingDeletePayload(item.payload).tabEntryId)
                }

                else -> {
                    logger.warning(TAG, "Dropping unknown outbox entry type=${item.type} id=${item.id}")
                    DispatchResult.Success
                }
            }
        } catch (e: Throwable) {
            logger.error(TAG, "Outbox dispatch crashed for id=${item.id}", e)
            DispatchResult.Permanent("dispatch_crash")
        }

    /**
     * [item] with a durable [PendingOutboxEntity.requestId], minting one if the row predates the
     * column.
     *
     * It has to be persisted rather than generated per attempt: the server treats a new id as a new
     * request, so a retry carrying a fresh one would apply the write a second time instead of being
     * answered from the replay cache.
     */
    @OptIn(ExperimentalUuidApi::class)
    private suspend fun withRequestId(item: PendingOutboxEntity): PendingOutboxEntity {
        if (item.requestId != null) return item

        val requestId = Uuid.random().toString()
        val envelope = json.decodeFromString(WebSocketMessageDto.serializer(), item.payload)
        val upgraded =
            item.copy(
                payload =
                    json.encodeToString(
                        WebSocketMessageDto.serializer(),
                        envelope.copy(requestId = requestId),
                    ),
                requestId = requestId,
            )
        database.pendingOutboxDao.upsert(upgraded)
        return upgraded
    }

    /**
     * Whether [item] was already sent on this connection and its verdict is still outstanding.
     *
     * The [ACK_TIMEOUT_MS] escape hatch covers the one case the connection-state listener misses: a
     * live socket where the ack itself was dropped. Re-sending is safe — the server answers a
     * repeated `requestId` from its replay cache rather than writing again.
     */
    private fun isAwaitingAck(
        item: PendingOutboxEntity,
        now: Long,
    ): Boolean {
        val sentAt = item.requestId?.let { inFlight[it] } ?: return false
        if (now - sentAt < ACK_TIMEOUT_MS) return true

        logger.warning(TAG, "No ack for id=${item.id} within ${ACK_TIMEOUT_MS}ms; re-sending")
        inFlight.remove(item.requestId)
        return false
    }

    /**
     * Whether [item] is a delete that must wait for an earlier write to the same entry.
     *
     * Deletes go over HTTP while creates and updates go over the socket, so the two have no shared
     * ordering. A `DELETE` that overtakes an unacknowledged create gets a 404, which
     * [dispatchDelete] treats as success and retires the row — and the create then commits and
     * leaves the entry alive on the server with nothing left to remove it. Holding the delete until
     * the write it deletes is acknowledged is what keeps the two transports in order; [onAck]
     * re-drains, so the wait ends as soon as the ack lands.
     */
    private fun isBlockedByEarlierWrite(
        item: PendingOutboxEntity,
        pending: List<PendingOutboxEntity>,
    ): Boolean {
        if (item.type != OUTBOX_TYPE_TAB_ENTRY_DELETE) return false

        val tabEntryId = item.id.removePrefix(DELETE_ID_PREFIX)
        val blocked =
            pending.any {
                (it.id == tabEntryId && it.type == OUTBOX_TYPE_NEW_TAB_ENTRY) ||
                    (it.id == "$UPDATE_ID_PREFIX$tabEntryId" && it.type == OUTBOX_TYPE_TAB_ENTRY_UPDATE)
            }
        if (blocked) {
            logger.debug(TAG, "Holding delete of $tabEntryId until its pending write is acked")
        }
        return blocked
    }

    private suspend fun dispatchNewTabEntry(item: PendingOutboxEntity): DispatchResult =
        when (val result = webSocketConnector.sendMessage(item.payload)) {
            is Result.Success -> {
                // Sent, not done: the row stays until the server acknowledges it by requestId.
                DispatchResult.Sent(requireNotNull(item.requestId))
            }

            is Result.Failure -> {
                // Lost the socket between drain gate and send — retry on next CONNECTED tick.
                when (result.error) {
                    DataError.Connection.NOT_CONNECTED -> DispatchResult.Transient("not_connected")
                    DataError.Connection.MESSAGE_SEND_FAILED -> DispatchResult.Transient("send_failed")
                }
            }
        }

    private suspend fun dispatchDelete(tabEntryId: String): DispatchResult =
        when (val result = service.deleteTabEntry(tabEntryId)) {
            is Result.Success -> {
                DispatchResult.Success
            }

            is Result.Failure -> {
                when (result.error) {
                    DataError.Remote.NO_INTERNET,
                    DataError.Remote.REQUEST_TIMEOUT,
                    DataError.Remote.SERVER_ERROR,
                    DataError.Remote.SERVICE_UNAVAILABLE,
                    DataError.Remote.TOO_MANY_REQUESTS,
                    DataError.Remote.UNKNOWN,
                    // 426 never clears for *this* build, but it does clear once the user updates —
                    // and the update prompt is already on screen by then. Keeping it pending is the
                    // difference between the delete landing after the update and being lost.
                    DataError.Remote.UPGRADE_REQUIRED,
                    -> DispatchResult.Transient(result.error.name.lowercase())

                    // 404 on delete: entry is already gone on the server, treat as success.
                    DataError.Remote.NOT_FOUND -> DispatchResult.Success

                    DataError.Remote.BAD_REQUEST,
                    DataError.Remote.UNAUTHORIZED,
                    DataError.Remote.FORBIDDEN,
                    DataError.Remote.CONFLICT,
                    DataError.Remote.PAYLOAD_TOO_LARGE,
                    DataError.Remote.SERIALIZATION,
                    // Turnstile only gates the auth endpoints, never this delete; classify as
                    // permanent for exhaustiveness (it would never clear on retry anyway).
                    DataError.Remote.TURNSTILE_FAILED,
                    -> DispatchResult.Permanent(result.error.name.lowercase())
                }
            }
        }

    /**
     * Applies the server's verdict on one write.
     *
     * Only `ACK` and `ERROR` are of interest, and only when they name a request: the errors the
     * server raises before it has parsed an envelope (`MESSAGE_TOO_LARGE`, `RATE_LIMITED`, an
     * unusable id) carry no `requestId` because they are not a verdict on any one write. Those are
     * logged and change nothing, which leaves every queued row pending — the right outcome for
     * backpressure.
     */
    private suspend fun onServerFrame(message: WebSocketMessageDto) {
        try {
            when (message.type) {
                WsMessageType.ACK -> {
                    message.requestId?.let { onAck(it) }
                }

                WsMessageType.ERROR -> {
                    val error = json.decodeFromString(WsErrorPayload.serializer(), message.payload)
                    val requestId = message.requestId
                    if (requestId == null) {
                        logger.warning(
                            TAG,
                            "Uncorrelated WS error code=${error.code} retryable=${error.retryable}; keeping every pending write",
                        )
                    } else {
                        onError(requestId, error)
                    }
                }

                else -> {}
            }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Throwable) {
            logger.error(TAG, "Failed to apply server verdict for requestId=${message.requestId}", e)
        }
    }

    private suspend fun onAck(requestId: String) {
        mutex.withLock {
            val row = resolveRow(requestId) ?: return
            logger.debug(TAG, "Ack for id=${row.id}; the write is durable server-side, deleting row")
            database.pendingOutboxDao.deleteById(row.id)
            sessionAttempts.remove(row.id)
        }
        // Retiring a row can unblock one that was waiting on it — a queued delete held behind the
        // write it deletes. Nothing else would re-drain until the next enqueue or reconnect.
        applicationScope.launch { drain() }
    }

    private suspend fun onError(
        requestId: String,
        error: WsErrorPayload,
    ) {
        val rejected =
            mutex.withLock {
                val row = resolveRow(requestId) ?: return

                if (error.retryable) {
                    // The write may still land. Keep the row, but spend an attempt so one that
                    // keeps failing parks instead of retrying on every reconnect all session.
                    val nextCount = (sessionAttempts[row.id] ?: 0) + 1
                    sessionAttempts[row.id] = nextCount
                    logger.warning(
                        TAG,
                        "Retryable WS error code=${error.code} for id=${row.id} ($nextCount/$MAX_ATTEMPTS)",
                    )
                    recordAttempt(row, lastError = "ws_${error.code.lowercase()}_attempt_$nextCount")
                    // The socket is still up, so no connection edge is coming to re-drain this.
                    // Backed off so a server that keeps rejecting cannot spin the row through its
                    // whole attempt budget in one burst.
                    applicationScope.launch {
                        delay(RETRY_BACKOFF_MS)
                        drain()
                    }
                    return
                }

                // Parked before the lock is released: the rollback below runs outside the mutex,
                // because it can make a network call, and a drain must not re-send the row while
                // it is being undone.
                sessionAttempts[row.id] = MAX_ATTEMPTS
                row
            }

        logger.error(
            TAG,
            "Write id=${rejected.id} rejected as final: code=${error.code} ${error.message}",
        )
        // Rolled back *before* the row is dropped. If the app dies in between, the row survives and
        // is re-sent, re-rejected and rolled back again; dropping the row first would leave the
        // optimistic entry looking synced with nothing left to correct it.
        rollBack(rejected, error)
        mutex.withLock {
            database.pendingOutboxDao.deleteById(rejected.id)
            sessionAttempts.remove(rejected.id)
        }
        // As in onAck: a row leaving the queue can release one that was held behind it.
        applicationScope.launch { drain() }
    }

    /**
     * The row a verdict belongs to, releasing its in-flight claim. Callers hold [mutex].
     *
     * Resolved from the table rather than from [inFlight], which is only a same-connection cache:
     * an ack can arrive on a socket opened after the process that sent the write was killed. A
     * verdict for a row that is already gone is a no-op — that is the same write being answered
     * twice, not a second write.
     */
    private suspend fun resolveRow(requestId: String): PendingOutboxEntity? {
        inFlight.remove(requestId)
        return database.pendingOutboxDao.getByRequestId(requestId).also {
            if (it == null) logger.debug(TAG, "Verdict for unknown requestId=$requestId; ignoring")
        }
    }

    /**
     * Undoes the optimistic local write behind a rejected row, so the UI stops showing something
     * the server will never hold.
     *
     * A create never landed, so the local entry is simply removed. An update did land once and its
     * server-side version is the truth, so the group is re-read — `TabEntryService` exposes no
     * per-entry fetch, and the group history is the available source of truth. The exception is an
     * entry the server says is gone: a backfill cannot remove what it is no longer told about, so
     * that one is deleted locally like a create.
     */
    private suspend fun rollBack(
        row: PendingOutboxEntity,
        error: WsErrorPayload,
    ) {
        val entryId =
            when (row.type) {
                OUTBOX_TYPE_NEW_TAB_ENTRY -> row.id
                OUTBOX_TYPE_TAB_ENTRY_UPDATE -> row.id.removePrefix(UPDATE_ID_PREFIX)
                else -> return
            }

        if (row.type == OUTBOX_TYPE_NEW_TAB_ENTRY || error.code == ERROR_TAB_ENTRY_NOT_FOUND) {
            logger.warning(TAG, "Rolling back local tab entry $entryId")
            database.tabEntryDao.deleteTabEntryAndSplits(
                tabEntryId = entryId,
                splitDao = database.tabEntrySplitDao,
            )
            return
        }

        val groupId =
            try {
                val envelope = json.decodeFromString(WebSocketMessageDto.serializer(), row.payload)
                json.decodeFromString(NewTabEntryWsPayload.serializer(), envelope.payload).groupId
            } catch (e: SerializationException) {
                logger.error(TAG, "Unreadable payload on id=${row.id}", e)
                null
            }
        if (groupId == null) {
            logger.error(TAG, "Cannot roll back id=${row.id}: unreadable payload")
            return
        }

        logger.warning(TAG, "Rolling back edit of $entryId by refetching group $groupId")
        // Never throws and never fails its caller; on failure it marks the group for the next sync.
        backfiller.backfill(groupId)
    }

    private suspend fun recordAttempt(
        item: PendingOutboxEntity,
        lastError: String,
    ) {
        database.pendingOutboxDao.upsert(
            item.copy(
                attemptCount = item.attemptCount + 1,
                lastAttemptAt = Clock.System.now().toEpochMilliseconds(),
                lastError = lastError,
            ),
        )
    }

    private sealed class DispatchResult {
        /**
         * Handed to the socket and awaiting the server's verdict. The row stays — this is the whole
         * point of the acknowledgement protocol, and the difference between a write that reached
         * the server and one that only reached a send buffer.
         */
        data class Sent(val requestId: String) : DispatchResult()

        /** Terminal without an ack: the HTTP delete path, and unknown row types. */
        data object Success : DispatchResult()

        data class Transient(val reason: String) : DispatchResult()

        data class Permanent(val reason: String) : DispatchResult()
    }

    /**
     * The id scheme and type tags are read back by the activity feed's merge layer, which recovers a
     * write's `tabEntryId` by stripping these prefixes, so they are shared rather than private.
     */
    internal companion object {
        private const val TAG = "TabEntryOutbox"
        private const val MAX_ATTEMPTS = 10

        /**
         * How long a sent write waits for its verdict before it is re-sent on the same connection.
         * A dropped socket already releases every claim, so this only covers a live socket that
         * lost the ack itself. Well inside the server's replay window, which answers the repeat
         * instead of writing again.
         */
        private const val ACK_TIMEOUT_MS = 30_000L

        /** Breathing room before re-sending after a retryable rejection, so a row cannot spin. */
        private const val RETRY_BACKOFF_MS = 5_000L

        /** Server `ErrorDto.code` for an entry it no longer has; the one update that cannot heal. */
        private const val ERROR_TAB_ENTRY_NOT_FOUND = "TAB_ENTRY_NOT_FOUND"
        const val DELETE_ID_PREFIX = "delete:"
        const val UPDATE_ID_PREFIX = "update:"
        const val OUTBOX_TYPE_NEW_TAB_ENTRY = "new_tab_entry"
        const val OUTBOX_TYPE_TAB_ENTRY_UPDATE = "tab_entry.update"
        const val OUTBOX_TYPE_TAB_ENTRY_DELETE = "tab_entry.delete"
    }
}
